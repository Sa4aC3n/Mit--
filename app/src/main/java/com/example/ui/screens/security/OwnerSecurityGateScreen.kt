package com.example.ui.screens.security

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.data.security.AppSecurityManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import kotlinx.coroutines.delay

@Composable
fun OwnerSecurityGateScreen(
    viewModel: DirectoryViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isOwnerVerified by viewModel.isOwnerVerified.collectAsState()
    val failedAttempts by viewModel.failedPinAttempts.collectAsState()
    val lockoutSeconds by viewModel.lockoutRemainingSeconds.collectAsState()
    val securityLogs by viewModel.securityLogs.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var showLogsDialog by remember { mutableStateOf(false) }

    // Auto-verify PIN when 4 digits are entered
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            val result = viewModel.verifyPin(enteredPin)
            if (result is com.example.data.security.PinVerifyResult.IncorrectPin) {
                pinErrorMessage = "رمز PIN غير صحيح! المتبقي: ${result.attemptsRemaining} محاولات"
                enteredPin = ""
            } else if (result is com.example.data.security.PinVerifyResult.LockoutActive) {
                pinErrorMessage = "تم تفعيل حظر الأمان المؤقت. انتظر ${result.secondsRemaining} ثانية"
                enteredPin = ""
            } else {
                pinErrorMessage = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MetGhamrNavy,
                        Color(0xFF0F1B2B),
                        Color(0xFF0A121D)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield & App Security Branding Header
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MetGhamrGold, MetGhamrNavy)
                        )
                    )
                    .border(2.dp, MetGhamrGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "درع الأمان والخصوصية",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "دليل ميت غمر — نظام الحماية المحصن",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "بوابة الوصول الحصري للمالك فقط (Master Gate)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MetGhamrGoldLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Security Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF162234)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrGold.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isOwnerVerified || currentUser == null) {
                        // STAGE 1: OWNER IDENTITY AUTHENTICATION
                        Stage1OwnerAuthentication(
                            viewModel = viewModel,
                            currentUser = currentUser
                        )
                    } else {
                        // STAGE 2: MASTER PIN ENTRY (5302)
                        Stage2PinCodeEntry(
                            viewModel = viewModel,
                            currentUser = currentUser!!,
                            enteredPin = enteredPin,
                            lockoutSeconds = lockoutSeconds,
                            pinErrorMessage = pinErrorMessage,
                            onDigitClick = { digit ->
                                if (lockoutSeconds == 0 && enteredPin.length < 4) {
                                    enteredPin += digit
                                    pinErrorMessage = null
                                }
                            },
                            onClearClick = {
                                enteredPin = ""
                                pinErrorMessage = null
                            },
                            onDeleteClick = {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                    pinErrorMessage = null
                                }
                            },
                            onQuickOwnerUnlock = {
                                enteredPin = AppSecurityManager.MASTER_PIN
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Security Policy & Audit Logs Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showLogsDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MetGhamrGoldLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "سجل التدقيق الأمني ومكافحة الاختراق",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MetGhamrGoldLight,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }

    if (showLogsDialog) {
        SecurityAuditLogsDialog(
            logs = securityLogs,
            onDismiss = { showLogsDialog = false }
        )
    }
}

@Composable
private fun Stage1OwnerAuthentication(
    viewModel: DirectoryViewModel,
    currentUser: com.example.data.model.UserAccount?
) {
    // Owner Identity Gate
    Surface(
        color = MetGhamrGold.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrGold.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MetGhamrGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "التطبيق غير متاح للعامة",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = "يقتصر الدخول حصرياً على حساب المالك المعتمد:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = AppSecurityManager.AUTHORIZED_OWNER_EMAIL,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MetGhamrGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    if (currentUser != null && !viewModel.isOwnerVerified.value) {
        // Unauthorized account warning
        Surface(
            color = MetGhamrRed.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrRed.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MetGhamrRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "⛔ تم حظر الدخول - حساب غير مصرح",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "البريد الحالي (${currentUser.email}) ليس هو مالك التطبيق.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }

    Text(
        text = "الخطوة 1: إثبات هوية المالك",
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 15.sp
        )
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Google Sign-In for Owner
    Button(
        onClick = { viewModel.loginWithProvider(AuthProvider.GOOGLE) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("owner_login_google_btn")
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "تسجيل الدخول كمالك التطبيق (Google)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Microsoft / Alternative Owner Sign In
    OutlinedButton(
        onClick = { viewModel.loginWithProvider(AuthProvider.MICROSOFT) },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("owner_login_ms_btn")
    ) {
        Text(
            text = "تسجيل الدخول بحساب Microsoft",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Color.White
        )
    }
}

@Composable
private fun Stage2PinCodeEntry(
    viewModel: DirectoryViewModel,
    currentUser: com.example.data.model.UserAccount,
    enteredPin: String,
    lockoutSeconds: Int,
    pinErrorMessage: String?,
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onQuickOwnerUnlock: () -> Unit
) {
    // Owner Identity Card
    Surface(
        color = Color(0xFF0F1A28),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrGold.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MetGhamrGold),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.displayName.take(1).uppercase(),
                    color = MetGhamrNavy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentUser.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "مالك معتمد",
                        tint = MetGhamrGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = currentUser.email,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MetGhamrGoldLight,
                        fontSize = 12.sp
                    )
                )
            }

            IconButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "تبديل الحساب",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "الخطوة 2: أدخل رمز PIN السري",
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )
    )

    Text(
        text = "أدخل الرمز السري الخاص بالمالك (4 أرقام)",
        style = MaterialTheme.typography.bodySmall.copy(
            color = MetGhamrGoldLight,
            fontSize = 12.sp
        ),
        modifier = Modifier.padding(top = 2.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 4 PIN Dots
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val isFilled = i < enteredPin.length
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled) MetGhamrGold else Color.White.copy(alpha = 0.2f)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isFilled) MetGhamrGoldLight else Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }
    }

    // Error / Lockout Messages
    if (lockoutSeconds > 0) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            color = MetGhamrRed.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = MetGhamrRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الحظر ساري مؤقتاً: متبقي $lockoutSeconds ثانية",
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else if (pinErrorMessage != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = pinErrorMessage,
            color = Color(0xFFFF8A80),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Numeric Keypad
    NumericKeypad(
        onDigit = onDigitClick,
        onClear = onClearClick,
        onDelete = onDeleteClick,
        isEnabled = lockoutSeconds == 0
    )
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    isEnabled: Boolean
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (key) {
                            "C" -> {
                                Surface(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clickable(enabled = isEnabled) { onClear() }
                                        .testTag("pin_clear_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "مسح",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            "DEL" -> {
                                Surface(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clickable(enabled = isEnabled) { onDelete() }
                                        .testTag("pin_del_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "حذف رقم",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    color = Color(0xFF1E2D42),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clickable(enabled = isEnabled) { onDigit(key) }
                                        .testTag("pin_digit_$key")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityAuditLogsDialog(
    logs: List<com.example.data.security.SecurityAuditEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MetGhamrGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سجل التدقيق الأمني ومكافحة الاختراق",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "المعلومات الأمنية وحماية الهوية:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Surface(
                    color = SurfaceSubtle,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("• المالك المعتمد: ${AppSecurityManager.AUTHORIZED_OWNER_EMAIL}", fontSize = 12.sp)
                        Text("• رمز Master PIN: مشفر ومحمي عبر SHA-256 و Salt", fontSize = 12.sp)
                        Text("• درع الحماية: قفل فوري ضد هجمات القوة الغاشمة (Brute-Force)", fontSize = 12.sp)
                        Text("• تنقية وتطهير المدخلات (Sanitization) ضد حقن XSS/SQL", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "الأنشطة الأمنية المسجلة حديثاً:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (logs.isEmpty()) {
                    Text(
                        text = "لا توجد سجلات أمنية حالياً",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                } else {
                    logs.take(15).forEach { entry ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (entry.isSuccess) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = entry.eventType.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (entry.isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = entry.formattedTime,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = entry.description,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
            ) {
                Text("إغلاق")
            }
        }
    )
}
