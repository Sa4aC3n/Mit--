package com.example.ui.screens.admin

import androidx.compose.foundation.background
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
import com.example.data.model.UserContributionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContributionsScreen(viewModel: DirectoryViewModel) {
    val contributions by viewModel.adminContributions.collectAsState()
    var selectedFilterTab by remember { mutableIntStateOf(1) } // 0: All, 1: Pending, 2: Approved, 3: Rejected

    var showRejectDialog by remember { mutableStateOf<UserContributionEntity?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    val filteredList = remember(contributions, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> contributions.filter { it.status == "PENDING" }
            2 -> contributions.filter { it.status == "APPROVED" }
            3 -> contributions.filter { it.status == "REJECTED" }
            else -> contributions
        }
    }

    AdminLayout(
        viewModel = viewModel,
        title = "إدارة ومراجعة المساهمات",
        currentRoute = ScreenRoute.AdminContributions.route
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = Color.White,
                contentColor = MetGhamrNavy,
                edgePadding = 0.dp
            ) {
                Tab(selected = selectedFilterTab == 1, onClick = { selectedFilterTab = 1 }) {
                    Text("قيد المراجعة (${contributions.count { it.status == "PENDING" }})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 0, onClick = { selectedFilterTab = 0 }) {
                    Text("الكل (${contributions.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 2, onClick = { selectedFilterTab = 2 }) {
                    Text("المقبولة (${contributions.count { it.status == "APPROVED" }})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 3, onClick = { selectedFilterTab = 3 }) {
                    Text("المرفوضة (${contributions.count { it.status == "REJECTED" }})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد مساهمات في هذا التبويب.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        ContributionAdminCard(
                            contribution = item,
                            onApprove = {
                                viewModel.approveContributionAdmin(item.id, "تم الاعتماد والمراجعة بواسطة المشرف")
                            },
                            onReject = {
                                showRejectDialog = item
                                rejectReason = ""
                            }
                        )
                    }
                }
            }
        }

        // Reject Dialog with required reason
        if (showRejectDialog != null) {
            val item = showRejectDialog!!
            AlertDialog(
                onDismissRequest = { showRejectDialog = null },
                title = { Text("رفض طلب المساهمة (${item.humanReadableId})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MetGhamrNavy) },
                text = {
                    Column {
                        Text("يرجى ذكر سبب الرفض بوضوح ليتم إرساله للمستخدم:", fontSize = 13.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            placeholder = { Text("مثال: بيانات مكررة، رقم هاتف غير صحيح، إلخ...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rejectReason.isNotBlank()) {
                                viewModel.rejectContributionAdmin(item.id, rejectReason.trim())
                                showRejectDialog = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        enabled = rejectReason.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تأكيد الرفض", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRejectDialog = null }, shape = RoundedCornerShape(8.dp)) {
                        Text("إلغاء")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun ContributionAdminCard(
    contribution: UserContributionEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MetGhamrNavy.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            contribution.humanReadableId,
                            color = MetGhamrNavy,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (contribution.type) {
                            "ADD_BUSINESS" -> "إضافة نشاط جديد"
                            "SUGGEST_EDIT" -> "اقتراح تعديل"
                            else -> "إبلاغ عن خطأ"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MetGhamrGoldDark
                    )
                }

                // Status Badge
                Surface(
                    color = when (contribution.status) {
                        "APPROVED" -> Color(0xFFE8F5E9)
                        "REJECTED" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFFFF8E1)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        when (contribution.status) {
                            "APPROVED" -> "تمت الموافقة"
                            "REJECTED" -> "مرفوض"
                            else -> "قيد الفحص"
                        },
                        color = when (contribution.status) {
                            "APPROVED" -> Color(0xFF2E7D32)
                            "REJECTED" -> Color(0xFFC62828)
                            else -> Color(0xFFF57F17)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "اسم النشاط: ${contribution.businessName}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MetGhamrNavy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "مُقدم الطلب: ${contribution.userName} (${contribution.userEmail})",
                fontSize = 12.sp,
                color = TextMuted
            )

            if (!contribution.userReason.isNull_or_blank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "ملاحظات/سبب المستخدم: \"${contribution.userReason}\"",
                    fontSize = 12.sp,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ADD_BUSINESS Details Preview
            if (contribution.type == "ADD_BUSINESS" && !contribution.payloadJson.isNullOrBlank()) {
                val json = try { org.json.JSONObject(contribution.payloadJson) } catch (e: Exception) { null }
                if (json != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val catName = json.optString("categoryName")
                            val spec = json.optString("specialization")
                            val phone = json.optString("phone")
                            val secPhone = json.optString("secondaryPhone")
                            val whatsapp = json.optString("whatsapp")
                            val address = json.optString("address")
                            val city = json.optString("city")
                            val hours = json.optString("workingHours")
                            val maps = json.optString("googleMapsUrl")
                            val imagesCount = json.optJSONArray("imageUrls")?.length() ?: 0

                            Text("📋 تفاصيل النشاط الجديد المقدمة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MetGhamrNavy)
                            if (catName.isNotBlank() || spec.isNotBlank()) {
                                Text("📁 التصنيف والتخصص: $catName ${if (spec.isNotBlank()) "«$spec»" else ""}", fontSize = 12.sp)
                            }
                            if (phone.isNotBlank()) Text("📞 الهاتف: $phone ${if (secPhone.isNotBlank()) " | إضافي: $secPhone" else ""}", fontSize = 12.sp)
                            if (whatsapp.isNotBlank()) Text("💬 واتس آب: $whatsapp", fontSize = 12.sp, color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                            if (address.isNotBlank() || city.isNotBlank()) Text("📍 العنوان: $city - $address", fontSize = 12.sp)
                            if (hours.isNotBlank()) Text("⏱️ المواعيد: $hours", fontSize = 12.sp)
                            if (maps.isNotBlank()) Text("🗺️ الخريطة: $maps", fontSize = 11.sp, color = MetGhamrNavy, maxLines = 1)
                            if (imagesCount > 0) Text("📸 عدد الصور: $imagesCount صور", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrGoldDark)
                        }
                    }
                }
            }

            // DIFF VIEW FOR SUGGEST_EDIT
            if (contribution.type == "SUGGEST_EDIT" && contribution.oldValue != null && contribution.newValue != null) {
                ContributionDiffCard(contribution = contribution)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (contribution.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفض الطلب", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اعتماد وموافقة", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
