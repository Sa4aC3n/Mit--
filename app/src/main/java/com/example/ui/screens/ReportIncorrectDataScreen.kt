package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.ContributionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncorrectDataScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val selectedBusiness by viewModel.selectedBusiness.collectAsState()
    val scrollState = rememberScrollState()

    if (selectedBusiness == null) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val biz = selectedBusiness!!

    val reportTypes = listOf(
        ContributionType.REPORT_INCORRECT_DATA to "بيانات غير صحيحة بصفة عامة",
        ContributionType.REPORT_CLOSED to "النشاط مغلق تماماً أو تم إنهاؤه",
        ContributionType.REPORT_DUPLICATE to "هذا النشاط مكرر في الدليل",
        ContributionType.OTHER to "أخرى"
    )

    var selectedTypePair by remember { mutableStateOf(reportTypes.first()) }
    var reason by remember { mutableStateOf("") }
    var reasonError by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        if (reason.trim().length < 5) {
            reasonError = "يرجى كتابة تفاصيل البلاغ بوضوح (5 أحرف على الأقل)"
            return false
        }
        reasonError = null
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإبلاغ عن خطأ في النشاط",
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
            // Target Business Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MetGhamrRed.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text("النشاط المحدد للبلاغ:", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text(biz.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("📍 ${biz.address}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }

            if (isSubmitted) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MetGhamrGreen,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "تم استلام البلاغ بنجاح",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MetGhamrNavy
                        )
                        Text(
                            text = "شكراً لاهتمامك. سيقوم فريق الإدارة بفحص بلاغك والتحقق الميداني قبل اتخاذ الإجراء المناسب.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("العودة للتفاصيل")
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "اختر نوع البلاغ ⚠️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )

                        // Radio options
                        reportTypes.forEach { pair ->
                            val selected = selectedTypePair.first == pair.first
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MetGhamrRed.copy(alpha = 0.1f) else SurfaceLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTypePair = pair }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { selectedTypePair = pair },
                                        colors = RadioButtonDefaults.colors(selectedColor = MetGhamrRed)
                                    )
                                    Text(
                                        text = pair.second,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MetGhamrRed else TextPrimary
                                    )
                                }
                            }
                        }

                        // Reason text input
                        OutlinedTextField(
                            value = reason,
                            onValueChange = {
                                reason = it
                                if (reasonError != null) reasonError = null
                            },
                            label = { Text("تفاصيل البلاغ والأسباب *") },
                            placeholder = { Text("مثال: الرقم غير مستعمل / المكان مغلق منذ شهرين...") },
                            isError = reasonError != null,
                            supportingText = { if (reasonError != null) Text(reasonError!!, color = MetGhamrRed) },
                            leadingIcon = { Icon(Icons.Default.Feedback, contentDescription = null, tint = MetGhamrRed) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = MetGhamrNavy,
                                unfocusedBorderColor = BorderLight,
                                focusedLabelColor = MetGhamrNavy,
                                unfocusedLabelColor = TextSecondary,
                                focusedPlaceholderColor = TextMuted,
                                unfocusedPlaceholderColor = TextMuted,
                                cursorColor = MetGhamrNavy
                            ),
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_reason_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (validate()) {
                                    viewModel.submitReportData(
                                        businessId = biz.id,
                                        businessName = biz.name,
                                        reportType = selectedTypePair.first,
                                        reason = reason
                                    ) {
                                        isSubmitted = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_report_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال البلاغ للإدارة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
