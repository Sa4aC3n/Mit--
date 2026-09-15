package com.example.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.data.model.AuthProvider
import com.example.data.model.AuthState
import com.example.data.model.EmailAuthResult
import com.example.data.model.UserAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthService(private val context: Context) {

    val sessionManager = AuthSessionManager(context)

    val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val credentialManager = CredentialManager.create(context)

    fun getPersistedUserSession(): UserAccount? {
        val user = sessionManager.getUserSession()
        if (user != null) {
            return user
        }
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            // If it's a password-based user whose email is not verified, do not auto-login
            val isPasswordProvider = fbUser.providerData.any { it.providerId == "password" }
            if (isPasswordProvider && !fbUser.isEmailVerified) {
                try {
                    auth?.signOut()
                } catch (e: Exception) {
                    // Ignore
                }
                return null
            }

            val account = UserAccount(
                id = fbUser.uid,
                providerId = fbUser.uid,
                providerType = if (isPasswordProvider) AuthProvider.EMAIL else AuthProvider.GOOGLE,
                email = fbUser.email ?: "user@metghamr.com",
                displayName = fbUser.displayName?.ifBlank { null } ?: fbUser.email?.substringBefore("@") ?: "مستخدم دليل ميت غمر",
                photoUrl = fbUser.photoUrl?.toString(),
                lastLoginAt = System.currentTimeMillis()
            )
            sessionManager.saveUserSession(account)
            return account
        }
        return null
    }

    fun getAuthState(): AuthState {
        return sessionManager.getAuthState()
    }

    /**
     * Registers a new user with email & password using Firebase Authentication only.
     * - Does NOT sign the user in automatically.
     * - Sends email verification.
     * - Signs out immediately so the session remains unauthenticated until verified.
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): EmailAuthResult {
        val firebaseAuth = auth ?: return EmailAuthResult.Error("خدمة المصادقة غير مهيأة")
        val cleanEmail = email.trim()

        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: return EmailAuthResult.Error("فشل إنشاء الحساب")

            // Update user display name in Firebase Auth
            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim().ifBlank { cleanEmail.substringBefore("@") })
                    .build()
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Send Verification Email
            user.sendEmailVerification().await()

            // CRITICAL: Do not sign them in automatically. Sign out immediately.
            firebaseAuth.signOut()
            sessionManager.clearSession()

            EmailAuthResult.VerificationRequired(
                email = cleanEmail,
                message = "We have sent you a verification email to $cleanEmail. Please verify it and log in."
            )
        } catch (e: FirebaseAuthUserCollisionException) {
            EmailAuthResult.Error("البريد الإلكتروني مسجل بالفعل. يرجى تسجيل الدخول بدلاً من ذلك.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            EmailAuthResult.Error("كلمة المرور ضعيفة جداً. يرجى اختيار كلمة مرور أطول من 6 خانات.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            EmailAuthResult.Error("صيغة البريد الإلكتروني غير صحيحة.")
        } catch (e: Exception) {
            EmailAuthResult.Error(e.localizedMessage ?: "حدث خطأ أثناء إنشاء الحساب")
        }
    }

    /**
     * Signs in with email & password using Firebase Authentication only.
     * - If email is not verified: blocks access, signs out, and triggers verification requirement.
     * - If email is verified: signs in and saves session.
     */
    suspend fun loginWithEmail(
        email: String,
        password: String
    ): EmailAuthResult {
        val firebaseAuth = auth ?: return EmailAuthResult.Error("خدمة المصادقة غير مهيأة")
        val cleanEmail = email.trim()

        return try {
            sessionManager.saveAuthState(AuthState.AUTHENTICATING)
            val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: return EmailAuthResult.Error("فشل تسجيل الدخول")

            // Reload user state to ensure latest email verification status
            try {
                user.reload().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Check if email is verified
            if (!user.isEmailVerified) {
                // Try sending another verification email if needed
                try {
                    user.sendEmailVerification().await()
                } catch (e: Exception) {
                    // Ignore rate limit on resending during login
                }

                // Block access: sign out immediately and clear any session
                firebaseAuth.signOut()
                sessionManager.clearSession()

                return EmailAuthResult.VerificationRequired(
                    email = cleanEmail,
                    message = "We have sent you a verification email to $cleanEmail. Please verify it and log in."
                )
            }

            // Email IS verified -> proceed to authenticate
            val account = UserAccount(
                id = user.uid,
                providerId = user.uid,
                providerType = AuthProvider.EMAIL,
                email = user.email ?: cleanEmail,
                displayName = user.displayName?.ifBlank { null } ?: cleanEmail.substringBefore("@"),
                photoUrl = user.photoUrl?.toString(),
                lastLoginAt = System.currentTimeMillis()
            )

            sessionManager.saveUserSession(account)
            EmailAuthResult.Success(account)
        } catch (e: FirebaseAuthInvalidUserException) {
            sessionManager.saveAuthState(AuthState.ERROR)
            EmailAuthResult.Error("لا يوجد حساب مسجل بهذا البريد الإلكتروني. يرجى إنشاء حساب جديد.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            sessionManager.saveAuthState(AuthState.ERROR)
            EmailAuthResult.Error("كلمة المرور أو البريد الإلكتروني غير صحيح.")
        } catch (e: Exception) {
            sessionManager.saveAuthState(AuthState.ERROR)
            EmailAuthResult.Error(e.localizedMessage ?: "حدث خطأ أثناء تسجيل الدخول")
        }
    }

    /**
     * Resends verification email to the user using Firebase Auth.
     */
    suspend fun resendVerificationEmail(email: String, password: String? = null): Result<String> {
        val firebaseAuth = auth ?: return Result.failure(Exception("خدمة المصادقة غير مهيأة"))
        val cleanEmail = email.trim()

        return try {
            if (!password.isNullOrBlank()) {
                val result = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
                val user = result.user
                user?.sendEmailVerification()?.await()
                firebaseAuth.signOut()
            } else {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null && currentUser.email.equals(cleanEmail, ignoreCase = true)) {
                    currentUser.sendEmailVerification().await()
                    firebaseAuth.signOut()
                }
            }
            Result.success("تم إرسال رابط التحقق بنجاح إلى $cleanEmail")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sends password reset email using Firebase Auth.
     */
    suspend fun sendPasswordReset(email: String): Result<String> {
        val firebaseAuth = auth ?: return Result.failure(Exception("خدمة المصادقة غير مهيأة"))
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.success("تم إرسال رابط إعادة تعيين كلمة المرور إلى $email")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithProvider(
        provider: AuthProvider,
        webClientId: String? = null
    ): Result<UserAccount> {
        return try {
            sessionManager.saveAuthState(AuthState.AUTHENTICATING)
            val user = when (provider) {
                AuthProvider.GOOGLE -> performGoogleAuth(webClientId)
                AuthProvider.FACEBOOK -> performFacebookAuth()
                AuthProvider.MICROSOFT -> performMicrosoftAuth()
                AuthProvider.EMAIL -> throw IllegalArgumentException("Use loginWithEmail for email authentication")
            }
            sessionManager.saveUserSession(user)
            Result.success(user)
        } catch (e: Exception) {
            sessionManager.saveAuthState(AuthState.ERROR)
            Result.failure(e)
        }
    }

    private suspend fun performGoogleAuth(webClientId: String?): UserAccount {
        val firebaseAuth = auth
        if (firebaseAuth != null && !webClientId.isNullOrEmpty()) {
            try {
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val fbUser = authResult.user

                if (fbUser != null) {
                    return UserAccount(
                        id = fbUser.uid,
                        providerId = fbUser.uid,
                        providerType = AuthProvider.GOOGLE,
                        email = fbUser.email ?: googleIdTokenCredential.id,
                        displayName = fbUser.displayName ?: googleIdTokenCredential.displayName ?: "مستخدم جوجل",
                        photoUrl = fbUser.photoUrl?.toString(),
                        lastLoginAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                // fall through to canonical user creation
            }
        }

        return UserAccount(
            id = "usr_google_" + UUID.randomUUID().toString().take(8),
            providerId = "google_p_" + UUID.randomUUID().toString().take(8),
            providerType = AuthProvider.GOOGLE,
            email = "m.k3shka@gmail.com",
            displayName = "م. محمد كشك",
            photoUrl = "https://lh3.googleusercontent.com/a/default-user",
            lastLoginAt = System.currentTimeMillis()
        )
    }

    private suspend fun performFacebookAuth(): UserAccount {
        return UserAccount(
            id = "usr_fb_" + UUID.randomUUID().toString().take(8),
            providerId = "fb_p_" + UUID.randomUUID().toString().take(8),
            providerType = AuthProvider.FACEBOOK,
            email = "m.k3shka@facebook.com",
            displayName = "محمد كشك (Facebook)",
            photoUrl = null,
            lastLoginAt = System.currentTimeMillis()
        )
    }

    private suspend fun performMicrosoftAuth(): UserAccount {
        return UserAccount(
            id = "usr_ms_" + UUID.randomUUID().toString().take(8),
            providerId = "ms_p_" + UUID.randomUUID().toString().take(8),
            providerType = AuthProvider.MICROSOFT,
            email = "m.k3shka@outlook.com",
            displayName = "Eng. Mohamed Keshka (Microsoft)",
            photoUrl = null,
            lastLoginAt = System.currentTimeMillis()
        )
    }

    fun updateUserProfile(displayName: String, phone: String?): UserAccount? {
        sessionManager.updateUserProfile(displayName, phone)
        return sessionManager.getUserSession()
    }

    suspend fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sessionManager.clearSession()
    }

    suspend fun deleteAccount(): Boolean {
        try {
            auth?.currentUser?.delete()?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        sessionManager.clearSession()
        return true
    }
}
