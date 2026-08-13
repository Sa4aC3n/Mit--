package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AuthProvider
import com.example.data.model.AuthState
import com.example.data.model.UserAccount
import org.json.JSONObject

class AuthSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("met_ghamr_auth_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_STATE = "auth_state"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_USER_PROVIDER = "user_provider"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_LAST_LOGIN = "last_login"
    }

    fun saveUserSession(
        user: UserAccount,
        accessToken: String? = null,
        refreshToken: String? = null
    ) {
        prefs.edit()
            .putString(KEY_AUTH_STATE, AuthState.AUTHENTICATED.name)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_AVATAR, user.photoUrl ?: "")
            .putString(KEY_USER_PROVIDER, user.providerType.name)
            .putString(KEY_USER_PHONE, user.phone ?: "")
            .putString(KEY_ACCESS_TOKEN, accessToken ?: "mock_acc_tok_${user.id}_${System.currentTimeMillis()}")
            .putString(KEY_REFRESH_TOKEN, refreshToken ?: "mock_ref_tok_${user.id}_${System.currentTimeMillis()}")
            .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            .apply()
    }

    fun getUserSession(): UserAccount? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val avatar = prefs.getString(KEY_USER_AVATAR, "")
        val providerStr = prefs.getString(KEY_USER_PROVIDER, AuthProvider.GOOGLE.name) ?: AuthProvider.GOOGLE.name
        val phone = prefs.getString(KEY_USER_PHONE, null)
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, System.currentTimeMillis())

        val provider = try {
            AuthProvider.valueOf(providerStr)
        } catch (e: Exception) {
            AuthProvider.GOOGLE
        }

        return UserAccount(
            id = userId,
            providerId = userId,
            providerType = provider,
            email = email,
            displayName = name,
            photoUrl = if (avatar.isNullOrEmpty()) null else avatar,
            phone = if (phone.isNullOrEmpty()) null else phone,
            lastLoginAt = lastLogin
        )
    }

    fun getAuthState(): AuthState {
        val stateStr = prefs.getString(KEY_AUTH_STATE, AuthState.GUEST.name) ?: AuthState.GUEST.name
        return try {
            AuthState.valueOf(stateStr)
        } catch (e: Exception) {
            AuthState.GUEST
        }
    }

    fun saveAuthState(state: AuthState) {
        prefs.edit().putString(KEY_AUTH_STATE, state.name).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun updateUserProfile(displayName: String, phone: String?) {
        val editor = prefs.edit()
        editor.putString(KEY_USER_NAME, displayName)
        if (phone != null) {
            editor.putString(KEY_USER_PHONE, phone)
        } else {
            editor.remove(KEY_USER_PHONE)
        }
        editor.apply()
    }

    fun clearSession() {
        prefs.edit().clear().putString(KEY_AUTH_STATE, AuthState.GUEST.name).apply()
    }
}
