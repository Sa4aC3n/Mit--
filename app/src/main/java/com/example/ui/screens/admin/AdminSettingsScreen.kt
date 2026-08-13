package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
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
fun AdminSettingsScreen(viewModel: DirectoryViewModel) {
    AdminLayout(
        viewModel = viewModel,
        title = "إعدادات الأمان والأدوار",
        currentRoute = ScreenRoute.AdminSettings.route
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
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MetGhamrNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "مصفوفة الأدوار والصلاحيات (Permissions Matrix):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MetGhamrNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        PermissionMatrixRow("SUPER_ADMIN", "مدير النظام الأعلى", "جميع الصلاحيات والإشراف التام")
                        PermissionMatrixRow("ADMIN", "مدير عام", "إدارة الأنشطة، التقييمات، البلاغات والمستخدمين")
                        PermissionMatrixRow("MODERATOR", "مشرف محتوى", "اعتماد ورفض المساهمات واقتراحات التعديل")
                        PermissionMatrixRow("SUPPORT", "دعم فني", "استلام ومتابعة البلاغات والشكاوى فقط")
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
                            Icon(Icons.Default.Security, contentDescription = null, tint = MetGhamrNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "سياسات الأمان والمصادقة الإدارية:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MetGhamrNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SecurityPolicyItem("التحقق من الصلاحيات بالخلفية (Server-Side Authorization)")
                        SecurityPolicyItem("تسجيل السجلات الأمنية غير القابلة للتعديل (Append-Only Audit Logs)")
                        SecurityPolicyItem("طلب تأكيد إضافي قبل تنفيذ العمليات الحساسة (Re-authentication)")
                        SecurityPolicyItem("حجب الكلمات السرية والـ Tokens عن جانب العميل (Zero Trust)")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MetGhamrGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دليل ميت غمر - إصدار لوحة التحكم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("الإصدار: v1.0.0 Pro • بيئة التشغيل: Production", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("طوّر بواسطة: Ajilika Technologies — Egyptian Roots. Digital Future.", color = MetGhamrGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionMatrixRow(roleKey: String, roleName: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(roleName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)
            }
            Surface(
                color = MetGhamrCream,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(roleKey, fontSize = 10.sp, color = MetGhamrNavy, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(description, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(start = 22.dp))
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun SecurityPolicyItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MetGhamrGold, shape = RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextDark)
    }
}
