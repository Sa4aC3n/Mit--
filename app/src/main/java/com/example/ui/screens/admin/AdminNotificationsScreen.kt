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

    AdminLayout(
        viewModel = viewModel,
        title = "منشئ ومُرسل الإشعارات الجماعية",
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
                            "إرسال إشعار مباشر لجميع مستخدمي التطبيق:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("عنوان الإشعار:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("مثال: تحديثات جديدة في دليل مطاعم ميت غمر 🍔") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("نص ورسالة الإشعار:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        placeholder = { Text("اكتب تفاصيل الإشعار بوضوح هنا...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank() && body.isNotBlank()) {
                                viewModel.sendBroadcastNotificationAdmin(title.trim(), body.trim())
                                title = ""
                                body = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                        shape = RoundedCornerShape(10.dp),
                        enabled = title.isNotBlank() && body.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إرسال الإشعار الآن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
