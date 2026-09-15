package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AddBusinessPayload
import com.example.data.model.BusinessEntity
import com.example.data.model.CategoryItem
import com.example.data.model.SubcategoryItem
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
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val categories by viewModel.categories.collectAsState()
    val possibleDuplicates by viewModel.possibleDuplicates.collectAsState()

    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var selectedSubcategory by remember { mutableStateOf<SubcategoryItem?>(null) }
    var specialization by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var secondaryPhone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("مدينة ميت غمر") }
    var address by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("مركز ميت غمر") }

    // Google Maps & Location
    var googleMapsUrl by remember { mutableStateOf("") }
    var customLat by remember { mutableStateOf<Double?>(null) }
    var customLng by remember { mutableStateOf<Double?>(null) }

    // Business Photos (up to 5)
    var imageUrls by remember { mutableStateOf(listOf<String>()) }
    var newImageUrlInput by remember { mutableStateOf("") }
    var imageUrlError by remember { mutableStateOf<String?>(null) }

    // Working Days & Shift Hours
    val allDays = remember {
        listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    }
    var selectedDays by remember {
        mutableStateOf(setOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس"))
    }
    // shiftMode: 0 = Single Shift, 1 = Two Shifts (Morning & Evening), 2 = Open 24h
    var shiftMode by remember { mutableIntStateOf(1) }
    var morningStart by remember { mutableStateOf("09:00 ص") }
    var morningEnd by remember { mutableStateOf("02:00 م") }
    var eveningStart by remember { mutableStateOf("05:00 م") }
    var eveningEnd by remember { mutableStateOf("11:00 م") }
    var singleShiftStart by remember { mutableStateOf("09:00 ص") }
    var singleShiftEnd by remember { mutableStateOf("10:00 م") }
    var customWorkingHoursNote by remember { mutableStateOf("") }

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
    var subcategoryDropdownExpanded by remember { mutableStateOf(false) }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var showDuplicateWarningDialog by remember { mutableStateOf(false) }
    var submittedContribution by remember { mutableStateOf<UserContributionEntity?>(null) }

    // Landmarks for quick Met Ghamr location picking
    val metGhamrLandmarks = remember {
        listOf(
            "وسط البلد - شارع بورسعيد" to Pair(30.7183, 31.2568),
            "شارع الحرية التجاري" to Pair(30.7195, 31.2590),
            "ميدان المحطة وسكة حديد" to Pair(30.7160, 31.2530),
            "ميدان عرابي ومجمع المحاكم" to Pair(30.7200, 31.2550),
            "شارع الجيش وكورنيش النيل" to Pair(30.7215, 31.2520),
            "صهرجت الكبرى" to Pair(30.7020, 31.2950),
            "قرية بشلا" to Pair(30.7350, 31.2280),
            "قرية دماص" to Pair(30.7500, 31.3100),
            "قرية كوم النور" to Pair(30.6850, 31.2800),
            "قرية تفهنا الأشراف" to Pair(30.7400, 31.2400)
        )
    }

    // Google Maps URL / Coordinates Parser
    fun parseGoogleMapsUrl(input: String) {
        googleMapsUrl = input
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            customLat = null
            customLng = null
            return
        }

        // 1. Raw coordinates "30.7183, 31.2568"
        val coordRegex = Regex("""(-?\d{1,2}\.\d+)[,\s]+(-?\d{1,3}\.\d+)""")
        val matchCoord = coordRegex.find(trimmed)
        if (matchCoord != null) {
            val latVal = matchCoord.groupValues[1].toDoubleOrNull()
            val lngVal = matchCoord.groupValues[2].toDoubleOrNull()
            if (latVal != null && lngVal != null && latVal in -90.0..90.0 && lngVal in -180.0..180.0) {
                customLat = latVal
                customLng = lngVal
                return
            }
        }

        // 2. @lat,lng in google maps url
        val atRegex = Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""")
        val matchAt = atRegex.find(trimmed)
        if (matchAt != null) {
            val latVal = matchAt.groupValues[1].toDoubleOrNull()
            val lngVal = matchAt.groupValues[2].toDoubleOrNull()
            if (latVal != null && lngVal != null) {
                customLat = latVal
                customLng = lngVal
                return
            }
        }

        // 3. q=lat,lng or ll=lat,lng
        val queryRegex = Regex("""[?&](?:q|ll)=(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""")
        val matchQuery = queryRegex.find(trimmed)
        if (matchQuery != null) {
            val latVal = matchQuery.groupValues[1].toDoubleOrNull()
            val lngVal = matchQuery.groupValues[2].toDoubleOrNull()
            if (latVal != null && lngVal != null) {
                customLat = latVal
                customLng = lngVal
                return
            }
        }
    }

    // Sample preset images for categories
    fun getSampleImagesForCategory(catId: String?): List<String> {
        return when (catId) {
            "cat_restaurants", "cat_food" -> listOf(
                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80"
            )
            "cat_doctors", "cat_health" -> listOf(
                "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=800&q=80"
            )
            "cat_pharmacies" -> listOf(
                "https://images.unsplash.com/photo-1586015555751-63c23e85e2ef?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1576602976047-174e57a47881?auto=format&fit=crop&w=800&q=80"
            )
            else -> listOf(
                "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80"
            )
        }
    }

    // Working Hours string builder
    fun computeWorkingHoursString(): String {
        val daysSummary = when {
            selectedDays.size == 7 -> "طوال أيام الأسبوع"
            selectedDays == setOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس") -> "السبت إلى الخميس (الجمعة عطلة)"
            selectedDays == setOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس") -> "الأحد إلى الخميس (الجمعة والسبت عطلة)"
            selectedDays.isEmpty() -> "حسب الحجز المسبق"
            else -> selectedDays.joinToString("، ")
        }
        val shiftsSummary = when (shiftMode) {
            2 -> "مفتوح 24 ساعة"
            1 -> "الفترة الصباحية: $morningStart - $morningEnd | الفترة المسائية: $eveningStart - $eveningEnd"
            else -> "من $singleShiftStart حتى $singleShiftEnd"
        }
        return if (customWorkingHoursNote.isNotBlank()) {
            "$daysSummary | $shiftsSummary (${customWorkingHoursNote.trim()})"
        } else {
            "$daysSummary | $shiftsSummary"
        }
    }

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
        val computedHours = computeWorkingHoursString()
        val payload = AddBusinessPayload(
            name = name.trim(),
            categoryId = selectedCategory!!.id,
            categoryName = selectedCategory!!.nameAr,
            subcategoryId = if (selectedCategory!!.id == "cat_companies") "" else (selectedSubcategory?.id ?: ""),
            specialization = if (selectedCategory!!.id == "cat_companies" || selectedCategory!!.subcategories.isEmpty()) "" else specialization.trim().ifEmpty { selectedSubcategory?.nameAr ?: "" },
            phone = phone.trim(),
            secondaryPhone = secondaryPhone.trim(),
            whatsapp = whatsapp.trim(),
            city = selectedCity,
            address = address.trim(),
            district = district.trim(),
            workingHours = computedHours,
            workingDays = selectedDays.joinToString("، "),
            morningShift = if (shiftMode == 1) "$morningStart - $morningEnd" else "",
            eveningShift = if (shiftMode == 1) "$eveningStart - $eveningEnd" else "",
            isTwoShifts = (shiftMode == 1),
            description = description.trim(),
            facebookUrl = facebookUrl.trim(),
            websiteUrl = websiteUrl.trim(),
            googleMapsUrl = googleMapsUrl.trim(),
            lat = customLat,
            lng = customLng,
            imageUrls = imageUrls.filter { it.isNotBlank() }.take(5),
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
                    containerColor = SurfaceCard,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
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
                // --- 🌟 Attractive Official Website Registration Invitation Card ---
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        SkyBlueDark,
                                        SkyBluePrimary
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Language,
                                            contentDescription = null,
                                            tint = CalmGold,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الموقع الرسمي لمدينة ميت غمر 🌐",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "سجل بيانات نشاطك لتوثيقها والوصول لأكبر عدد من العملاء",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            Text(
                                text = "يمكنك الآن تسجيل وإضافة نشاطك التجاري مباشرة عبر البوابة الرسمية لدليل ميت غمر لضمان سرعة النشر والتوثيق والظهور في نتائج البحث.",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    val url = "http://www.mit3mr.com/p/dalil-mitghamr.html"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = SkyBlueDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_register_official_website"),
                                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = SkyBlueDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تسجيل بيانات النشاط على الموقع الرسمي الآن",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SkyBlueDark
                                    )
                                }
                            }
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
                        val inputColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedLabelColor = MetGhamrNavy,
                            unfocusedLabelColor = TextSecondary,
                            focusedPlaceholderColor = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF94A3B8),
                            focusedBorderColor = MetGhamrNavy,
                            unfocusedBorderColor = BorderLight,
                            cursorColor = MetGhamrNavy
                        )
                        val inputTextStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )

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
                            textStyle = inputTextStyle,
                            colors = inputColors,
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
                                textStyle = inputTextStyle,
                                colors = inputColors,
                                label = { Text("التصنيف الرئيسي *") },
                                placeholder = { Text("اختر التصنيف الرئيسي") },
                                isError = categoryError != null,
                                supportingText = { if (categoryError != null) Text(categoryError!!, color = MetGhamrRed) },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = MetGhamrNavy) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MetGhamrNavy) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_business_category_picker")
                            )

                            // Overlay clickable box
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { categoryDropdownExpanded = true }
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
                                        text = { Text(cat.nameAr, color = Color.Black, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = MetGhamrNavy) },
                                        onClick = {
                                            selectedCategory = cat
                                            selectedSubcategory = null
                                            specialization = ""
                                            categoryError = null
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Subcategory / Specialization (تصنيف الفرعي / التخصص)
                        if (selectedCategory != null) {
                            val subcategories = selectedCategory!!.subcategories
                            if (subcategories.isEmpty() || selectedCategory!!.id == "cat_companies") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MetGhamrNavy.copy(alpha = 0.04f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MetGhamrTeal)
                                        Text(
                                            text = "تصنيف «${selectedCategory!!.nameAr}» يضم الأنشطة والشركات مباشرة دون أقسام فرعية.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MetGhamrNavy.copy(alpha = 0.04f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "التصنيف الفرعي المعتمد *",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MetGhamrNavy
                                            )
                                            Surface(
                                                color = MetGhamrTeal.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${subcategories.size} قسماً معتمداً",
                                                    fontSize = 11.sp,
                                                    color = MetGhamrTeal,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Dropdown selector for subcategory
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = selectedSubcategory?.nameAr ?: specialization.ifBlank { "اختر القسم الفرعي المعتمد" },
                                                onValueChange = {},
                                                readOnly = true,
                                                textStyle = inputTextStyle,
                                                colors = inputColors,
                                                label = { Text("قائمة الأقسام الفرعية المعتمدة") },
                                                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = MetGhamrGoldDark) },
                                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MetGhamrNavy) },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .clickable { subcategoryDropdownExpanded = true }
                                            )

                                            DropdownMenu(
                                                expanded = subcategoryDropdownExpanded,
                                                onDismissRequest = { subcategoryDropdownExpanded = false },
                                                modifier = Modifier
                                                    .fillMaxWidth(0.9f)
                                                    .background(Color.White)
                                            ) {
                                                subcategories.forEach { sub ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                text = sub.nameAr,
                                                                fontWeight = if (selectedSubcategory?.id == sub.id) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (selectedSubcategory?.id == sub.id) MetGhamrNavy else Color.Black
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = if (selectedSubcategory?.id == sub.id) MetGhamrTeal else Color.Transparent
                                                            )
                                                        },
                                                        onClick = {
                                                            selectedSubcategory = sub
                                                            specialization = sub.nameAr
                                                            subcategoryDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Quick Chips for 1-tap selection
                                        Text("أو اختر القسم الفرعي بنقرة واحدة:", fontSize = 11.sp, color = TextMuted)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(subcategories) { sub ->
                                                val isSelected = selectedSubcategory?.id == sub.id || specialization == sub.nameAr
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedSubcategory = sub
                                                        specialization = sub.nameAr
                                                    },
                                                    label = { Text(sub.nameAr, fontSize = 11.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MetGhamrNavy,
                                                        selectedLabelColor = Color.White
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                            textStyle = inputTextStyle,
                            colors = inputColors,
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
                            textStyle = inputTextStyle,
                            colors = inputColors,
                            label = { Text("رقم هاتف إضافي (اختياري)") },
                            placeholder = { Text("012xxxxxxx أو رقم أرضي") },
                            leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 6. WhatsApp Number (رقم واتس آب بعد رقم اضافي)
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            textStyle = inputTextStyle,
                            colors = inputColors,
                            label = { Text("رقم واتس آب للتواصل والطلبات (WhatsApp) (اختياري)") },
                            placeholder = { Text("مثال: 01012345678 أو 201012345678") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = "WhatsApp",
                                    tint = Color(0xFF25D366)
                                )
                            },
                            trailingIcon = {
                                if (whatsapp.isNotBlank()) {
                                    IconButton(onClick = { whatsapp = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextMuted)
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // WhatsApp quick copy chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (phone.isNotBlank()) {
                                SuggestionChip(
                                    onClick = { whatsapp = phone },
                                    label = { Text("📱 نفس الهاتف الأساسي", fontSize = 11.sp) }
                                )
                            }
                            if (secondaryPhone.isNotBlank()) {
                                SuggestionChip(
                                    onClick = { whatsapp = secondaryPhone },
                                    label = { Text("📞 نفس الرقم الإضافي", fontSize = 11.sp) }
                                )
                            }
                        }

                        // 7. City / Village Selector (اختيار المدينة أو القرية التابعة)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCity,
                                onValueChange = {},
                                readOnly = true,
                                textStyle = inputTextStyle,
                                colors = inputColors,
                                label = { Text("المدينة / القرية التابعة *") },
                                leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = MetGhamrNavy) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MetGhamrNavy) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_business_city_picker")
                            )

                            // Overlay clickable box
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { cityDropdownExpanded = true }
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
                                        text = { Text(loc, color = Color.Black, fontWeight = if (loc == selectedCity) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MetGhamrTeal) },
                                        onClick = {
                                            selectedCity = loc
                                            cityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 8. Address
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                if (addressError != null) addressError = null
                            },
                            textStyle = inputTextStyle,
                            colors = inputColors,
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

                        // 9. District
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            textStyle = inputTextStyle,
                            colors = inputColors,
                            label = { Text("الحي / المنطقة") },
                            placeholder = { Text("مثال: وسط البلد، كفر المقدام، شارع المحطة") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 10. Google Maps Location (الموقع على خريطة Google maps)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PinDrop, contentDescription = null, tint = Color(0xFFEA4335))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "الموقع على خريطة Google Maps 📍",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MetGhamrNavy
                                    )
                                }

                                OutlinedTextField(
                                    value = googleMapsUrl,
                                    onValueChange = { parseGoogleMapsUrl(it) },
                                    textStyle = inputTextStyle,
                                    colors = inputColors,
                                    label = { Text("رابط خرائط جوجل أو الإحداثيات") },
                                    placeholder = { Text("الصق الرابط من خرائط Google أو الإحداثيات (30.7183, 31.2568)") },
                                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFFEA4335)) },
                                    trailingIcon = {
                                        if (googleMapsUrl.isNotBlank()) {
                                            IconButton(onClick = {
                                                googleMapsUrl = ""
                                                customLat = null
                                                customLng = null
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = TextMuted)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Detected Coordinates Card & Map Open Action
                                if (customLat != null && customLng != null) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("تم تحديد الموقع والإحداثيات", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                                }
                                                Text(
                                                    "خط العرض: ${"%.4f".format(customLat)} | خط الطول: ${"%.4f".format(customLng)}",
                                                    fontSize = 11.sp,
                                                    color = TextDark
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = {
                                                    val mapUri = Uri.parse("geo:$customLat,$customLng?q=$customLat,$customLng(${Uri.encode(name.ifBlank { "موقع النشاط" })})")
                                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                                    try {
                                                        context.startActivity(mapIntent)
                                                    } catch (e: Exception) {
                                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$customLat,$customLng"))
                                                        context.startActivity(webIntent)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("معاينة الخريطة", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                // Quick Landmark Coordinates
                                Text("تحديد موقع تقريبي سريع في ميت غمر:", fontSize = 11.sp, color = TextMuted)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(metGhamrLandmarks) { landmark ->
                                        SuggestionChip(
                                            onClick = {
                                                customLat = landmark.second.first
                                                customLng = landmark.second.second
                                                googleMapsUrl = "https://maps.google.com/?q=${landmark.second.first},${landmark.second.second}"
                                            },
                                            label = { Text(landmark.first, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // 11. Facebook Page
                        OutlinedTextField(
                            value = facebookUrl,
                            onValueChange = { facebookUrl = it },
                            textStyle = inputTextStyle,
                            colors = inputColors,
                            label = { Text("رابط صفحة الفيسبوك (اختياري)") },
                            placeholder = { Text("facebook.com/yourpage") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF1877F2)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 12. Website URL
                        OutlinedTextField(
                            value = websiteUrl,
                            onValueChange = { websiteUrl = it },
                            textStyle = inputTextStyle,
                            colors = inputColors,
                            label = { Text("الموقع الإلكتروني الرسمي (اختياري)") },
                            placeholder = { Text("www.example.com") },
                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MetGhamrNavy) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Divider(color = BorderLight, thickness = 1.dp)

                        // 13. Business Photos Section (صور النشاط بحد أقصى 5 صور)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MetGhamrNavy)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "صور النشاط التجاري 📸",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )
                                    }
                                    Surface(
                                        color = if (imageUrls.size >= 5) Color(0xFFFFEBEE) else MetGhamrNavy.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${imageUrls.size} من 5 صور",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (imageUrls.size >= 5) Color(0xFFC62828) else MetGhamrNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "يمكنك وضع روابط صور مباشرة من الإنترنت (بحد أقصى 5 صور) لعرض واجهة النشاط أو المنتجات في معرض الصور.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                // Add Image Input (if less than 5)
                                if (imageUrls.size < 5) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newImageUrlInput,
                                            onValueChange = {
                                                newImageUrlInput = it
                                                imageUrlError = null
                                            },
                                            textStyle = inputTextStyle,
                                            colors = inputColors,
                                            label = { Text("رابط الصورة ${imageUrls.size + 1}") },
                                            placeholder = { Text("https://example.com/photo.jpg") },
                                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = TextMuted) },
                                            isError = imageUrlError != null,
                                            supportingText = { if (imageUrlError != null) Text(imageUrlError!!, color = MetGhamrRed) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Button(
                                            onClick = {
                                                val cleanUrl = newImageUrlInput.trim()
                                                if (cleanUrl.isBlank()) {
                                                    imageUrlError = "أدخل رابط صورة صحيح"
                                                } else if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                                                    imageUrlError = "الرابط يجب أن يبدأ بـ https://"
                                                } else {
                                                    imageUrls = imageUrls + cleanUrl
                                                    newImageUrlInput = ""
                                                    imageUrlError = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إضافة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Quick sample button
                                    val sampleImages = getSampleImagesForCategory(selectedCategory?.id)
                                    val availableSample = sampleImages.firstOrNull { it !in imageUrls }
                                    if (availableSample != null) {
                                        TextButton(
                                            onClick = {
                                                if (imageUrls.size < 5) {
                                                    imageUrls = imageUrls + availableSample
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MetGhamrGoldDark, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إضافة صورة نموذجية للنشاط للتجربة 🖼️", fontSize = 11.sp, color = MetGhamrNavy)
                                        }
                                    }
                                }

                                // List of added images with thumbnail
                                if (imageUrls.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        imageUrls.forEachIndexed { index, url ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = "صورة ${index + 1}",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.LightGray)
                                                    )

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Surface(
                                                            color = if (index == 0) MetGhamrGold.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.4f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = if (index == 0) "صورة الغلاف الرئيسية ⭐" else "صورة رقم ${index + 1}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (index == 0) MetGhamrGoldDark else TextDark,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = url,
                                                            fontSize = 11.sp,
                                                            color = TextMuted,
                                                            maxLines = 1
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            imageUrls = imageUrls.filterIndexed { i, _ -> i != index }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFC62828))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = BorderLight, thickness = 1.dp)

                        // 14. Working Hours Section (مواعيد العمل: فترتين صباحية ومسائية، وتحديد أيام الأسبوع)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MetGhamrNavy)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مواعيد وأوقات العمل ⏱️",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MetGhamrNavy
                                    )
                                }

                                // 1. Working Days in Week
                                Text(
                                    text = "1. تحديد أيام العمل في الأسبوع:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MetGhamrNavy
                                )

                                // Presets for days
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val isAllWeek = selectedDays.size == 7
                                    val isSatToThu = selectedDays == setOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")
                                    val isSunToThu = selectedDays == setOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس")

                                    FilterChip(
                                        selected = isAllWeek,
                                        onClick = { selectedDays = allDays.toSet() },
                                        label = { Text("طوال الأسبوع", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isSatToThu,
                                        onClick = { selectedDays = setOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس") },
                                        label = { Text("السبت - الخميس", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = isSunToThu,
                                        onClick = { selectedDays = setOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس") },
                                        label = { Text("الأحد - الخميس", fontSize = 11.sp) }
                                    )
                                }

                                // Individual Day Chips
                                Text("أو حدد أيام معينة من الأسبوع بالضغط عليها:", fontSize = 11.sp, color = TextMuted)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(allDays) { day ->
                                        val isChecked = selectedDays.contains(day)
                                        FilterChip(
                                            selected = isChecked,
                                            onClick = {
                                                selectedDays = if (isChecked) {
                                                    selectedDays - day
                                                } else {
                                                    selectedDays + day
                                                }
                                            },
                                            label = { Text(day, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MetGhamrNavy,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Divider(color = BorderLight)

                                // 2. Shift Mode
                                Text(
                                    text = "2. نظام فترات العمل ومواعيدها:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MetGhamrNavy
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = shiftMode == 1,
                                        onClick = { shiftMode = 1 },
                                        label = { Text("☀️🌙 فترتان (صباحية ومسائية)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MetGhamrNavy,
                                            selectedLabelColor = Color.White
                                        )
                                    )

                                    FilterChip(
                                        selected = shiftMode == 0,
                                        onClick = { shiftMode = 0 },
                                        label = { Text("☀️ فترة واحدة", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MetGhamrNavy,
                                            selectedLabelColor = Color.White
                                        )
                                    )

                                    FilterChip(
                                        selected = shiftMode == 2,
                                        onClick = { shiftMode = 2 },
                                        label = { Text("⚡ 24 ساعة", fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MetGhamrNavy,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }

                                // 3. Shift Times Details
                                if (shiftMode == 1) {
                                    // Morning Shift
                                    Surface(
                                        color = Color(0xFFFFF8E1),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("الفترة الصباحية ☀️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = morningStart,
                                                    onValueChange = { morningStart = it },
                                                    label = { Text("من الساعة") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = inputTextStyle,
                                                    colors = inputColors
                                                )
                                                OutlinedTextField(
                                                    value = morningEnd,
                                                    onValueChange = { morningEnd = it },
                                                    label = { Text("حتى الساعة") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = inputTextStyle,
                                                    colors = inputColors
                                                )
                                            }

                                            // Quick presets for morning
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                SuggestionChip(
                                                    onClick = { morningStart = "09:00 ص"; morningEnd = "02:00 م" },
                                                    label = { Text("9 ص - 2 م", fontSize = 10.sp) }
                                                )
                                                SuggestionChip(
                                                    onClick = { morningStart = "08:30 ص"; morningEnd = "01:30 م" },
                                                    label = { Text("8:30 ص - 1:30 م", fontSize = 10.sp) }
                                                )
                                                SuggestionChip(
                                                    onClick = { morningStart = "10:00 ص"; morningEnd = "03:00 م" },
                                                    label = { Text("10 ص - 3 م", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }

                                    // Evening Shift
                                    Surface(
                                        color = Color(0xFFEDE7F6),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.NightlightRound, contentDescription = null, tint = Color(0xFF512DA8), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("الفترة المسائية 🌙", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF311B92))
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = eveningStart,
                                                    onValueChange = { eveningStart = it },
                                                    label = { Text("من الساعة") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = inputTextStyle,
                                                    colors = inputColors
                                                )
                                                OutlinedTextField(
                                                    value = eveningEnd,
                                                    onValueChange = { eveningEnd = it },
                                                    label = { Text("حتى الساعة") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = inputTextStyle,
                                                    colors = inputColors
                                                )
                                            }

                                            // Quick presets for evening
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                SuggestionChip(
                                                    onClick = { eveningStart = "05:00 م"; eveningEnd = "11:00 م" },
                                                    label = { Text("5 م - 11 م", fontSize = 10.sp) }
                                                )
                                                SuggestionChip(
                                                    onClick = { eveningStart = "06:00 م"; eveningEnd = "12:00 ص" },
                                                    label = { Text("6 م - 12 ص", fontSize = 10.sp) }
                                                )
                                                SuggestionChip(
                                                    onClick = { eveningStart = "04:30 م"; eveningEnd = "10:30 م" },
                                                    label = { Text("4:30 م - 10:30 م", fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }
                                } else if (shiftMode == 0) {
                                    // Single Shift
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = singleShiftStart,
                                            onValueChange = { singleShiftStart = it },
                                            label = { Text("من الساعة") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = inputTextStyle,
                                            colors = inputColors
                                        )
                                        OutlinedTextField(
                                            value = singleShiftEnd,
                                            onValueChange = { singleShiftEnd = it },
                                            label = { Text("حتى الساعة") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            textStyle = inputTextStyle,
                                            colors = inputColors
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SuggestionChip(
                                            onClick = { singleShiftStart = "09:00 ص"; singleShiftEnd = "10:00 م" },
                                            label = { Text("9 ص - 10 م", fontSize = 10.sp) }
                                        )
                                        SuggestionChip(
                                            onClick = { singleShiftStart = "10:00 ص"; singleShiftEnd = "11:00 م" },
                                            label = { Text("10 ص - 11 م", fontSize = 10.sp) }
                                        )
                                        SuggestionChip(
                                            onClick = { singleShiftStart = "08:00 ص"; singleShiftEnd = "08:00 م" },
                                            label = { Text("8 ص - 8 م", fontSize = 10.sp) }
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("النشاط مفتوح ومتاح لخدمة العملاء طوال الـ 24 ساعة يومياً", fontSize = 12.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                // Generated Working Hours Preview
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("نص مواعيد العمل التلقائي للعملاء:", fontSize = 11.sp, color = TextMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = computeWorkingHoursString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MetGhamrNavy
                                        )
                                    }
                                }

                                // Custom note on hours
                                OutlinedTextField(
                                    value = customWorkingHoursNote,
                                    onValueChange = { customWorkingHoursNote = it },
                                    textStyle = inputTextStyle,
                                    colors = inputColors,
                                    label = { Text("ملاحظة إضافية عن المواعيد (اختياري)") },
                                    placeholder = { Text("مثال: استراحة صلاة الجمعة، أو طوارئ 24 ساعة...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 15. Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 500) description = it },
                            textStyle = inputTextStyle,
                            colors = inputColors,
                            label = { Text("وصف مختصر عن الخدمات والأصناف") },
                            placeholder = { Text("اكتب نبذة عن الخدمات المقدمة، العروض، أو أية معلومات مفيدة...") },
                            supportingText = { Text("${description.length}/500 حرف") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 16. User Notes
                        OutlinedTextField(
                            value = userNotes,
                            onValueChange = { userNotes = it },
                            textStyle = inputTextStyle,
                            colors = inputColors,
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
