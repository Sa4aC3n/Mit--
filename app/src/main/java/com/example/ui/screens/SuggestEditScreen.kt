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
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestEditScreen(
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

    val editableFields = listOf(
        "المدينة / القرية التابعة" to biz.city,
        "العنوان التفصيلي" to biz.address,
        "رقم الهاتف" to biz.phone,
        "صفحة الفيسبوك" to (biz.facebookUrl ?: ""),
        "الموقع الإلكتروني" to (biz.websiteUrl ?: ""),
        "مواعيد العمل" to biz.workingHours,
        "التخصص / نوع الخدمات" to biz.specialty,
        "الوصف والشرح" to biz.description,
        "رقم هاتف إضافي / واتساب" to (biz.phoneSecondary ?: ""),
        "بيانات أخرى" to ""
    )

    var selectedFieldPair by remember { mutableStateOf(editableFields.first()) }
    var fieldDropdownExpanded by remember { mutableStateOf(false) }

    var newValue by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    var newValueError by remember { mutableStateOf<String?>(null) }
    var reasonError by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var valid = true
        if (newValue.trim().length < 2) {
            newValueError = "يرجى كتابة التعديل أو القيمة الجديدة بوضوح"
            valid = false
        } else {
            newValueError = null
        }

        if (reason.trim().length < 5) {
            reasonError = "يرجى كتابة سبب التعديل لمساعدة فريق المراجعة"
            valid = false
        } else {
            reasonError = null
        }

        return valid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اقتراح تعديل بيانات نشاط",
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
            // Business Header Summary
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "النشاط المستهدف:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = biz.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "📍 ${biz.address}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
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
                            text = "تم إرسال اقتراح التعديل بنجاح",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MetGhamrNavy
                        )
                        Text(
                            text = "شكراً لمساهمتك في إبقاء بيانات دليل ميت غمر محدثة ودقيقة. سيتم فحص التعديل وتطبيقه بعد المراجعة.",
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
                            Text("العودة لتفاصيل النشاط")
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
                            text = "حدد البيان المراد تصحيحه ✏️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )

                        // Field Picker Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedFieldPair.first,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("نوع البيان") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MetGhamrNavy) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fieldDropdownExpanded = true }
                                    .testTag("suggest_edit_field_picker")
                            )

                            DropdownMenu(
                                expanded = fieldDropdownExpanded,
                                onDismissRequest = { fieldDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Color.White)
                            ) {
                                editableFields.forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.first, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            selectedFieldPair = pair
                                            fieldDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Current Value Card (oldValue display)
                        if (selectedFieldPair.second.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceLight,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("القيمة الحالية بالدليل:", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = selectedFieldPair.second,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MetGhamrRed
                                    )
                                }
                            }
                        }

                        // New Value Input
                        OutlinedTextField(
                            value = newValue,
                            onValueChange = {
                                newValue = it
                                if (newValueError != null) newValueError = null
                            },
                            label = { Text("القيمة الصحيحة المقترحة *") },
                            placeholder = { Text("اكتب القيمة الصحيحة هنا...") },
                            isError = newValueError != null,
                            supportingText = { if (newValueError != null) Text(newValueError!!, color = MetGhamrRed) },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = MetGhamrGreen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggest_edit_new_value_input")
                        )

                        // Reason Input
                        OutlinedTextField(
                            value = reason,
                            onValueChange = {
                                reason = it
                                if (reasonError != null) reasonError = null
                            },
                            label = { Text("سبب التعديل / ملاحظات إضافية *") },
                            placeholder = { Text("مثال: الرقم السابق لا يعمل / انتقل المحل للمكان الجديد...") },
                            isError = reasonError != null,
                            supportingText = { if (reasonError != null) Text(reasonError!!, color = MetGhamrRed) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary) },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggest_edit_reason_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (validate()) {
                                    viewModel.submitSuggestEdit(
                                        businessId = biz.id,
                                        businessName = biz.name,
                                        fieldName = selectedFieldPair.first,
                                        oldValue = selectedFieldPair.second,
                                        newValue = newValue,
                                        reason = reason
                                    ) {
                                        isSubmitted = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_suggest_edit_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال الاقتراح للمراجعة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
