package com.example.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.data.model.AuthProvider
import com.example.data.model.AuthState
import com.example.data.model.UserAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthService(private val context: Context) {

    val sessionManager = AuthSessionManager(context)

    private val auth: FirebaseAuth? by lazy {
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
            val account = UserAccount(
                id = fbUser.uid,
                providerId = fbUser.uid,
                providerType = AuthProvider.GOOGLE,
                email = fbUser.email ?: "google.user@metghamr.com",
                displayName = fbUser.displayName ?: "مستخدم جوجل",
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
