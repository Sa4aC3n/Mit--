package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider
import com.example.data.security.AppSecurityManager
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.ProviderBadge
import com.example.ui.components.SuperAdminBadge
import com.example.ui.screens.security.SecurityAuditLogsDialog
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

    if (currentUser == null) {
        LoginScreen(
            viewModel = viewModel,
            onBackClick = onBackClick,
            showTopBar = false,
            showSocialAndGuest = false,
            onContinueAsGuest = {
                viewModel.navigateTo(ScreenRoute.Home.route)
            }
        )
        return
    }

    val isOwnerVerified by viewModel.isOwnerVerified.collectAsState()
    val securityLogs by viewModel.securityLogs.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showPrivacySettingsDialog by remember { mutableStateOf(false) }
    var showAppUpdateDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsPrefs = remember { context.getSharedPreferences("met_ghamr_user_settings", android.content.Context.MODE_PRIVATE) }
    var isNotifsEnabled by remember { mutableStateOf(settingsPrefs.getBoolean("notifications_master_enabled", true)) }

    var editName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editPhone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            val user = currentUser!!

                // --- 1. PROFILE HEADER ---
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                .background(SkyBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                color = Color.White,
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

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProviderBadge(provider = user.providerType)
                            if (user.isSuperAdmin || isOwnerVerified) {
                                SuperAdminBadge()
                            }
                        }

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

                // Super Admin Exclusive Management Panel Banner
                if (user.isSuperAdmin || isOwnerVerified) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "صلاحيات مدير النظام الأعلى (Super Admin)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "مرحباً بك يا باشمهندس محمد كشك • تحكم كامل بالمنصة",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "حسابك يمتلك أعلى رتبة إدارية في النظام مع صلاحيات الإشراف التام، إدارة البيانات، النسخ الاحتياطي، المراجعات، وحسابات المستخدمين.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { viewModel.navigateTo(ScreenRoute.AdminDashboard.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("super_admin_enter_dashboard_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("الدخول للوحة الإدارة الشاملة (Super Admin)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
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

                        if (isOwnerVerified || user.isSuperAdmin) {
                            ProfileMenuItem(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "لوحة تحكم مدير النظام الأعلى",
                                subtitle = "الإشراف التام، النسخ الاحتياطي، الأنشطة والمستخدمين",
                                onClick = { viewModel.navigateTo(ScreenRoute.AdminDashboard.route) },
                                tag = "profile_menu_admin"
                            )

                            HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                        }

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

                // --- 2.5 OWNER SECURITY VAULT CARD ---
                if (isOwnerVerified) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MetGhamrGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "لوحة أمان المالك ورمز PIN",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    )
                                }

                                Surface(
                                    color = MetGhamrGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "محمي ومشفر 🛡️",
                                        color = MetGhamrGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "التطبيق محمي بحساب المالك (${AppSecurityManager.AUTHORIZED_OWNER_EMAIL}) ورمز PIN السري، مع درع لمنع التخمين وحظر الروابط غير المصرح بها.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.lockApp() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrGold),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("lock_app_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MetGhamrNavy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "قفل التطبيق الآن",
                                        color = MetGhamrNavy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showAuditLogsDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrGold.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("view_security_logs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MetGhamrGoldLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "سجل الأمان",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
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
                            subtitle = if (isNotifsEnabled) "مفعلة لإشعارات الخدمات الجديدة" else "متوقفة - انقر للضبط والتفعيل",
                            onClick = { showNotificationSettingsDialog = true },
                            tag = "profile_menu_notifs"
                        )

                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)

                        ProfileMenuItem(
                            icon = Icons.Default.Security,
                            title = "حماية الخصوصية",
                            subtitle = "بياناتك الشخصية مشفرة وآمنة",
                            onClick = { showPrivacySettingsDialog = true },
                            tag = "profile_menu_privacy"
                        )
                    }
                }

                // --- APP ACTIONS CARD (SHARE, RATE, UPDATE) WITH GRADIENT ---
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        Brush.horizontalGradient(
                            listOf(
                                MetGhamrGold.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.3f),
                                MetGhamrGold.copy(alpha = 0.6f)
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_actions_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0B2239),
                                        Color(0xFF163E66),
                                        Color(0xFF1D5286),
                                        Color(0xFF0D253F)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // رأس البطاقة
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = MetGhamrGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "تطبيق دليل ميت غمر",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                        )
                                        Text(
                                            text = "على متجر Google Play الرسمي",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                                Surface(
                                    color = MetGhamrGold.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = MetGhamrGold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الإصدار الرسمي",
                                            color = MetGhamrGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            val playStoreAppUrl = "https://play.google.com/store/apps/details?id=com.dalil.mit3mr.app"

                            // الصف الأول: زران (مشاركة التطبيق + تقييم التطبيق)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1) زر مشاركة التطبيق
                                Button(
                                    onClick = {
                                        try {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "حمل الآن تطبيق دليل ميت غمر الرسمي - دليلك الشامل لمدينة ميت غمر وجميع قراها (أطباء، صيدليات، مطاعم، محلات، خدمات وأرقام الطوارئ):\n$playStoreAppUrl"
                                                )
                                                type = "text/plain"
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "مشاركة تطبيق دليل ميت غمر")
                                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(shareIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تعذر فتح نافذة المشاركة", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = MetGhamrNavy
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_app_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مشاركة التطبيق",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // 2) زر تقييم التطبيق
                                Button(
                                    onClick = {
                                        try {
                                            val marketIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=com.dalil.mit3mr.app")
                                            ).apply {
                                                setPackage("com.android.vending")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(marketIntent)
                                        } catch (e: Exception) {
                                            try {
                                                val genericMarket = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("market://details?id=com.dalil.mit3mr.app")
                                                ).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(genericMarket)
                                            } catch (e2: Exception) {
                                                val webIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(playStoreAppUrl)
                                                ).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(webIntent)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFF9E6),
                                        contentColor = Color(0xFF8B6400)
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("rate_app_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تقييم التطبيق",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // 3) زر عريض تحتهما: تحديث التطبيق بلون ذهبي بارز
                            Button(
                                onClick = {
                                    showAppUpdateDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MetGhamrGold,
                                    contentColor = MetGhamrNavy
                                ),
                                contentPadding = PaddingValues(vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("update_app_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MetGhamrNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تحديث التطبيق",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.5.sp
                                )
                            }
                        }
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
                val profileFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = MetGhamrNavy,
                    unfocusedBorderColor = BorderLight,
                    focusedLabelColor = MetGhamrNavy,
                    unfocusedLabelColor = TextSecondary
                )
                val profileTextStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 14.sp)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        textStyle = profileTextStyle,
                        colors = profileFieldColors,
                        label = { Text("الاسم الظاهر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        textStyle = profileTextStyle,
                        colors = profileFieldColors,
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

    // Security Audit Logs Dialog
    if (showAuditLogsDialog) {
        SecurityAuditLogsDialog(
            logs = securityLogs,
            onDismiss = { showAuditLogsDialog = false }
        )
    }

    // Notification Settings Dialog
    if (showNotificationSettingsDialog) {
        NotificationSettingsDialog(
            onDismiss = { showNotificationSettingsDialog = false },
            onSettingsChanged = { isNotifsEnabled = it }
        )
    }

    // Privacy & Security Settings Dialog
    if (showPrivacySettingsDialog) {
        PrivacySecurityDialog(
            viewModel = viewModel,
            onDismiss = { showPrivacySettingsDialog = false }
        )
    }

    // App Update Dialog (Exact match to requested popup design)
    if (showAppUpdateDialog) {
        AppUpdateDialog(
            onDismiss = { showAppUpdateDialog = false }
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
