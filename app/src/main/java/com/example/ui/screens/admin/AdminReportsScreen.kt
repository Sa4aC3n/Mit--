package com.example.ui.screens.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@Composable
fun AdminReportsScreen(viewModel: DirectoryViewModel) {
    val reports by viewModel.adminReports.collectAsState()

    AdminLayout(
        viewModel = viewModel,
        title = "إدارة البلاغات والشكاوى",
        currentRoute = ScreenRoute.AdminReports.route
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "بلاغات المستخدمين المرفوعة (${reports.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MetGhamrNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (reports.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد بلاغات مفتوحة حالياً 👍", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(reports, key = { it.id }) { report ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFD32F2F))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(report.reason, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                    }

                                    val isResolved = report.status == "RESOLVED"
                                    Surface(
                                        color = if (isResolved) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (isResolved) "تم الحل" else "مفتوح",
                                            color = if (isResolved) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("التفاصيل: ${report.description}", fontSize = 12.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("مستهدف مراجعة ID: ${report.reviewId}", fontSize = 11.sp, color = TextMuted)

                                if (report.status != "RESOLVED") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = { viewModel.resolveReportAdmin(report.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("معالجة وإغلاق البلاغ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
}
