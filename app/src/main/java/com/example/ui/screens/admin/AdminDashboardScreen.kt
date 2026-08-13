package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@Composable
fun AdminDashboardScreen(viewModel: DirectoryViewModel) {
    val kpis by viewModel.adminKPIs.collectAsState()
    val dataQuality by viewModel.dataQualityMetrics.collectAsState()
    val pendingContributions by viewModel.adminContributions.collectAsState()
    val openReports by viewModel.adminReports.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshAdminKPIs()
    }

    AdminLayout(
        viewModel = viewModel,
        title = "لوحة التحكم الإدارية",
        currentRoute = ScreenRoute.AdminDashboard.route
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "أهلاً بك في نظام الإدارة ⚡",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "مراجعة واعتَماد بيانات دليل ميت غمر لحظة بلحظة",
                                    color = MetGhamrGold,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = MetGhamrGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Real KPI Summary Grid
            item {
                Text(
                    "مؤشرات الأداء الحقيقية (Real KPIs):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MetGhamrNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "الأنشطة النشطة",
                        value = "${kpis.activeBusinesses}",
                        subtitle = "إجمالي ${kpis.totalBusinesses}",
                        icon = Icons.Default.Storefront,
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenRoute.AdminBusinesses.route) }
                    )
                    StatCard(
                        title = "مساهمات معلقة",
                        value = "${kpis.pendingContributions}",
                        subtitle = "طلب فحص",
                        icon = Icons.Default.RateReview,
                        accentColor = Color(0xFFFFA000),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenRoute.AdminContributions.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "البلاغات والشكاوى",
                        value = "${kpis.openReports}",
                        subtitle = "مفتوحة",
                        icon = Icons.Default.ReportProblem,
                        accentColor = Color(0xFFD32F2F),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenRoute.AdminReports.route) }
                    )
                    StatCard(
                        title = "إجمالي المستخدمين",
                        value = "${kpis.totalUsers}",
                        subtitle = "حسابات مسجلة",
                        icon = Icons.Default.People,
                        accentColor = MetGhamrNavy,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(ScreenRoute.AdminUsers.route) }
                    )
                }
            }

            // Quick Actions Section
            item {
                Text(
                    "الوصول السريع للمهام:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MetGhamrNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "المساهمات (${kpis.pendingContributions})",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = MetGhamrGoldDark,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(ScreenRoute.AdminContributions.route)
                    }
                    QuickActionButton(
                        title = "إرسال إشعار",
                        icon = Icons.Default.NotificationsActive,
                        color = MetGhamrNavy,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(ScreenRoute.AdminNotifications.route)
                    }
                    QuickActionButton(
                        title = "جودة البيانات",
                        icon = Icons.Default.HealthAndSafety,
                        color = Color(0xFF00897B),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(ScreenRoute.AdminAnalytics.route)
                    }
                }
            }

            // Data Quality Alert Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MetGhamrNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ملخص جودة وتحسين بيانات الدليل:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MetGhamrNavy
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DataMetricPill("بدون هاتف", "${dataQuality.missingPhoneCount}", Color(0xFFD32F2F))
                            DataMetricPill("بدون مواعيد", "${dataQuality.missingHoursCount}", Color(0xFFFFA000))
                            DataMetricPill("بدون عنوان", "${dataQuality.missingAddressCount}", Color(0xFF1976D2))
                        }
                    }
                }
            }

            // Recent Pending Contributions Action List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "آخر طلبات المساهمة المعلقة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MetGhamrNavy
                    )
                    TextButton(onClick = { viewModel.navigateTo(ScreenRoute.AdminContributions.route) }) {
                        Text("عرض الكل", color = MetGhamrGoldDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val pendingList = pendingContributions.filter { it.status == "PENDING" }.take(3)
            if (pendingList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد مساهمات معلقة بحاجة للمراجعة الآن ✨", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(pendingList, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${item.humanReadableId} • ${item.businessName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MetGhamrNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "بواسطة: ${item.userName} (${item.userEmail})",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Button(
                                onClick = { viewModel.navigateTo(ScreenRoute.AdminContributions.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrGold),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("فحص", color = MetGhamrNavy, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DataMetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        Text(label, fontSize = 10.sp, color = TextMuted)
    }
}
