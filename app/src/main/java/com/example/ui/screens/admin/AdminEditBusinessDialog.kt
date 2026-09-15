package com.example.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.BusinessEntity
import com.example.data.model.CategoryItem
import com.example.data.model.seed.InitialDataSeed
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditBusinessDialog(
    business: BusinessEntity,
    viewModel: DirectoryViewModel,
    onDismiss: () -> Unit
) {
    // Form fields initialized with current business values
    var name by remember { mutableStateOf(business.name) }
    var categoryId by remember { mutableStateOf(business.categoryId) }
    var categoryName by remember { mutableStateOf(business.categoryName) }
    var specialty by remember { mutableStateOf(business.specialty) }
    var description by remember { mutableStateOf(business.description) }
    
    var phone by remember { mutableStateOf(business.phone) }
    var phoneSecondary by remember { mutableStateOf(business.phoneSecondary ?: "") }
    var whatsapp by remember { mutableStateOf(business.whatsapp ?: "") }
    
    var city by remember { mutableStateOf(business.city) }
    var area by remember { mutableStateOf(business.area) }
    var address by remember { mutableStateOf(business.address) }
    var latitude by remember { mutableDoubleStateOf(business.latitude) }
    var longitude by remember { mutableDoubleStateOf(business.longitude) }
    var mapsUrlInput by remember { mutableStateOf("") }
    
    var imageUrl by remember { mutableStateOf(business.imageUrl ?: "") }
    var workingHours by remember { mutableStateOf(business.workingHours) }
    var facebookUrl by remember { mutableStateOf(business.facebookUrl ?: "") }
    var websiteUrl by remember { mutableStateOf(business.websiteUrl ?: "") }
    
    var isVerified by remember { mutableStateOf(business.isVerified) }
    var isActive by remember { mutableStateOf(business.isActive) }
    var isOpenNow by remember { mutableStateOf(business.isOpenNow) }
    
    var isSaving by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUrl = uri.toString()
            viewModel.showToast("تم اختيار صورة جديدة بنجاح 📸")
        }
    }

    val categories = InitialDataSeed.categories

    // Predefined villages and major areas in Met Ghamr for 1-tap selection
    val popularLocations = listOf(
        "مدينة ميت غمر" to "شارع الحرية",
        "مدينة ميت غمر" to "شارع البحر",
        "مدينة ميت غمر" to "شارع 26 يوليو",
        "مدينة ميت غمر" to "ميدان المحطة",
        "مدينة ميت غمر" to "شارع بورسعيد",
        "مدينة ميت غمر" to "شارع أحمد عرابي",
        "صهرجت الكبرى" to "الشارع الرئيسي",
        "بشلا" to "طريق ميت غمر - المنصورة",
        "ميت ناجي" to "وسط البلد",
        "دنديط" to "المدخل الرئيسي",
        "كوم النور" to "شارع السوق",
        "تفهنا الأشراف" to "بجوار جامعة الأزهر",
        "أتميدة" to "الشارع التجاري",
        "سنتماي" to "وسط القرية",
        "هلا" to "الشارع الرئيسي",
        "ميت الفرماوي" to "طريق الزقازيق"
    )

    // Working hours presets
    val workingHoursPresets = listOf(
        "يومياً: 9:00 ص - 11:00 م",
        "على مدار 24 ساعة (طوارئ)",
        "السبت - الخميس: 10:00 ص - 10:00 م (الجمعة مغلق)",
        "يومياً: 1:00 م - 10:00 م",
        "السبت - الأربعاء: 9:00 ص - 5:00 م",
        "الفترة الصباحية والمسائية (10ص - 2ظ / 6م - 11م)"
    )

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تعديل بيانات المنشأة",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "صلاحيات مدير النظام الأعلى (Super Admin) 👑",
                                fontSize = 11.sp,
                                color = Color(0xFFFFD700)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.testTag("admin_edit_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    viewModel.showToast("يرجى كتابة اسم المنشأة")
                                    return@Button
                                }
                                if (phone.isBlank()) {
                                    viewModel.showToast("يرجى إدخال رقم الهاتف الأساسي")
                                    return@Button
                                }
                                isSaving = true
                                val updatedEntity = business.copy(
                                    name = name.trim(),
                                    categoryId = categoryId,
                                    categoryName = categoryName,
                                    specialty = specialty.trim(),
                                    description = description.trim(),
                                    phone = phone.trim(),
                                    phoneSecondary = phoneSecondary.trim().ifEmpty { null },
                                    whatsapp = whatsapp.trim().ifEmpty { null },
                                    city = city.trim().ifEmpty { "مدينة ميت غمر" },
                                    area = area.trim().ifEmpty { "وسط البلد" },
                                    address = address.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    imageUrl = imageUrl.trim().ifEmpty { null },
                                    workingHours = workingHours.trim(),
                                    facebookUrl = facebookUrl.trim().ifEmpty { null },
                                    websiteUrl = websiteUrl.trim().ifEmpty { null },
                                    isVerified = isVerified,
                                    isActive = isActive,
                                    isOpenNow = isOpenNow,
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.updateBusinessDirect(updatedEntity) {
                                    isSaving = false
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving && name.isNotBlank() && phone.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("admin_save_business_top_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF0F172A),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "حفظ التعديلات",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A)
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إلغاء", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    viewModel.showToast("يرجى كتابة اسم المنشأة")
                                    return@Button
                                }
                                if (phone.isBlank()) {
                                    viewModel.showToast("يرجى إدخال رقم الهاتف الأساسي")
                                    return@Button
                                }
                                isSaving = true
                                val updatedEntity = business.copy(
                                    name = name.trim(),
                                    categoryId = categoryId,
                                    categoryName = categoryName,
                                    specialty = specialty.trim(),
                                    description = description.trim(),
                                    phone = phone.trim(),
                                    phoneSecondary = phoneSecondary.trim().ifEmpty { null },
                                    whatsapp = whatsapp.trim().ifEmpty { null },
                                    city = city.trim().ifEmpty { "مدينة ميت غمر" },
                                    area = area.trim().ifEmpty { "وسط البلد" },
                                    address = address.trim(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    imageUrl = imageUrl.trim().ifEmpty { null },
                                    workingHours = workingHours.trim(),
                                    facebookUrl = facebookUrl.trim().ifEmpty { null },
                                    websiteUrl = websiteUrl.trim().ifEmpty { null },
                                    isVerified = isVerified,
                                    isActive = isActive,
                                    isOpenNow = isOpenNow,
                                    updatedAt = System.currentTimeMillis()
                                )
                                viewModel.updateBusinessDirect(updatedEntity) {
                                    isSaving = false
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving && name.isNotBlank() && phone.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(2f)
                                .testTag("admin_save_business_bottom_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الحفظ...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("حفظ وتحديث النشاط فوراً ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8FAFC))
            ) {
                // Section Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = MetGhamrNavy,
                    edgePadding = 12.dp,
                    divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("البيانات الأساسية", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("الاتصال والتواصل", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("العنوان و Google Maps", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("الصور والمظهر", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("المواعيد والروابط", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        text = { Text("الحالة والتوثيق", fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Super Admin Notice Banner
                    item {
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تعديل مباشر بصلاحيات Super Admin",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "التعديلات تنعكس فورياً على شاشة النشاط وجميع المستخدمين والدليل المحلي",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> {
                            // --- TAB 0: Basic Info ---
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            "البيانات الأساسية للمنشأة 🏢",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )

                                        // Business Name
                                        OutlinedTextField(
                                            value = name,
                                            onValueChange = { name = it },
                                            label = { Text("اسم المنشأة أو النشاط *") },
                                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = MetGhamrNavy) },
                                            trailingIcon = {
                                                if (name.isNotEmpty()) {
                                                    IconButton(onClick = { name = "" }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_edit_name_input"),
                                            singleLine = true
                                        )

                                        // Category Picker
                                        Text("التصنيف الرئيسي *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedCard(
                                                onClick = { showCategoryDropdown = true },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Category, contentDescription = null, tint = MetGhamrTeal)
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(categoryName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                                            Text("معرف: $categoryId", fontSize = 10.sp, color = TextMuted)
                                                        }
                                                    }
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showCategoryDropdown,
                                                onDismissRequest = { showCategoryDropdown = false },
                                                modifier = Modifier.fillMaxWidth(0.9f)
                                            ) {
                                                categories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(cat.nameAr, fontWeight = if (cat.id == categoryId) FontWeight.Bold else FontWeight.Normal)
                                                                if (cat.id == categoryId) {
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            categoryId = cat.id
                                                            categoryName = cat.nameAr
                                                            showCategoryDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Specialty / Subcategory
                                        OutlinedTextField(
                                            value = specialty,
                                            onValueChange = { specialty = it },
                                            label = { Text("التخصص / الخدمات الرئيسية *") },
                                            placeholder = { Text("مثال: عظام ومفاصل، مشويات، وجبات سريعة...") },
                                            leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = MetGhamrGold) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Quick Subcategory Suggestions based on chosen category
                                        val currentCatItem = categories.find { it.id == categoryId }
                                        if (currentCatItem != null && currentCatItem.subcategories.isNotEmpty()) {
                                            Text("اقتراحات سريعة للتخصص:", fontSize = 11.sp, color = TextMuted)
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(currentCatItem.subcategories) { sub ->
                                                    SuggestionChip(
                                                        onClick = { specialty = sub.nameAr },
                                                        label = { Text(sub.nameAr, fontSize = 11.sp) },
                                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                                            containerColor = if (specialty == sub.nameAr) MetGhamrNavy.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Description
                                        OutlinedTextField(
                                            value = description,
                                            onValueChange = { description = it },
                                            label = { Text("نبذة ووصف تفصيلي عن النشاط") },
                                            placeholder = { Text("أدخل تفاصيل الخدمات، سنوات الخبرة، الميزات...") },
                                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MetGhamrNavy) },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 3,
                                            maxLines = 6
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // --- TAB 1: Contact & Communication ---
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            "أرقام الاتصال والتواصل 📞",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )

                                        // Primary Phone
                                        OutlinedTextField(
                                            value = phone,
                                            onValueChange = { phone = it },
                                            label = { Text("رقم الهاتف الأساسي *") },
                                            placeholder = { Text("مثال: 01012345678 أو 0506900000") },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF2E7D32)) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_edit_phone_input"),
                                            singleLine = true
                                        )

                                        // Secondary Phone
                                        OutlinedTextField(
                                            value = phoneSecondary,
                                            onValueChange = { phoneSecondary = it },
                                            label = { Text("رقم هاتف إضافي (اختياري)") },
                                            placeholder = { Text("مثال: 01234567890") },
                                            leadingIcon = { Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = MetGhamrNavy) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // WhatsApp Number
                                        OutlinedTextField(
                                            value = whatsapp,
                                            onValueChange = { whatsapp = it },
                                            label = { Text("رقم الواتساب (اختياري)") },
                                            placeholder = { Text("مثال: 01012345678") },
                                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366)) },
                                            trailingIcon = {
                                                if (phone.isNotEmpty() && whatsapp.isEmpty()) {
                                                    TextButton(onClick = { whatsapp = phone }) {
                                                        Text("مثل الأساسي", fontSize = 11.sp, color = MetGhamrNavy, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            // --- TAB 2: Address & Google Maps Location ---
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            "العنوان والموقع الجغرافي وخريطة Google Maps 📍",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )

                                        // City or Village
                                        OutlinedTextField(
                                            value = city,
                                            onValueChange = { city = it },
                                            label = { Text("المدينة / القرية التابعة *") },
                                            leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = MetGhamrNavy) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Area / Neighborhood
                                        OutlinedTextField(
                                            value = area,
                                            onValueChange = { area = it },
                                            label = { Text("المنطقة / الشارع / الحي *") },
                                            placeholder = { Text("مثال: شارع الحرية، شارع البحر، وسط البلد...") },
                                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MetGhamrTeal) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Quick Village / Area Selectors
                                        Text("اختيار سريع لأشهر مناطق وقرى ميت غمر:", fontSize = 11.sp, color = TextMuted)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(popularLocations) { (c, a) ->
                                                SuggestionChip(
                                                    onClick = {
                                                        city = c
                                                        area = a
                                                    },
                                                    label = { Text("$c • $a", fontSize = 11.sp) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = if (city == c && area == a) MetGhamrNavy.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
                                                    )
                                                )
                                            }
                                        }

                                        // Full Address
                                        OutlinedTextField(
                                            value = address,
                                            onValueChange = { address = it },
                                            label = { Text("العنوان التفصيلي والعلامات المميزة *") },
                                            placeholder = { Text("مثال: بجوار مسجد النور، أمام المحطة، عمارة الأمل الدور الثاني...") },
                                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = MetGhamrNavy) },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 2,
                                            maxLines = 3
                                        )

                                        HorizontalDivider(color = Color(0xFFE2E8F0))

                                        // GPS Coordinates & Google Maps Link helper
                                        Text(
                                            "إحداثيات الموقع على الخريطة (GPS & Google Maps):",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MetGhamrNavy
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = latitude.toString(),
                                                onValueChange = { latitude = it.toDoubleOrNull() ?: latitude },
                                                label = { Text("خط العرض (Lat)") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = longitude.toString(),
                                                onValueChange = { longitude = it.toDoubleOrNull() ?: longitude },
                                                label = { Text("خط الطول (Lng)") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }

                                        // Helper to parse Google Maps URL
                                        OutlinedTextField(
                                            value = mapsUrlInput,
                                            onValueChange = {
                                                mapsUrlInput = it
                                                // Extract lat/lng if pasted
                                                if (it.contains("@") && it.contains(",")) {
                                                    try {
                                                        val atPart = it.substringAfter("@").substringBefore("/")
                                                        val coords = atPart.split(",")
                                                        if (coords.size >= 2) {
                                                            val lat = coords[0].toDoubleOrNull()
                                                            val lng = coords[1].toDoubleOrNull()
                                                            if (lat != null && lng != null) {
                                                                latitude = lat
                                                                longitude = lng
                                                                viewModel.showToast("تم استخراج الإحداثيات من رابط الخريطة بنجاح 📍")
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                }
                                            },
                                            label = { Text("لصق رابط موقع Google Maps لاستخراج الإحداثيات") },
                                            placeholder = { Text("الصق الرابط هنا مثل https://maps.google.com/...") },
                                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFFEA4335)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Reset to Default Met Ghamr Center
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    latitude = 30.7183
                                                    longitude = 31.2568
                                                    viewModel.showToast("تم تعيين إحداثيات مركز ميت غمر (30.7183, 31.2568)")
                                                }
                                            ) {
                                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تعيين موقع مركز ميت غمر", fontSize = 11.sp, color = MetGhamrNavy)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // --- TAB 3: Images & Visual Media ---
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            "صور النشاط والمظهر المرئي 📸",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )

                                        // Current Image Preview
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (imageUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = imageUrl,
                                                    contentDescription = "معاينة صورة النشاط",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                // Clear image badge
                                                IconButton(
                                                    onClick = { imageUrl = "" },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "حذف الصورة", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                            } else {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text("لا توجد صورة محددة (سيتم استخدام صورة التصنيف الافتراضية)", fontSize = 12.sp, color = TextMuted)
                                                }
                                            }
                                        }

                                        // Upload Buttons Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("admin_pick_photo_button")
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("رفع صورة من المعرض 🖼️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Manual Image URL Input
                                        OutlinedTextField(
                                            value = imageUrl,
                                            onValueChange = { imageUrl = it },
                                            label = { Text("أو إدخال رابط الصورة مباشرة (URL)") },
                                            placeholder = { Text("https://example.com/image.jpg") },
                                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = MetGhamrTeal) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Quick Presets from High Quality Unsplash Images for Category
                                        Text("نماذج صور عالية الدقة مناسبة للتصنيف:", fontSize = 11.sp, color = TextMuted)
                                        val sampleImages = when (categoryId) {
                                            "cat_restaurants" -> listOf(
                                                "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800",
                                                "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800",
                                                "https://images.unsplash.com/photo-1544025162-d76694265947?w=800"
                                            )
                                            "cat_doctors", "cat_medical_centers" -> listOf(
                                                "https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=800",
                                                "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=800",
                                                "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?w=800"
                                            )
                                            "cat_pharmacies" -> listOf(
                                                "https://images.unsplash.com/photo-1586015555751-63bb77f4322a?w=800",
                                                "https://images.unsplash.com/photo-1576602976047-174e57a47881?w=800"
                                            )
                                            "cat_supermarkets" -> listOf(
                                                "https://images.unsplash.com/photo-1542838132-92c53300491e?w=800",
                                                "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=800"
                                            )
                                            "cat_cafes" -> listOf(
                                                "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800",
                                                "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800"
                                            )
                                            else -> listOf(
                                                "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=800",
                                                "https://images.unsplash.com/photo-1497366216548-37526070297c?w=800"
                                            )
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(sampleImages) { url ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .border(
                                                            if (imageUrl == url) 2.dp else 1.dp,
                                                            if (imageUrl == url) Color(0xFFD4AF37) else Color(0xFFCBD5E1),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { imageUrl = url }
                                                ) {
                                                    AsyncImage(
                                                        model = url,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (imageUrl == url) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(0xFF0F172A).copy(alpha = 0.5f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFFD700))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        4 -> {
                            // --- TAB 4: Working Hours & Social Links ---
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            "مواعيد العمل وروابط التواصل ⏰",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )

                                        // Working Hours
                                        OutlinedTextField(
                                            value = workingHours,
                                            onValueChange = { workingHours = it },
                                            label = { Text("مواعيد وساعات العمل *") },
                                            placeholder = { Text("مثال: يومياً من 9:00 ص إلى 11:00 م") },
                                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = MetGhamrGold) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Quick Presets
                                        Text("اختيار سريع لصيغة المواعيد:", fontSize = 11.sp, color = TextMuted)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(workingHoursPresets) { preset ->
                                                SuggestionChip(
                                                    onClick = { workingHours = preset },
                                                    label = { Text(preset, fontSize = 11.sp) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = if (workingHours == preset) MetGhamrNavy.copy(alpha = 0.15f) else Color(0xFFF1F5F9)
                                                    )
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFE2E8F0))

                                        // Facebook Page Link
                                        OutlinedTextField(
                                            value = facebookUrl,
                                            onValueChange = { facebookUrl = it },
                                            label = { Text("رابط صفحة الفيسبوك (اختياري)") },
                                            placeholder = { Text("https://facebook.com/page-name") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF1877F2)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        // Website Link
                                        OutlinedTextField(
                                            value = websiteUrl,
                                            onValueChange = { websiteUrl = it },
                                            label = { Text("رابط الموقع الإلكتروني (اختياري)") },
                                            placeholder = { Text("https://mybusiness.com") },
                                            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = MetGhamrNavy) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        5 -> {
                            // --- TAB 5: Status & Super Admin Flags ---
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            "إعدادات وحالة النشاط (صلاحيات إدارية حصرية) 🛡️",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MetGhamrNavy
                                        )

                                        // Active Switch
                                        Surface(
                                            color = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        if (isActive) "النشاط مفعّل ونشط في الدليل 🟢" else "النشاط موقوف ومخفي عن الزوار 🔴",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                    )
                                                    Text(
                                                        if (isActive) "يظهر في نتائج البحث والتصنيفات" else "موقوف إدارياً ولن يظهر للجمهور",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                                Switch(
                                                    checked = isActive,
                                                    onCheckedChange = { isActive = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = Color(0xFF2E7D32)
                                                    )
                                                )
                                            }
                                        }

                                        // Verified Switch
                                        Surface(
                                            color = if (isVerified) Color(0xFFE3F2FD) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            "توثيق النشاط الرسمي بالدليل",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MetGhamrNavy
                                                        )
                                                        if (isVerified) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Icon(Icons.Default.Verified, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    Text(
                                                        "إظهار شارة التوثيق الذهبية/الزرقاء لتعزيز ثقة أهالي ميت غمر",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                                Switch(
                                                    checked = isVerified,
                                                    onCheckedChange = { isVerified = it },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = VerifiedBlue
                                                    )
                                                )
                                            }
                                        }

                                        // Is Open Now Switch
                                        Surface(
                                            color = Color(0xFFF8FAFC),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        if (isOpenNow) "الحالة الآن: مفتوح لاستقبال العملاء 🟢" else "الحالة الآن: مغلق 🔴",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextDark
                                                    )
                                                    Text(
                                                        "تغيير مؤقت لحالة الفتح اللحظية",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                                Switch(
                                                    checked = isOpenNow,
                                                    onCheckedChange = { isOpenNow = it }
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
    }
}
