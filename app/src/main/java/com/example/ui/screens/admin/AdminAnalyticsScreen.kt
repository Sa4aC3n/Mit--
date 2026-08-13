package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@Composable
fun AdminAnalyticsScreen(viewModel: DirectoryViewModel) {
    val dataQuality by viewModel.dataQualityMetrics.collectAsState()

    AdminLayout(
        viewModel = viewModel,
        title = "تحليلات أداء وجودة البيانات",
        currentRoute = ScreenRoute.AdminAnalytics.route
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MetGhamrNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تحليل وتدقيق جودة النواقص ببيانات الدليل:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MetGhamrNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        QualityProgressRow(
                            label = "أنشطة بدون أرقام هواتف",
                            count = dataQuality.missingPhoneCount,
                            total = dataQuality.totalBusinesses,
                            color = Color(0xFFD32F2F)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        QualityProgressRow(
                            label = "أنشطة بدون مواعيد عمل محددة",
                            count = dataQuality.missingHoursCount,
                            total = dataQuality.totalBusinesses,
                            color = Color(0xFFFFA000)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        QualityProgressRow(
                            label = "أنشطة بدون عناوين تفصيلية",
                            count = dataQuality.missingAddressCount,
                            total = dataQuality.totalBusinesses,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MetGhamrNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "نسب توزيع الأنشطة على التصنيفات الرئيسية:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MetGhamrNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val categories = listOf(
                            "مطاعم وكافيهات" to 0.28f,
                            "عيادات وأطباء" to 0.24f,
                            "فنيين وصنايعية" to 0.18f,
                            "صيدليات ومستشفيات" to 0.16f,
                            "مصانع وورش" to 0.14f
                        )

                        categories.forEach { (cat, pct) ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text("${(pct * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrGoldDark)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MetGhamrNavy,
                                    trackColor = MetGhamrCream
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // --- AI Assistant Analytics & Admin Controls ---
            item {
                val aiAnalytics by viewModel.aiAnalytics.collectAsState()
                val aiConfig by viewModel.aiConfig.collectAsState()
                var isAiEnabled by remember(aiConfig) { mutableStateOf(aiConfig.isAiEnabled) }
                var selectedModel by remember(aiConfig) { mutableStateOf(aiConfig.modelName) }
                var dailyGuestLimit by remember(aiConfig) { mutableStateOf(aiConfig.dailyGuestLimit.toString()) }
                var dailyUserLimit by remember(aiConfig) { mutableStateOf(aiConfig.dailyUserLimit.toString()) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤖", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "تحليلات وإعدادات المساعد الذكي (AI Analytics):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MetGhamrNavy
                                )
                            }

                            Switch(
                                checked = isAiEnabled,
                                onCheckedChange = {
                                    isAiEnabled = it
                                    viewModel.updateAiConfig(aiConfig.copy(isAiEnabled = it))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MetGhamrGold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Metric Grid Cards
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MetGhamrBlue.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("أسئلة اليوم 📊", fontSize = 11.sp, color = TextSecondary)
                                    Text("${aiAnalytics.questionsToday}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MetGhamrGold.copy(alpha = 0.12f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("أسئلة الشهر 📅", fontSize = 11.sp, color = TextSecondary)
                                    Text("${aiAnalytics.questionsThisMonth}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MetGhamrGoldDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = OpenGreen.copy(alpha = 0.1f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("معدل الاستجابة ⚡", fontSize = 11.sp, color = TextSecondary)
                                    Text("${aiAnalytics.avgResponseTimeMs} ms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OpenGreen)
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MetGhamrNavy.copy(alpha = 0.08f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("التكلفة التقديرية 💵", fontSize = 11.sp, color = TextSecondary)
                                    Text("${aiAnalytics.estimatedMonthlyCostUsd} /شهر", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("🔥 الأقسام الأكثر استعلاماً بالذكاء الاصطناعي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                        Spacer(modifier = Modifier.height(6.dp))
                        aiAnalytics.mostAskedCategories.forEach { (catName, qCount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• $catName", fontSize = 11.sp, color = TextDark)
                                Text("$qCount سؤال", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("⚙️ تحكم المشرف الفائق (Super Admin Settings):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = dailyGuestLimit,
                                onValueChange = { dailyGuestLimit = it },
                                label = { Text("حد الزائر/يوم", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = dailyUserLimit,
                                onValueChange = { dailyUserLimit = it },
                                label = { Text("حد المسجل/يوم", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val gLimit = dailyGuestLimit.toIntOrNull() ?: 15
                                val uLimit = dailyUserLimit.toIntOrNull() ?: 50
                                viewModel.updateAiConfig(
                                    aiConfig.copy(
                                        dailyGuestLimit = gLimit,
                                        dailyUserLimit = uLimit,
                                        isAiEnabled = isAiEnabled
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ إعدادات المساعد الذكي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QualityProgressRow(label: String, count: Int, total: Int, color: Color) {
    val pct = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Text("$count من أصل $total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
