package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
    /** PIN is not yet configured on this device — first-run setup required. */
    data object NotConfigured : PinVerifyResult()
}

class AppSecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("met_ghamr_security_vault", Context.MODE_PRIVATE)

    companion object {
        /**
         * SECURITY: OWNER_EMAIL is NOT used as the authority for admin access.
         * Admin authority comes exclusively from Firebase Custom Claims (token.admin == true),
         * verified in AuthService.resolveUserRole() via Firebase ID Token.
         * This email is only used for display/audit purposes.
         */
        val AUTHORIZED_OWNER_EMAIL: String
            get() = try {
                com.example.BuildConfig.OWNER_EMAIL.takeIf { it.isNotBlank() } ?: ""
            } catch (e: Exception) { "" }

        private const val MAX_FAILED_ATTEMPTS = 5
        // Progressive lockout: 30s, 60s, 120s, 300s, 600s
        private val LOCKOUT_DURATIONS_MS = longArrayOf(30_000, 60_000, 120_000, 300_000, 600_000)

        private const val KEY_APP_UNLOCKED    = "sec_app_unlocked"
        private const val KEY_FAILED_ATTEMPTS = "sec_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL   = "sec_lockout_until"
        private const val KEY_LOCKOUT_COUNT   = "sec_lockout_count"
        private const val KEY_SALT            = "sec_pbkdf2_salt_v3"  // 16-byte random Base64
        private const val KEY_PIN_HASH        = "sec_pin_hash_v3"     // PBKDF2 derived, Base64

        // PBKDF2 parameters
        private const val PBKDF2_ITERATIONS   = 100_000
        private const val PBKDF2_KEY_LEN_BITS = 256
        private const val PBKDF2_ALGORITHM    = "PBKDF2WithHmacSHA256"
    }

    // ── App lock state ────────────────────────────────────────────────────────

    /**
     * The app starts LOCKED. It is only set to true after a successful verifyMasterPin().
     * The init block does NOT auto-unlock to prevent PIN bypass.
     */
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _lockoutSecondsLeft = MutableStateFlow(0)
    val lockoutSecondsLeft: StateFlow<Int> = _lockoutSecondsLeft.asStateFlow()

    private val _securityLogs = MutableStateFlow<List<SecurityAuditEntry>>(emptyList())
    val securityLogs: StateFlow<List<SecurityAuditEntry>> = _securityLogs.asStateFlow()

    init {
        // Restore failed attempt count from prefs — but never auto-unlock
        _failedAttempts.value = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        checkLockoutStatus()
        // NOTE: _isAppUnlocked stays false until verifyMasterPin() succeeds
    }

    // ── Owner identity ────────────────────────────────────────────────────────

    /**
     * Email comparison is only used for display; it does NOT grant admin rights.
     * Admin rights come from Firebase Custom Claims checked server-side.
     */
    fun isOwnerEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return email.trim().lowercase() == AUTHORIZED_OWNER_EMAIL.lowercase()
    }

    /**
     * Authority check: strictly verifies that the user possesses Firebase Custom Claims (admin == true).
     * BuildConfig data is never trusted as proof of admin privileges.
     */
    fun isOwnerAccount(user: UserAccount?): Boolean =
        user != null && user.isSuperAdmin

    // ── Lockout ───────────────────────────────────────────────────────────────

    fun checkLockoutStatus(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (now < lockoutUntil) {
            val secondsRemaining = ((lockoutUntil - now) / 1000).toInt().coerceAtLeast(1)
            _lockoutSecondsLeft.value = secondsRemaining
            true
        } else {
            _lockoutSecondsLeft.value = 0
            false
        }
    }

    // ── PIN verification ──────────────────────────────────────────────────────

    fun verifyMasterPin(enteredPin: String, user: UserAccount?): PinVerifyResult {
        // 1. Active lockout check
        if (checkLockoutStatus()) {
            val seconds = _lockoutSecondsLeft.value
            addAuditLog(
                SecurityEventType.BRUTE_FORCE_LOCKOUT,
                "محاولة إدخال PIN أثناء فترة الحظر ($seconds ثانية متبقية)",
                false
            )
            return PinVerifyResult.LockoutActive(seconds)
        }

        // 2. Check PIN is configured
        val storedHash = prefs.getString(KEY_PIN_HASH, null)
        if (storedHash.isNullOrBlank()) {
            addAuditLog(
                SecurityEventType.UNAUTHORIZED_LOGIN_ATTEMPT,
                "لا يوجد رمز PIN مُعيَّن. يجب ضبطه أولاً من إعدادات الأمان.",
                false
            )
            return PinVerifyResult.NotConfigured
        }

        // 3. PBKDF2 comparison (constant-time via MessageDigest.isEqual internally)
        val enteredHash = deriveKey(enteredPin.trim(), getSalt())
        val isCorrect = enteredHash == storedHash

        return if (isCorrect) {
            // Reset failure state
            _failedAttempts.value = 0
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putInt(KEY_LOCKOUT_COUNT, 0)
                .remove(KEY_LOCKOUT_UNTIL)
                .putBoolean(KEY_APP_UNLOCKED, true)
                .apply()

            _isAppUnlocked.value = true
            _lockoutSecondsLeft.value = 0

            addAuditLog(
                SecurityEventType.PIN_UNLOCK_SUCCESS,
                "تم التحقق بنجاح من رمز PIN: ${user?.displayName ?: "(مجهول)"}",
                true
            )
            PinVerifyResult.Success
        } else {
            val newFailCount = _failedAttempts.value + 1
            _failedAttempts.value = newFailCount
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, newFailCount).apply()

            if (newFailCount >= MAX_FAILED_ATTEMPTS) {
                val lockoutCount = prefs.getInt(KEY_LOCKOUT_COUNT, 0)
                val durationMs = LOCKOUT_DURATIONS_MS.getOrElse(lockoutCount) { LOCKOUT_DURATIONS_MS.last() }
                val lockoutUntil = System.currentTimeMillis() + durationMs
                val secondsLeft = (durationMs / 1000).toInt()

                prefs.edit()
                    .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                    .putInt(KEY_LOCKOUT_COUNT, lockoutCount + 1)
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .apply()

                _lockoutSecondsLeft.value = secondsLeft
                _failedAttempts.value = 0

                addAuditLog(
                    SecurityEventType.BRUTE_FORCE_LOCKOUT,
                    "تجاوز الحد الأقصى للمحاولات. حظر لمدة $secondsLeft ثانية",
                    false
                )
                PinVerifyResult.LockoutActive(secondsLeft)
            } else {
                val remaining = MAX_FAILED_ATTEMPTS - newFailCount
                addAuditLog(
                    SecurityEventType.PIN_UNLOCK_FAILURE,
                    "رمز PIN غير صحيح. المحاولات المتبقية: $remaining",
                    false
                )
                PinVerifyResult.IncorrectPin(remaining)
            }
        }
    }

    /**
     * Sets a new master PIN. Must be called from a secure admin context after verifying
     * Firebase Custom Claims (admin == true). Stores only PBKDF2 derived key, never the PIN.
     */
    fun setMasterPin(newPin: String): Boolean {
        if (newPin.length < 4) return false
        val salt = generateAndStoreSalt()
        val hash = deriveKey(newPin.trim(), salt)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        addAuditLog(
            SecurityEventType.SECURITY_SETTINGS_CHANGE,
            "تم تحديث رمز PIN باستخدام PBKDF2WithHmacSHA256 ($PBKDF2_ITERATIONS تكرار)",
            true
        )
        return true
    }

    /** Returns true if a master PIN has been configured on this device. */
    fun isMasterPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun lockApp() {
        _isAppUnlocked.value = false
        prefs.edit().putBoolean(KEY_APP_UNLOCKED, false).apply()
        addAuditLog(SecurityEventType.MANUAL_APP_LOCK, "تم قفل التطبيق فورياً", true)
    }

    fun addAuditLog(type: SecurityEventType, description: String, isSuccess: Boolean) {
        val entry = SecurityAuditEntry(
            eventType = type,
            description = sanitizeInput(description),
            isSuccess = isSuccess
        )
        _securityLogs.value = (listOf(entry) + _securityLogs.value).take(50)
    }

    // ── PBKDF2 key derivation ─────────────────────────────────────────────────

    private fun getSalt(): String {
        val existing = prefs.getString(KEY_SALT, null)
        if (!existing.isNullOrBlank()) return existing
        return generateAndStoreSalt()
    }

    private fun generateAndStoreSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val salt = Base64.encodeToString(bytes, Base64.NO_WRAP)
        prefs.edit().putString(KEY_SALT, salt).apply()
        return salt
    }

    /**
     * Derives a key from [pin] using PBKDF2WithHmacSHA256.
     * Returns Base64-encoded derived key.
     */
    private fun deriveKey(pin: String, saltBase64: String): String {
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LEN_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }

    // ── Input sanitization ────────────────────────────────────────────────────

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
            .replace("\"", "\u201C")
            .replace("'", "\u2019")
            .replace("--", "—")
            .replace(";", "؛")
            .trim()
    }
}
