package com.example.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.engine.MetGhamrGeoHierarchy
import com.example.data.model.BusinessEntity
import com.example.data.model.seed.InitialDataSeed
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.util.ExcelFileImportHelper
import com.example.util.FileExportUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartExcelTemplateDialog(
    viewModel: DirectoryViewModel,
    onDismiss: () -> Unit,
    onInjectCsvRow: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val categories = InitialDataSeed.categories

    // File picker launcher for Excel/CSV upload directly inside template dialog
    val excelDialogPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val parsedResult = ExcelFileImportHelper.parseUploadedFile(context, uri)
                if (parsedResult.errorMessage != null) {
                    viewModel.showToast("خطأ أثناء قراءة الملف: ${parsedResult.errorMessage}")
                } else if (parsedResult.totalRows == 0) {
                    viewModel.showToast("الملف لا يحتوي على بيانات صالحة")
                } else {
                    if (onInjectCsvRow != null) {
                        onInjectCsvRow(parsedResult.csvFormattedText)
                    }
                    viewModel.showToast("تم استخراج ${parsedResult.totalRows} سجل من (${parsedResult.fileName}) ودمجها بالمعالج ✅")
                    onDismiss()
                }
            } catch (e: Exception) {
                viewModel.showToast("فشلت معالجة الملف: ${e.localizedMessage}")
            }
        }
    }

    var selectedTopTab by remember { mutableIntStateOf(0) } // 0: Live Dropdown Entry, 1: Download Templates, 2: Categories Guide

    // Live Dropdown Form State
    var businessName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull { it.id == "cat_doctors" } ?: categories.first()) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    var selectedSpecialty by remember { mutableStateOf(selectedCategory.subcategories.firstOrNull()?.nameAr ?: "عام") }
    var showSpecialtyMenu by remember { mutableStateOf(false) }

    var phonePrimary by remember { mutableStateOf("") }
    var phoneSecondary by remember { mutableStateOf("") }
    var whatsappNumber by remember { mutableStateOf("") }

    val popularAreas = listOf(
        "شارع بورسعيد", "شارع البحر (الكورنيش)", "شارع الحرية", "شارع 26 يوليو",
        "شارع أحمد عرابي", "ميدان المحطة", "وسط البلد", "حي المعلمين", "حي الأشراف",
        "صهرجت الكبرى", "تفهنا الأشراف", "سنفا", "بشلا", "دنديط", "كوم النور",
        "هلا", "أتميدة", "سنتماي", "ميت ناجي", "ميت أبو خالد", "كفر سرنجا", "المعصرة"
    )
    var selectedArea by remember { mutableStateOf(popularAreas.first()) }
    var showAreaMenu by remember { mutableStateOf(false) }

    var detailedAddress by remember { mutableStateOf("") }
    var workingDays by remember { mutableStateOf("السبت إلى الخميس") }
    var workingHours by remember { mutableStateOf("09:00 ص - 10:00 م") }
    var descriptionText by remember { mutableStateOf("") }

    var isSavingDirectly by remember { mutableStateOf(false) }

    // Synchronize subcategory selection when category changes
    LaunchedEffect(selectedCategory) {
        selectedSpecialty = selectedCategory.subcategories.firstOrNull()?.nameAr ?: "${selectedCategory.nameAr} عام"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MetGhamrTeal.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MetGhamrTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "قالب إكسل الذكي ونظام التصنيف الموحد 📊",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "قوائم منسدلة قياسية لمنع تكرار الأنشطة والأخطاء الإملائية",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }
                }

                // Sub-tabs
                TabRow(
                    selectedTabIndex = selectedTopTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = MetGhamrNavy
                ) {
                    Tab(
                        selected = selectedTopTab == 0,
                        onClick = { selectedTopTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("إدخال بالقوائم المنسدلة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTopTab == 1,
                        onClick = { selectedTopTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("تنزيل ملفات Excel / CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTopTab == 2,
                        onClick = { selectedTopTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("دليل التصنيفات المعتمد (${categories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Content Body
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTopTab) {
                        0 -> {
                            // --- TAB 0: Interactive Form with Cascading Dropdowns ---
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "اختر التصنيف الرئيسي أولاً وسيقوم النظام بتحديث خيارات التخصص الفرعي تلقائياً بدون أخطاء إملائية.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E3A8A)
                                            )
                                        }
                                    }
                                }

                                // 1. Business Name
                                item {
                                    OutlinedTextField(
                                        value = businessName,
                                        onValueChange = { businessName = it },
                                        label = { Text("اسم النشاط أو المنشأة * (مثال: عيادة د. محمد، نادي ميت غمر، مدرسة اللغات...)") },
                                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = MetGhamrNavy) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                // 2. Main Category (Dropdown 1)
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("1. التصنيف الرئيسي * (قائمة منسدلة معتمدة)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedCard(
                                                onClick = { showCategoryMenu = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Category, contentDescription = null, tint = MetGhamrTeal)
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(selectedCategory.nameAr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                                            Text("التصنيف القياسي بالدليل", fontSize = 10.sp, color = TextMuted)
                                                        }
                                                    }
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MetGhamrNavy)
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showCategoryMenu,
                                                onDismissRequest = { showCategoryMenu = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                categories.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(cat.nameAr, fontWeight = if (cat.id == selectedCategory.id) FontWeight.Bold else FontWeight.Normal)
                                                                if (cat.id == selectedCategory.id) {
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedCategory = cat
                                                            showCategoryMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Subcategory / Specialty (Dropdown 2 - Cascading from Category)
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("2. التخصص / القسم الفرعي * (ينبثق تلقائياً من التصنيف المختار)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedCard(
                                                onClick = { showSpecialtyMenu = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Stars, contentDescription = null, tint = MetGhamrGold)
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(selectedSpecialty, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                                            Text("تخصص تابع لـ ${selectedCategory.nameAr}", fontSize = 10.sp, color = TextMuted)
                                                        }
                                                    }
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MetGhamrNavy)
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showSpecialtyMenu,
                                                onDismissRequest = { showSpecialtyMenu = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                if (selectedCategory.subcategories.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("${selectedCategory.nameAr} عام") },
                                                        onClick = {
                                                            selectedSpecialty = "${selectedCategory.nameAr} عام"
                                                            showSpecialtyMenu = false
                                                        }
                                                    )
                                                } else {
                                                    selectedCategory.subcategories.forEach { sub ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(sub.nameAr, fontWeight = if (sub.nameAr == selectedSpecialty) FontWeight.Bold else FontWeight.Normal)
                                                                    if (sub.nameAr == selectedSpecialty) {
                                                                        Spacer(modifier = Modifier.width(8.dp))
                                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(16.dp))
                                                                    }
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedSpecialty = sub.nameAr
                                                                showSpecialtyMenu = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Fast Chips for Subcategories
                                        if (selectedCategory.subcategories.isNotEmpty()) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                items(selectedCategory.subcategories) { sub ->
                                                    FilterChip(
                                                        selected = selectedSpecialty == sub.nameAr,
                                                        onClick = { selectedSpecialty = sub.nameAr },
                                                        label = { Text(sub.nameAr, fontSize = 11.sp) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Phone & WhatsApp
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = phonePrimary,
                                            onValueChange = { phonePrimary = it },
                                            label = { Text("الهاتف الأساسي *") },
                                            placeholder = { Text("010xxxxxxxx") },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MetGhamrNavy) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = whatsappNumber,
                                            onValueChange = { whatsappNumber = it },
                                            label = { Text("واتساب") },
                                            placeholder = { Text("2010xxxxxxxx") },
                                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = MetGhamrTeal) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }

                                // 5. Area / Village Dropdown
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("3. المنطقة أو القرية المعتمدة * (قائمة منسدلة)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedCard(
                                                onClick = { showAreaMenu = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444))
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(selectedArea, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                                            Text("نطاق مركز ومدينة ميت غمر", fontSize = 10.sp, color = TextMuted)
                                                        }
                                                    }
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MetGhamrNavy)
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showAreaMenu,
                                                onDismissRequest = { showAreaMenu = false },
                                                modifier = Modifier.fillMaxWidth(0.85f)
                                            ) {
                                                popularAreas.forEach { area ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(area, fontWeight = if (area == selectedArea) FontWeight.Bold else FontWeight.Normal)
                                                                if (area == selectedArea) {
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(16.dp))
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedArea = area
                                                            showAreaMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 6. Detailed Address & Description
                                item {
                                    OutlinedTextField(
                                        value = detailedAddress,
                                        onValueChange = { detailedAddress = it },
                                        label = { Text("العنوان بالتفصيل (اسم الشارع، العمارة، بجوار علامة مميزة)") },
                                        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = MetGhamrNavy) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = descriptionText,
                                        onValueChange = { descriptionText = it },
                                        label = { Text("وصف الخدمات والعروض") },
                                        placeholder = { Text("مثال: كشف وتشخيص، خدمة 24 ساعة، توصيل مجاني، صالات مكيفة...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3
                                    )
                                }

                                // Action Buttons for Live Entry
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 1. Direct Save to Directory
                                        Button(
                                            onClick = {
                                                if (businessName.isBlank()) {
                                                    viewModel.showToast("يرجى كتابة اسم النشاط أولاً")
                                                    return@Button
                                                }
                                                if (phonePrimary.isBlank()) {
                                                    viewModel.showToast("يرجى إدخال رقم الهاتف الأساسي")
                                                    return@Button
                                                }

                                                isSavingDirectly = true
                                                val newBusiness = BusinessEntity(
                                                    id = "biz_" + UUID.randomUUID().toString().take(8),
                                                    name = businessName.trim(),
                                                    categoryId = selectedCategory.id,
                                                    categoryName = selectedCategory.nameAr,
                                                    specialty = selectedSpecialty.trim(),
                                                    description = descriptionText.trim().ifEmpty { "نشاط مصنف قياسياً بدليل ميت غمر" },
                                                    phone = phonePrimary.trim(),
                                                    phoneSecondary = phoneSecondary.trim().ifEmpty { null },
                                                    whatsapp = whatsappNumber.trim().ifEmpty { null },
                                                    city = if (selectedArea.contains("شارع") || selectedArea.contains("حي") || selectedArea.contains("ميدان") || selectedArea == "وسط البلد") "مدينة ميت غمر" else "مركز ميت غمر",
                                                    area = selectedArea,
                                                    address = detailedAddress.trim().ifEmpty { selectedArea },
                                                    workingHours = "$workingDays: $workingHours",
                                                    isVerified = true,
                                                    isActive = true,
                                                    updatedAt = System.currentTimeMillis()
                                                )

                                                viewModel.addBusinessDirect(newBusiness) {
                                                    isSavingDirectly = false
                                                    viewModel.showToast("تمت إضافة (${newBusiness.name}) بنجاح إلى الدليل والتصنيف القياسي! ✨")
                                                    businessName = ""
                                                    phonePrimary = ""
                                                    detailedAddress = ""
                                                    descriptionText = ""
                                                }
                                            },
                                            enabled = !isSavingDirectly && businessName.isNotBlank() && phonePrimary.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            if (isSavingDirectly) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("حفظ فوري بالدليل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // 2. Generate CSV row
                                        OutlinedButton(
                                            onClick = {
                                                if (businessName.isBlank()) {
                                                    viewModel.showToast("يرجى كتابة اسم النشاط أولاً")
                                                    return@OutlinedButton
                                                }
                                                val rowLine = "${businessName.trim()},${selectedCategory.nameAr},${selectedSpecialty.trim()},${phonePrimary.trim()},${phoneSecondary.trim()},${whatsappNumber.trim()},${detailedAddress.trim().ifEmpty { selectedArea }},${selectedArea},مدينة ميت غمر,${workingDays},${workingHours},${descriptionText.trim()},,,,\n"
                                                
                                                clipboardManager.setText(AnnotatedString(rowLine))
                                                onInjectCsvRow?.invoke(rowLine)
                                                viewModel.showToast("تم نسخ سطر البيانات وجاهز للصق في ملف Excel أو مساحة الاستيراد 📋")
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("نسخ سطر Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // --- TAB 1: Download Templates (Excel .xls & CSV) ---
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0D9488).copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF0D9488).copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.TableView, contentDescription = null, tint = Color(0xFF0D9488))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("1. قالب Excel الشامل متعدد الصفحات (.xls)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                                    Text("يفتح مباشرة في Microsoft Excel و Google Sheets", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }

                                            Text(
                                                text = "يحتوي الملف على 4 شيتات متكاملة:\n" +
                                                        "• صفحة 1: نموذج إدخال الأنشطة مع عينات جاهزة للأطباء والنوادي والمدارس والمصالح الحكومية.\n" +
                                                        "• صفحة 2: دليل التصنيفات والتخصصات المعتمدة (مرجع القوائم المنسدلة).\n" +
                                                        "• صفحة 3: المناطق والقرى المعتمدة بميت غمر.\n" +
                                                        "• صفحة 4: قواعد الإدخال القياسية لمنع تكرار الهمزات والأخطاء.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF334155),
                                                lineHeight = 18.sp
                                            )

                                            Button(
                                                onClick = {
                                                    val excelXml = viewModel.getImportTemplateExcel()
                                                    FileExportUtils.downloadAndShareFile(
                                                        context = context,
                                                        fileName = "قالب_استيراد_أنشطة_ميت_غمر_الشامل.xls",
                                                        content = excelXml,
                                                        mimeType = "application/vnd.ms-excel",
                                                        title = "فتح أو حفظ قالب إكسل الشامل (Excel Workbook)"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("تنزيل قالب Excel الشامل (.xls) ✨", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MetGhamrNavy.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Article, contentDescription = null, tint = MetGhamrNavy)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("2. قالب CSV القياسي (UTF-8)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MetGhamrNavy)
                                                    Text("مناسب للاستيراد السريع والنسخ المباشر", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    val csv = viewModel.getImportTemplateCsv(com.example.data.model.BulkImportType.BUSINESSES)
                                                    FileExportUtils.downloadAndShareFile(
                                                        context = context,
                                                        fileName = "قالب_أنشطة_ميت_غمر.csv",
                                                        content = csv,
                                                        mimeType = "text/csv",
                                                        title = "فتح أو حفظ قالب CSV"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("تنزيل ملف CSV القياسي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF107C41).copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF107C41))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text("3. رفع ملف Excel معبأ من جهازك 📤", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF166534))
                                                    Text("استيراد فوري للملفات بصيغة .xlsx أو .xls أو .csv", fontSize = 11.sp, color = TextMuted)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    excelDialogPickerLauncher.launch("*/*")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("اختيار ورفع ملف EXCEL من الهاتف 🚀", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // --- TAB 2: Categories & Specialties Catalog ---
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("تصدير دليل التصنيفات إلى ملف Excel (.xls)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                                            }
                                            Text(
                                                "قم بتنزيل ملف إكسل منسق وجاهز يحتوي على كافة التصنيفات الـ 21 والتخصصات والكلمات الدلالية.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E3A8A)
                                            )
                                            Button(
                                                onClick = {
                                                    val excelContent = viewModel.getCategoriesCatalogExcel()
                                                    FileExportUtils.downloadAndShareFile(
                                                        context = context,
                                                        fileName = "فهرس_التصنيفات_والتخصصات_ميت_غمر.xls",
                                                        content = excelContent,
                                                        mimeType = "application/vnd.ms-excel",
                                                        title = "فتح أو حفظ فهرس التصنيفات (Excel)"
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("تنزيل ملف Excel للتصنيفات والتخصصات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                item {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "دليل التصنيفات والتخصصات القياسية المعتمدة في النظام لمنع تكرار الهمزات والأخطاء:",
                                            fontSize = 11.sp,
                                            color = MetGhamrNavy,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                items(categories) { cat ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = MetGhamrTeal, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(cat.nameAr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("(${cat.subcategories.size} تخصص)", fontSize = 10.sp, color = TextMuted)
                                            }

                                            if (cat.subcategories.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    items(cat.subcategories) { sub ->
                                                        Surface(
                                                            color = Color(0xFFF8FAFC),
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                        ) {
                                                            Text(
                                                                text = sub.nameAr,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF334155),
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        }
    }
}
