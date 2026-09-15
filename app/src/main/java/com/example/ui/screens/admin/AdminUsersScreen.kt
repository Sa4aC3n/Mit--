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
import com.example.data.model.UserAccountEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: DirectoryViewModel) {
    val users by viewModel.adminUserAccounts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var showSuspendDialog by remember { mutableStateOf<UserAccountEntity?>(null) }
    var suspendReason by remember { mutableStateOf("") }

    val filteredList = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true) }
    }

    AdminLayout(
        viewModel = viewModel,
        title = "إدارة حسابات المستخدمين",
        currentRoute = ScreenRoute.AdminUsers.route
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم المستخدم أو البريد...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MetGhamrNavy) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("إجمالي الحسابات: ${filteredList.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد مستخدمون مسجلون بعد.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { user ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MetGhamrNavy)
                                            if (user.role == "SUPER_ADMIN" || user.email.trim().equals("m.k3shka@gmail.com", ignoreCase = true)) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFF0F172A),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("مدير النظام الأعلى", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(user.email, fontSize = 12.sp, color = TextMuted)
                                    }
                                    Surface(
                                        color = if (user.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (user.status == "ACTIVE") "نشط" else "محظور",
                                            color = if (user.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (!user.suspensionReason.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("سبب الحظر: ${user.suspensionReason}", fontSize = 11.sp, color = Color(0xFFC62828))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    val isSuperAdminUser = user.role == "SUPER_ADMIN" || user.email.trim().equals("m.k3shka@gmail.com", ignoreCase = true)
                                    if (isSuperAdminUser) {
                                        Surface(
                                            color = Color(0xFF0F172A).copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Shield, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حساب محمي بصلاحيات المالك الأعلى", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                            }
                                        }
                                    } else if (user.status == "ACTIVE") {
                                        OutlinedButton(
                                            onClick = {
                                                showSuspendDialog = user
                                                suspendReason = ""
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حظر الحساب", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.restoreUserAdmin(user.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إلغاء الحظر وتنشيط", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSuspendDialog != null) {
            val user = showSuspendDialog!!
            AlertDialog(
                onDismissRequest = { showSuspendDialog = null },
                title = { Text("حظر حساب (${user.name})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MetGhamrNavy) },
                text = {
                    Column {
                        Text("يرجى ذكر سبب حظر حساب هذا المستخدم:", fontSize = 13.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = suspendReason,
                            onValueChange = { suspendReason = it },
                            placeholder = { Text("مثال: مخالفة شروط الاستخدام، نشر تقييمات وهمية...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (suspendReason.isNotBlank()) {
                                viewModel.suspendUserAdmin(user.id, suspendReason.trim())
                                showSuspendDialog = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        enabled = suspendReason.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حظر الحساب", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showSuspendDialog = null }, shape = RoundedCornerShape(8.dp)) {
                        Text("إلغاء")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
