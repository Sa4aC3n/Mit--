package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminAuditLogsScreen(viewModel: DirectoryViewModel) {
    val auditLogs by viewModel.adminAuditLogs.collectAsState()

    AdminLayout(
        viewModel = viewModel,
        title = "سجل الأمان والنشاط الإداري",
        currentRoute = ScreenRoute.AdminAuditLogs.route
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "سجل الحركات الإدارية والأمنية المسجلة (Append-Only):",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MetGhamrNavy
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (auditLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد سجلات نشاط إدارية مسجلة حتى الآن.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(auditLogs, key = { it.id }) { log ->
                        val dateFormatted = remember(log.timestamp) {
                            SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(log.timestamp))
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    color = MetGhamrNavy.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.padding(8.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            log.actorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MetGhamrNavy
                                        )
                                        Text(
                                            dateFormatted,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "الإجراء: ${log.action} (${log.entityType})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MetGhamrGoldDark
                                    )
                                    if (log.detailsJson.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            log.detailsJson,
                                            fontSize = 11.sp,
                                            color = TextDark
                                        )
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
