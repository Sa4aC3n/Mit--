package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContributionStatus
import com.example.data.model.ContributionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionDetailScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val contribution by viewModel.selectedContribution.collectAsState()
    val scrollState = rememberScrollState()

    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    if (contribution == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MetGhamrNavy)
        }
        return
    }

    val item = contribution!!

    val statusEnum = try {
        ContributionStatus.valueOf(item.status)
    } catch (_: Exception) {
        ContributionStatus.PENDING
    }

    val typeEnum = try {
        ContributionType.valueOf(item.type)
    } catch (_: Exception) {
        ContributionType.OTHER
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تفاصيل المساهمة ${item.humanReadableId}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MetGhamrNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.humanReadableId,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MetGhamrNavy
                        )

                        Surface(
                            color = Color(statusEnum.badgeColorHex).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusEnum.displayNameAr,
                                color = Color(statusEnum.badgeColorHex),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.businessName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetGhamrNavy
                    )

                    Text(
                        text = "نوع الطلب: ${typeEnum.displayNameAr}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Text(
                        text = "تاريخ الإرسال: ${formatTimestampArabic(item.createdAt)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Timeline Tracker Step
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "مسار المراجعة والاعتماد ⏱️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MetGhamrNavy
                    )

                    TimelineNode(
                        title = "تم استلام الطلب",
                        subtitle = "تم تسجيل مساهمتك بنجاح في قاعدة البيانات بكود ${item.humanReadableId}",
                        isCompleted = true,
                        isCurrent = item.status == "PENDING"
                    )

                    TimelineNode(
                        title = "جاري الفحص والمراجعة",
                        subtitle = "يقوم فريق ميت غمر للتدقيق بمراجعة البيانات المدخلة وتأكيد صحتها",
                        isCompleted = item.status in listOf("APPROVED", "REJECTED", "UNDER_REVIEW"),
                        isCurrent = item.status == "UNDER_REVIEW"
                    )

                    TimelineNode(
                        title = if (item.status == "REJECTED") "تم رفض الطلب" else "اعتماد الطلب ونشره",
                        subtitle = when (item.status) {
                            "APPROVED" -> "تمت الموافقة وتطبيق التعديلات في الدليل العام بنجاح 🎉"
                            "REJECTED" -> item.moderatorNote ?: "لم يتم اعتماد المساهمة نظراً لعدم تطابق البيانات أو التكرار."
                            else -> "سيظهر النشاط مباشرة في التطبيق فور الموافقة النهائية."
                        },
                        isCompleted = item.status in listOf("APPROVED", "REJECTED"),
                        isCurrent = item.status in listOf("APPROVED", "REJECTED"),
                        isError = item.status == "REJECTED"
                    )
                }
            }

            // Submitted Data Payload
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "البيانات المقدمة 📄",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MetGhamrNavy
                    )

                    if (item.fieldName != null) {
                        DetailRow(label = "الحقل المستهدف", value = item.fieldName)
                        if (item.oldValue != null) {
                            DetailRow(label = "القيمة السابقة", value = item.oldValue, valueColor = MetGhamrRed)
                        }
                        if (item.newValue != null) {
                            DetailRow(label = "القيمة المقترحة", value = item.newValue, valueColor = MetGhamrGreen)
                        }
                    }

                    if (item.userReason != null) {
                        DetailRow(label = "سبب/ملاحظات المساهم", value = item.userReason)
                    }

                    if (item.payloadJson != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تفاصيل الطلب:\n${item.payloadJson.replace("[{}\"]".toRegex(), "")}",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Reviewer Notes Notice
            if (item.moderatorNote != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.status == "REJECTED") MetGhamrRed.copy(alpha = 0.1f) else MetGhamrGreen.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ملاحظات فريق الدليل 💬",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (item.status == "REJECTED") MetGhamrRed else MetGhamrGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.moderatorNote,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Cancel Pending Contribution Button
            if (item.status == "PENDING") {
                OutlinedButton(
                    onClick = { showCancelConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MetGhamrRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cancel_contribution_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إلغاء هذا الطلب", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("إلغاء المساهمة", fontWeight = FontWeight.Bold, color = MetGhamrRed) },
            text = { Text("هل أنت متأكد من إلغاء هذا الطلب وسحبه من المراجعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        viewModel.cancelContribution(item.id) {
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed)
                ) {
                    Text("نعم، إلغاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("تراجع")
                }
            }
        )
    }
}

@Composable
fun TimelineNode(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isError: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isError -> MetGhamrRed
                isCompleted -> MetGhamrGreen
                isCurrent -> MetGhamrGold
                else -> TextSecondary.copy(alpha = 0.3f)
            },
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isError -> Icons.Default.Close
                        isCompleted -> Icons.Default.Check
                        else -> Icons.Default.Schedule
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isError) MetGhamrRed else if (isCompleted || isCurrent) MetGhamrNavy else TextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
