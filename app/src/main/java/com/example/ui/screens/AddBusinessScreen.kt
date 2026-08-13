package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AddBusinessPayload
import com.example.data.model.BusinessEntity
import com.example.data.model.CategoryItem
import com.example.data.model.UserContributionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBusinessScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onContributionSubmitted: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val categories = viewModel.categories
    val possibleDuplicates by viewModel.possibleDuplicates.collectAsState()

    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var specialization by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var secondaryPhone by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("مدينة ميت غمر") }
    var address by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("مركز ميت غمر") }
    var workingHours by remember { mutableStateOf("من 9:00 صباحاً حتى 10:00 مساءً") }
    var description by remember { mutableStateOf("") }
    var facebookUrl by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var userNotes by remember { mutableStateOf("") }

    // Errors State
    var nameError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var showDuplicateWarningDialog by remember { mutableStateOf(false) }
    var submittedContribution by remember { mutableStateOf<UserContributionEntity?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (name.trim().length < 2) {
            nameError = "اسم النشاط يجب أن يتكون من حرفين على الأقل"
            isValid = false
        } else {
            nameError = null
        }

        if (selectedCategory == null) {
            categoryError = "يرجى اختيار التصنيف الرئيسي"
            isValid = false
        } else {
            categoryError = null
        }

        val cleanPhone = phone.trim()
        if (cleanPhone.length < 8) {
            phoneError = "أدخل رقم هاتف صحيح (مثال: 01012345678 أو أرضي 0501234567)"
            isValid = false
        } else {
            phoneError = null
        }

        if (address.trim().length < 5) {
            addressError = "يرجى كتابة عنوان واضح بمدينة ميت غمر"
            isValid = false
        } else {
            addressError = null
        }

        return isValid
    }

    fun submitPayload() {
        val payload = AddBusinessPayload(
            name = name.trim(),
            categoryId = selectedCategory!!.id,
            categoryName = selectedCategory!!.nameAr,
            specialization = specialization.trim(),
            phone = phone.trim(),
            secondaryPhone = secondaryPhone.trim(),
            city = selectedCity,
            address = address.trim(),
            district = district.trim(),
            workingHours = workingHours.trim(),
            description = description.trim(),
            facebookUrl = facebookUrl.trim(),
            websiteUrl = websiteUrl.trim(),
            userNotes = userNotes.trim()
        )

        viewModel.submitAddBusiness(payload) { contrib ->
            submittedContribution = contrib
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إضافة نشاط تجاري جديد",
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
        if (submittedContribution != null) {
            // Success Screen State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SurfaceLight)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MetGhamrGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MetGhamrGreen,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }

                        Text(
                            text = "تم استلام الطلب بنجاح 🎉",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "كود الطلب الخاص بك:\n${submittedContribution!!.humanReadableId}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MetGhamrGold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "سيقوم فريق مراجعة دليل ميت غمر بمراجعة بيانات النشاط (${submittedContribution!!.businessName}) والتحقق منها قبل اعتماد ظهورها للجميع.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.selectContribution(submittedContribution!!.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("متابعة حالة الطلب ⏱️")
                        }

                        OutlinedButton(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("العودة للرئيسية", color = MetGhamrNavy)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SurfaceLight)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.AddBusiness,
                            contentDescription = null,
                            tint = MetGhamrGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "ساهم في دعم أنشطة ميت غمر 🏛️",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أضف النشاط التجاري الجديد ليجده الجميع بكل سهولة. تحظى جميع الإضافات بمراجعة دقيقة.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Main Form Card
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
                            text = "البيانات الأساسية 📝",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )

                        // 1. Business Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (nameError != null) nameError = null
                            },
                            label = { Text("اسم النشاط التجاري *") },
                            placeholder = { Text("مثال: مطعم الأكابر، صيدلية السلام") },
                            isError = nameError != null,
                            supportingText = { if (nameError != null) Text(nameError!!, color = MetGhamrRed) },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = MetGhamrNavy) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_business_name_input")
                        )

                        // 2. Category Picker
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCategory?.nameAr ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("التصنيف الرئيسي *") },
                                placeholder = { Text("اختر التصنيف") },
                                isError = categoryError != null,
                                supportingText = { if (categoryError != null) Text(categoryError!!, color = MetGhamrRed) },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = MetGhamrNavy) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = true }
                                    .testTag("add_business_category_picker")
                            )

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Color.White)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.nameAr, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MetGhamrNavy) },
                                        onClick = {
                                            selectedCategory = cat
                                            categoryError = null
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Dynamic Specialization
                        if (selectedCategory != null) {
                            OutlinedTextField(
                                value = specialization,
                                onValueChange = { specialization = it },
                                label = { Text("التخصص الدقيق / نوع المطبخ (اختياري)") },
                                placeholder = {
                                    Text(
                                        when (selectedCategory!!.id) {
                                            "cat_food" -> "مثال: شرقي، مشويات، بيتزا"
                                            "cat_doctors" -> "مثال: باطنة، أطفال، أسنان"
                                            "cat_crafts" -> "مثال: سباكة، كهرباء، نجارة"
                                            else -> "مثال: جملة وقطاعي، صيانة فورية"
                                        }
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MetGhamrNavy) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_business_specialization_input")
                            )
                        }

                        Divider(color = BorderLight, thickness = 1.dp)

                        Text(
                            text = "بيانات الاتصال والعنوان 📞",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )

                        // 4. Primary Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                if (phoneError != null) phoneError = null
                            },
                            label = { Text("رقم الهاتف الرئيسي *") },
                            placeholder = { Text("010xxxxxxx أو 050xxxxxxx") },
                            isError = phoneError != null,
                            supportingText = { if (phoneError != null) Text(phoneError!!, color = MetGhamrRed) },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MetGhamrNavy) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_business_phone_input")
                        )

                        // 5. Secondary Phone Number
                        OutlinedTextField(
                            value = secondaryPhone,
                            onValueChange = { secondaryPhone = it },
                            label = { Text("رقم هاتف إضافي / واتساب (اختياري)") },
                            placeholder = { Text("012xxxxxxx") },
                            leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 6. City / Village Selector (اختيار المدينة أو القرية التابعة)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCity,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("المدينة / القرية التابعة *") },
                                leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = MetGhamrNavy) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { cityDropdownExpanded = true }
                                    .testTag("add_business_city_picker")
                            )

                            DropdownMenu(
                                expanded = cityDropdownExpanded,
                                onDismissRequest = { cityDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Color.White)
                            ) {
                                com.example.data.model.MetGhamrLocations.CITIES_AND_VILLAGES.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc, fontWeight = if (loc == selectedCity) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MetGhamrTeal) },
                                        onClick = {
                                            selectedCity = loc
                                            cityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 7. Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                if (addressError != null) addressError = null
                            },
                            label = { Text("العنوان بالتفصيل *") },
                            placeholder = { Text("مثال: شارع الحرية بجوار مسجد النصر، ميت غمر") },
                            isError = addressError != null,
                            supportingText = { if (addressError != null) Text(addressError!!, color = MetGhamrRed) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MetGhamrNavy) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_business_address_input")
                        )

                        // 8. District
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("الحي / المنطقة") },
                            placeholder = { Text("مثال: وسط البلد، كفر المقدام، شارع المحطة") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 9. Facebook Page
                        OutlinedTextField(
                            value = facebookUrl,
                            onValueChange = { facebookUrl = it },
                            label = { Text("رابط صفحة الفيسبوك (اختياري)") },
                            placeholder = { Text("facebook.com/yourpage") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF1877F2)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 10. Website URL
                        OutlinedTextField(
                            value = websiteUrl,
                            onValueChange = { websiteUrl = it },
                            label = { Text("الموقع الإلكتروني الرسمي (اختياري)") },
                            placeholder = { Text("www.example.com") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MetGhamrNavy) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = BorderLight, thickness = 1.dp)

                        Text(
                            text = "مواعيد العمل والوصف ⏱️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MetGhamrNavy
                        )

                        // 8. Working Hours
                        OutlinedTextField(
                            value = workingHours,
                            onValueChange = { workingHours = it },
                            label = { Text("مواعيد العمل") },
                            placeholder = { Text("يومياً من 9 صباحاً حتى 11 مساءً") },
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 9. Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 500) description = it },
                            label = { Text("وصف مختصر عن الخدمات والأصناف") },
                            placeholder = { Text("اكتب نبذة عن الخدمات المقدمة، العروض، أو أية معلومات مفيدة...") },
                            supportingText = { Text("${description.length}/500 حرف") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 10. User Notes
                        OutlinedTextField(
                            value = userNotes,
                            onValueChange = { userNotes = it },
                            label = { Text("ملاحظات خاصة للمراجعين (اختياري)") },
                            placeholder = { Text("مثال: أنا صاحب هذا النشاط / تم تغيير الاسم مؤخراً") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                if (validate()) {
                                    // Trigger Duplicate check before final submission
                                    viewModel.checkPossibleDuplicates(name, phone)
                                    showDuplicateWarningDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_add_business_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إرسال النشاط للمراجعة",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Duplicate Detection Dialog Step
    if (showDuplicateWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MetGhamrGold)
                    Text("فحص الأنشطة المشابهة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (possibleDuplicates.isNotEmpty()) {
                        Text(
                            text = "قد يكون هذا النشاط مسجلاً بالفعل لدينا. نرجو مراجعة النتائج المشابهة أدناه لتفادي التكرار:",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )

                        possibleDuplicates.forEach { dup ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(dup.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)
                                    Text("📍 ${dup.address}", fontSize = 11.sp, color = TextSecondary)
                                    Text("📞 ${dup.phone}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "لم نجد أنشطة مشابهة تطابق الاسم أو رقم الهاتف بنفس الحروف. هل تود تأكيد إرسال الطلب الآن؟",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateWarningDialog = false
                        submitPayload()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrGreen)
                ) {
                    Text(if (possibleDuplicates.isNotEmpty()) "إرسال على أي حال" else "تأكيد الإرسال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateWarningDialog = false }) {
                    Text("إلغاء ومراجعة البيانات")
                }
            }
        )
    }
}
