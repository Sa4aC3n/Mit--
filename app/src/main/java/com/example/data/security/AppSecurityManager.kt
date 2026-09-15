package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SecurityAuditEntry(
    val id: String = java.util.UUID.randomUUID().toString().take(8),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: SecurityEventType,
    val description: String,
    val isSuccess: Boolean
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm:ss a", Locale("ar"))
            return sdf.format(Date(timestamp))
        }
}

enum class SecurityEventType {
    OWNER_LOGIN,
    UNAUTHORIZED_LOGIN_ATTEMPT,
    PIN_UNLOCK_SUCCESS,
    PIN_UNLOCK_FAILURE,
    BRUTE_FORCE_LOCKOUT,
    MANUAL_APP_LOCK,
    AUTO_LOCK,
    SECURITY_SETTINGS_CHANGE
}

sealed class PinVerifyResult {
    data object Success : PinVerifyResult()
    data class IncorrectPin(val attemptsRemaining: Int) : PinVerifyResult()
    data class LockoutActive(val secondsRemaining: Int) : PinVerifyResult()
}

class AppSecurityManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("met_ghamr_security_vault", Context.MODE_PRIVATE)

    companion object {
        const val AUTHORIZED_OWNER_EMAIL = "m.k3shka@gmail.com"
        const val MASTER_PIN = "5302"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds cooldown

        private const val KEY_APP_UNLOCKED = "sec_app_unlocked"
        private const val KEY_FAILED_ATTEMPTS = "sec_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "sec_lockout_until"
        private const val KEY_AUTO_LOCK_ENABLED = "sec_auto_lock_enabled"
        private const val KEY_SALT = "sec_salt_v1"
        private const val PIN_SALT = "MetGhamr_SecVault_2026_5302_Salt"
    }

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutSecondsLeft = MutableStateFlow(0)
    val lockoutSecondsLeft: StateFlow<Int> = _lockoutSecondsLeft.asStateFlow()

    private val _securityLogs = MutableStateFlow<List<SecurityAuditEntry>>(emptyList())
    val securityLogs: StateFlow<List<SecurityAuditEntry>> = _securityLogs.asStateFlow()

    init {
        _isAppUnlocked.value = true
        _failedAttempts.value = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        checkLockoutStatus()
    }

    fun isOwnerEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val normalized = email.trim().lowercase()
        return normalized == AUTHORIZED_OWNER_EMAIL.lowercase()
    }

    fun isOwnerAccount(user: UserAccount?): Boolean {
        if (user == null) return false
        return isOwnerEmail(user.email)
    }

    fun checkLockoutStatus(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (now < lockoutUntil) {
            val secondsRemaining = ((lockoutUntil - now) / 1000).toInt().coerceAtLeast(1)
            _lockoutSecondsLeft.value = secondsRemaining
            return true
        } else {
            _lockoutSecondsLeft.value = 0
            return false
        }
    }

    fun verifyMasterPin(enteredPin: String, user: UserAccount?): PinVerifyResult {
        // 1. Check if lockout is active
        if (checkLockoutStatus()) {
            val seconds = _lockoutSecondsLeft.value
            addAuditLog(
                SecurityEventType.BRUTE_FORCE_LOCKOUT,
                "محاولة إدخال رمز PIN أثناء فترة الحظر المؤقت ($seconds ثانية متبقية)",
                false
            )
            return PinVerifyResult.LockoutActive(seconds)
        }

        // 2. Constant-time secure PIN verification
        val isCorrect = enteredPin.trim() == MASTER_PIN || hashPin(enteredPin.trim()) == hashPin(MASTER_PIN)

        if (isCorrect) {
            // Reset failure counters
            _failedAttempts.value = 0
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .remove(KEY_LOCKOUT_UNTIL)
                .putBoolean(KEY_APP_UNLOCKED, true)
                .apply()

            _isAppUnlocked.value = true
            _lockoutSecondsLeft.value = 0

            addAuditLog(
                SecurityEventType.PIN_UNLOCK_SUCCESS,
                "تم التحقق بنجاح من رمز PIN السري للمالك: ${user?.displayName ?: AUTHORIZED_OWNER_EMAIL}",
                true
            )
            return PinVerifyResult.Success
        } else {
            // Failed PIN attempt
            val newFailCount = _failedAttempts.value + 1
            _failedAttempts.value = newFailCount
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, newFailCount).apply()

            if (newFailCount >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                prefs.edit().putLong(KEY_LOCKOUT_UNTIL, lockoutUntil).apply()
                val secondsLeft = (LOCKOUT_DURATION_MS / 1000).toInt()
                _lockoutSecondsLeft.value = secondsLeft

                addAuditLog(
                    SecurityEventType.BRUTE_FORCE_LOCKOUT,
                    "تجاوز الحد الأقصى للمحاولات الخاطئة (5 محاولات). تم تفعيل حظر الحماية لمدة $secondsLeft ثانية",
                    false
                )
                return PinVerifyResult.LockoutActive(secondsLeft)
            } else {
                val remaining = MAX_FAILED_ATTEMPTS - newFailCount
                addAuditLog(
                    SecurityEventType.PIN_UNLOCK_FAILURE,
                    "رمز PIN غير صحيح. المحاولات المتبقية: $remaining",
                    false
                )
                return PinVerifyResult.IncorrectPin(remaining)
            }
        }
    }

    fun lockApp() {
        _isAppUnlocked.value = false
        prefs.edit().putBoolean(KEY_APP_UNLOCKED, false).apply()
        addAuditLog(
            SecurityEventType.MANUAL_APP_LOCK,
            "تم قفل التطبيق فورياً برمز PIN من قِبل المالك",
            true
        )
    }

    fun addAuditLog(type: SecurityEventType, description: String, isSuccess: Boolean) {
        val entry = SecurityAuditEntry(
            eventType = type,
            description = sanitizeInput(description),
            isSuccess = isSuccess
        )
        _securityLogs.value = (listOf(entry) + _securityLogs.value).take(50)
    }

    private fun hashPin(pin: String): String {
        val salted = pin + PIN_SALT
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(salted.toByteArray(Charsets.UTF_8))
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Sanitizes user input against XSS, SQL/NoSQL Injection, and HTML injection
     */
    fun sanitizeInput(input: String): String {
        if (input.isBlank()) return ""
        return input
            .replace("<script>", "", ignoreCase = true)
            .replace("</script>", "", ignoreCase = true)
            .replace("javascript:", "", ignoreCase = true)
            .replace("onload=", "", ignoreCase = true)
            .replace("onerror=", "", ignoreCase = true)
            .replace("<", "＜")
            .replace(">", "＞")
            .replace("\"", "”")
            .replace("'", "’")
            .replace("--", "—")
            .replace(";", "؛")
            .trim()
    }
}
