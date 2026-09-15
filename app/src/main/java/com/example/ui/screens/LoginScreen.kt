package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onContinueAsGuest: () -> Unit,
    showTopBar: Boolean = true,
    showSocialAndGuest: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val isLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }

    val mainCardContent = @Composable { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceLight)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo Badge
                    Surface(
                        color = MetGhamrNavy,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = "شعار دليل ميت غمر",
                                tint = MetGhamrGold,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "دليل ميت غمر الشامل",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy,
                            fontSize = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isRegisterMode)
                            "أنشئ حسابك الجديد بالبريد الإلكتروني للوصول إلى الخدمات"
                        else
                            "سجل دخولك إلى حسابك باستخدام البريد الإلكتروني أو الحسابات المرتبطة",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Switcher (Sign In vs Register Tabs)
                    TabRow(
                        selectedTabIndex = if (isRegisterMode) 1 else 0,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = MetGhamrNavy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = !isRegisterMode,
                            onClick = {
                                isRegisterMode = false
                                viewModel.clearAuthError()
                            },
                            text = {
                                Text(
                                    "تسجيل الدخول",
                                    fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_login")
                        )
                        Tab(
                            selected = isRegisterMode,
                            onClick = {
                                isRegisterMode = true
                                viewModel.clearAuthError()
                            },
                            text = {
                                Text(
                                    "إنشاء حساب",
                                    fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_register")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Error Message Banner
                    AnimatedVisibility(visible = authError != null) {
                        authError?.let { msg ->
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        color = Color(0xFF991B1B),
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Registration Name Field
                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("الاسم الكامل") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MetGhamrNavy)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = MetGhamrNavy,
                                unfocusedBorderColor = BorderLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Email Field
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("البريد الإلكتروني") },
                        placeholder = { Text("example@domain.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MetGhamrNavy)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = MetGhamrNavy,
                            unfocusedBorderColor = BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("كلمة المرور") },
                        placeholder = { Text("6 أحرف أو أكثر") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MetGhamrNavy)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = {
                                focusManager.clearFocus()
                                if (!isRegisterMode && !isLoading) {
                                    viewModel.loginWithEmail(emailInput, passwordInput)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = MetGhamrNavy,
                            unfocusedBorderColor = BorderLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    // Confirm Password Field in Register Mode
                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirmPasswordInput,
                            onValueChange = { confirmPasswordInput = it },
                            label = { Text("تأكيد كلمة المرور") },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = MetGhamrNavy)
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (passwordInput != confirmPasswordInput) {
                                        viewModel.showToast("كلمتا المرور غير متطابقتين")
                                    } else if (!isLoading) {
                                        viewModel.registerWithEmail(emailInput, passwordInput, nameInput)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = MetGhamrNavy,
                                unfocusedBorderColor = BorderLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_confirm_password_input")
                        )
                    }

                    // Forgot Password Link in Login Mode
                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "نسيت كلمة المرور؟",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MetGhamrNavy,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier
                                    .clickable {
                                        forgotPasswordEmail = emailInput
                                        showForgotPasswordDialog = true
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Submit Button (Login or Register)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isRegisterMode) {
                                if (passwordInput != confirmPasswordInput) {
                                    viewModel.showToast("كلمتا المرور غير متطابقتين")
                                    return@Button
                                }
                                viewModel.registerWithEmail(emailInput, passwordInput, nameInput)
                            } else {
                                viewModel.loginWithEmail(emailInput, passwordInput)
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag(if (isRegisterMode) "register_submit_btn" else "login_submit_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    if (isRegisterMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    tint = MetGhamrGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRegisterMode) "إنشاء حساب وإرسال رابط التحقق" else "تسجيل الدخول",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (showSocialAndGuest) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Or Separator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                            Text(
                                text = "أو عبر الحسابات السريعة",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Social Login Buttons (معطلة مؤقتاً لحين تشغيلها في الوقت المناسب)
                        // Google
                        OutlinedButton(
                            onClick = { /* معطل مؤقتاً - لا يحدث أي شيء عند الضغط */ },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("login_google_btn")
                        ) {
                            Text(
                                text = "Google",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF4285F4)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Facebook & Microsoft Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* معطل مؤقتاً - لا يحدث أي شيء عند الضغط */ },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("login_facebook_btn")
                            ) {
                                Text(
                                    text = "Facebook",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1877F2)
                                )
                            }

                            OutlinedButton(
                                onClick = { /* معطل مؤقتاً - لا يحدث أي شيء عند الضغط */ },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("login_microsoft_btn")
                            ) {
                                Text(
                                    text = "Microsoft",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF00A4EF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider(color = BorderLight, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Guest Mode Action
                        Text(
                            text = "المتابعة كزائر وتصفح الدليل",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MetGhamrNavy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onContinueAsGuest() }
                                .padding(8.dp)
                                .testTag("login_guest_mode_link")
                        )
                    }
                }
            }
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isRegisterMode) "إنشاء حساب جديد" else "تسجيل الدخول",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("login_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MetGhamrNavy,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            mainCardContent(innerPadding)
        }
    } else {
        mainCardContent(PaddingValues(0.dp))
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "إعادة تعيين كلمة المرور",
                    fontWeight = FontWeight.Bold,
                    color = MetGhamrNavy
                )
            },
            text = {
                Column {
                    Text(
                        text = "أدخل بريدك الإلكتروني وسنرسل لك رابطاً لإعادة تعيين كلمة المرور عبر Firebase Authentication:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("البريد الإلكتروني") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordEmail.isNotBlank()) {
                            viewModel.sendPasswordReset(forgotPasswordEmail)
                            showForgotPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                ) {
                    Text("إرسال الرابط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}
