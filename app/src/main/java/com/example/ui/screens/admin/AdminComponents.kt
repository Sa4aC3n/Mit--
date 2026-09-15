package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminKPIsSummary
import com.example.data.model.UserContributionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLayout(
    viewModel: DirectoryViewModel,
    title: String,
    currentRoute: String,
    onBackClick: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val kpis by viewModel.adminKPIs.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0F172A),
                drawerContentColor = Color.White,
                modifier = Modifier.width(310.dp)
            ) {
                val drawerScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(drawerScrollState)
                ) {
                    AdminDrawerHeader(kpis = kpis)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        AdminDrawerItem(
                            icon = Icons.Default.Dashboard,
                            label = "لوحة التحكم الرئيسية",
                            selected = currentRoute == ScreenRoute.AdminDashboard.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminDashboard.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.Storefront,
                            label = "إدارة الأنشطة التجاريّة",
                            selected = currentRoute == ScreenRoute.AdminBusinesses.route,
                            badge = if (kpis.pendingBusinesses > 0) "${kpis.pendingBusinesses}" else null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminBusinesses.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.FolderSpecial,
                            label = "إدارة التصنيفات",
                            selected = currentRoute == ScreenRoute.AdminCategories.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminCategories.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.RateReview,
                            label = "إدارة المساهمات",
                            selected = currentRoute == ScreenRoute.AdminContributions.route,
                            badge = if (kpis.pendingContributions > 0) "${kpis.pendingContributions}" else null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminContributions.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.ReportProblem,
                            label = "إدارة البلاغات والشكاوى",
                            selected = currentRoute == ScreenRoute.AdminReports.route,
                            badge = if (kpis.openReports > 0) "${kpis.openReports}" else null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminReports.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.People,
                            label = "إدارة المستخدمين",
                            selected = currentRoute == ScreenRoute.AdminUsers.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminUsers.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.StarHalf,
                            label = "التقييمات والمراجعات",
                            selected = currentRoute == ScreenRoute.AdminReviews.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminReviews.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.Notifications,
                            label = "إرسال الإشعارات",
                            selected = currentRoute == ScreenRoute.AdminNotifications.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminNotifications.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.BarChart,
                            label = "التحليلات وجودة البيانات",
                            selected = currentRoute == ScreenRoute.AdminAnalytics.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminAnalytics.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.History,
                            label = "سجل النشاط الإداري",
                            selected = currentRoute == ScreenRoute.AdminAuditLogs.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminAuditLogs.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.AdminPanelSettings,
                            label = "إعدادات الأدوار والأمان",
                            selected = currentRoute == ScreenRoute.AdminSettings.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminSettings.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.Backup,
                            label = "🗄️ إدارة البيانات والنسخ الاحتياطي",
                            selected = currentRoute == ScreenRoute.AdminDataManagement.route,
                            badge = null
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminDataManagement.route)
                        }

                        AdminDrawerItem(
                            icon = Icons.Default.SmartToy,
                            label = "🧠 محرك جمع وإثراء البيانات (Data Engine)",
                            selected = currentRoute == ScreenRoute.AdminDataCollector.route,
                            badge = "جديد"
                        ) {
                            scope.launch { drawerState.close() }
                            viewModel.navigateTo(ScreenRoute.AdminDataCollector.route)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            onClick = {
                                viewModel.navigateTo(ScreenRoute.Home.route)
                            },
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = SkyBluePrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "الخروج من لوحة التحكم",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "دليل ميت غمر • Ajilika Tech",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (onBackClick != null) {
                                onBackClick()
                            } else {
                                scope.launch { drawerState.open() }
                            }
                        }) {
                            Icon(
                                imageVector = if (onBackClick != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                                contentDescription = "القائمة",
                                tint = SkyBluePrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshAdminKPIs() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث البيانات", tint = SkyBluePrimary)
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SkyBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SkyBlueDark, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceCard)
                )
            },
            containerColor = SurfaceLight
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}

@Composable
fun AdminDrawerHeader(kpis: AdminKPIsSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SkyBluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "لوحة الإشراف الإداري",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    "م. محمد كشك • مدير النظام الأعلى 👑",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "m.k3shka@gmail.com",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BadgeChip(label = "معلقة", count = kpis.pendingContributions, color = Color(0xFFF59E0B))
            BadgeChip(label = "بلاغات", count = kpis.openReports, color = WatermelonRed)
        }
    }
}

@Composable
fun BadgeChip(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "$label: $count",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AdminDrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) SkyBluePrimary.copy(alpha = 0.25f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) SkyBlueLight else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = if (selected) SkyBlueLight else Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(WatermelonRed)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        subtitle,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MetGhamrNavy
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                title,
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ContributionDiffCard(
    contribution: UserContributionEntity
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MetGhamrCream.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Compare, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "مقارنة البيانات (الفارق المطلوب تعديله):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MetGhamrNavy
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (contribution.fieldName != null) {
                Text(
                    "الحقل المستهدف: ${contribution.fieldName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MetGhamrGoldDark
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                // Current Data
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(10.dp)
                ) {
                    Text("القيمة الحالية بالدليل:", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        contribution.oldValue ?: "لا يوجد",
                        fontSize = 12.sp,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Proposed Data
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(10.dp)
                ) {
                    Text("القيمة الجديدة المقترحة:", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        contribution.newValue ?: "غير محدد",
                        fontSize = 12.sp,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "تأكيد",
    confirmColor: Color = MetGhamrNavy,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MetGhamrNavy) },
        text = { Text(message, fontSize = 13.sp, color = TextDark) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إلغاء", color = TextMuted)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
