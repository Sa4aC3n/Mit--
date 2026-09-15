package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
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
fun AdminNotificationsScreen(viewModel: DirectoryViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("NEW_BUSINESS") } // NEW_BUSINESS, OFFER, UPDATE

    val notificationTypes = listOf(
        Triple("NEW_BUSINESS", "🏪 نشاط جديد", MetGhamrNavy),
        Triple("OFFER", "🏷️ عرض ترويجي", MetGhamrGold),
        Triple("UPDATE", "📢 تحديث عام", MetGhamrGreen)
    )

    AdminLayout(
        viewModel = viewModel,
        title = "منشئ ومُرسل الإشعارات والعروض",
        currentRoute = ScreenRoute.AdminNotifications.route
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MetGhamrNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "إرسال إشعار مباشر لجميع هواتف مستخدمي ميت غمر:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("نوع الإشعار والتنبيه:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Type Selector Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        notificationTypes.forEach { (typeKey, label, color) ->
                            val isSelected = selectedType == typeKey
                            OutlinedButton(
                                onClick = { selectedType = typeKey },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else BorderLight
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                selectedType = "NEW_BUSINESS"
                                title = "افتتاح صيدلية د. محمود الحديثة"
                                businessName = "صيدلية د. محمود"
                                body = "صيدلية متكاملة تقدم خدمة 24 ساعة وتوصيل الأدوية لكافة أنحاء ميت غمر."
                            },
                            label = { Text("نموذج نشاط جديد 🏪", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = {
                                selectedType = "OFFER"
                                title = "خصم 30% على وجبات التوفير العائلية"
                                businessName = "مطعم ومشاوي البرنس"
                                body = "استمتع بخصم خاص وحصري لعملاء دليل ميت غمر حتى نهاية الأسبوع."
                            },
                            label = { Text("نموذج عرض ترويجي 🏷️", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedType == "NEW_BUSINESS" || selectedType == "OFFER") {
                        Text("اسم النشاط التجاري (اختياري):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            placeholder = { Text("مثال: مطعم ومشاوي البرنس") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text("عنوان الإشعار:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                if (selectedType == "OFFER") "مثال: خصم 20% بمناسبة الافتتاح 🏷️"
                                else "مثال: نشاط جديد: صيدلية الأمل 🏪"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("تفاصيل ورسالة الإشعار:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        placeholder = { Text("اكتب تفاصيل العرض أو النشاط بوضوح هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank() && body.isNotBlank()) {
                                viewModel.sendBroadcastNotificationAdmin(
                                    title = title.trim(),
                                    body = body.trim(),
                                    type = selectedType,
                                    businessName = businessName.trim().ifBlank { null }
                                )
                                title = ""
                                body = ""
                                businessName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (selectedType) {
                                "OFFER" -> MetGhamrGold
                                "UPDATE" -> MetGhamrGreen
                                else -> MetGhamrNavy
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = title.isNotBlank() && body.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (selectedType) {
                                "OFFER" -> "نشر وإرسال العرض الترويجي الآن 🏷️"
                                "NEW_BUSINESS" -> "إرسال تنبيه النشاط الجديد الآن 🏪"
                                else -> "إرسال الإشعار لجميع المستخدمين 📢"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
