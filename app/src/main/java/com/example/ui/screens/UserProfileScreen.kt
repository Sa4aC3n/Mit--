package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.ui.components.ProviderBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    var editName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editPhone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل الدخول والملف الشخصي", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentUser != null) {
                val user = currentUser!!

                // --- 1. PROFILE HEADER ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(MetGhamrNavy),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                color = MetGhamrGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        )

                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (!user.phone.isNullOrBlank()) {
                            Text(
                                text = "رقم الهاتف: ${user.phone}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        ProviderBadge(provider = user.providerType)

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل البيانات الشخصية", fontSize = 13.sp)
                        }
                    }
                }

                // --- 2. ACCOUNT SECTION ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "الحساب والأنشطة",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = "لوحة التحكم الإدارية",
                            subtitle = "إدارة الأنشطة، المساهمات، البلاغات والمستخدمين",
                            onClick = { viewModel.navigateTo(ScreenRoute.AdminDashboard.route) },
                            tag = "profile_menu_admin"
                        )

                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "المفضلة",
                            subtitle = "الأنشطة والخدمات المحفوظة",
                            onClick = onNavigateToFavorites,
                            tag = "profile_menu_favorites"
                        )

                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.Star,
                            title = "تقييماتي ومراجعاتي",
                            subtitle = "المراجعات والتقييمات المنشورة باسمك",
                            onClick = { viewModel.navigateTo(ScreenRoute.MyReviews.route) },
                            tag = "profile_menu_reviews"
                        )

                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.Assignment,
                            title = "مساهماتي واقتراحاتي",
                            subtitle = "متابعة طلبات إضافة الأنشطة وتعديل البيانات",
                            onClick = { viewModel.navigateTo(ScreenRoute.MyContributions.route) },
                            tag = "profile_menu_contributions"
                        )

                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.AddBusiness,
                            title = "أضف نشاطاً تجارياً جديداً",
                            subtitle = "ساهم في نمو دليل ميت غمر مجاناً",
                            onClick = { viewModel.navigateTo(ScreenRoute.AddBusiness.route) },
                            tag = "profile_menu_add_business"
                        )
                    }
                }

                // --- 3. SETTINGS SECTION ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "الإعدادات والخصوصية",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.Notifications,
                            title = "الإشعارات التنبيهية",
                            subtitle = "مفعلة لإشعارات الخدمات الجديدة",
                            onClick = { viewModel.showToast("الإشعارات التنبيهية مفعلة") },
                            tag = "profile_menu_notifs"
                        )

                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.Security,
                            title = "حماية الخصوصية",
                            subtitle = "بياناتك الشخصية مشفرة وآمنة",
                            onClick = { viewModel.showToast("تطبيق دليل ميت غمر يحترم خصوصية بياناتك بالكامل") },
                            tag = "profile_menu_privacy"
                        )
                    }
                }

                // --- 4. ABOUT SECTION ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "عن التطبيق",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        ProfileMenuItem(
                            icon = Icons.Default.Info,
                            title = "عن دليل ميت غمر",
                            subtitle = "معلومات الإصدار وتطوير المنصة",
                            onClick = onNavigateToAbout,
                            tag = "profile_menu_about"
                        )
                    }
                }

                // --- 5. DANGER ZONE ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "إجراءات الحساب",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrRed
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedButton(
                            onClick = { showLogoutConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MetGhamrRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showDeleteAccountDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MetGhamrRed.copy(alpha = 0.8f)),
                            modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف الحساب والبيانات الشخصية", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // GUEST PROFILE VIEW
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = MetGhamrNavy.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MetGhamrNavy,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "مرحباً بك في دليل ميت غمر",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                        )

                        Text(
                            text = "سجل دخولك الآن للوصول إلى جميع المميزات بما في ذلك إضافة التقييمات، حفظ المفضلة، والمزامنة عبر الأجهزة.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.loginWithProvider(AuthProvider.GOOGLE) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("guest_profile_google")
                        ) {
                            Text("التسجيل بواسطة Google", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.loginWithProvider(AuthProvider.FACEBOOK) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("guest_profile_facebook")
                        ) {
                            Text("التسجيل بواسطة Facebook", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.loginWithProvider(AuthProvider.MICROSOFT) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A4EF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("guest_profile_microsoft")
                        ) {
                            Text("التسجيل بواسطة Microsoft", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("تعديل البيانات الشخصية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم الظاهر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الهاتف (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateUserProfile(editName, editPhone.ifBlank { null })
                            showEditProfileDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Logout Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في تسجيل الخروج من حسابك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed)
                ) {
                    Text("تسجيل الخروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("تأكيد حذف الحساب", fontWeight = FontWeight.Bold, color = MetGhamrRed) },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف الحساب نهائياً؟ سيتم حذف جميع بياناتك الشخصية والمفضلة وفقاً لسياسة الخصوصية.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed)
                ) {
                    Text("حذف الحساب نهائياً")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MetGhamrNavy.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
