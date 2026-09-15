package com.example.ui.screens.admin

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CategoryItem
import com.example.data.model.SubcategoryItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import java.util.UUID

@Composable
fun AdminCategoriesScreen(viewModel: DirectoryViewModel) {
    val categories by viewModel.categories.collectAsState()
    val allActiveBusinesses by viewModel.allActiveBusinesses.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }

    // Filter categories based on search query
    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories.sortedBy { it.sortOrder }
        } else {
            val q = searchQuery.trim().lowercase()
            categories.filter { cat ->
                cat.nameAr.lowercase().contains(q) ||
                cat.description.lowercase().contains(q) ||
                cat.subcategories.any { sub ->
                    sub.nameAr.lowercase().contains(q) ||
                    sub.keywords.any { kw -> kw.lowercase().contains(q) }
                }
            }.sortedBy { it.sortOrder }
        }
    }

    val totalSubcategories = remember(categories) {
        categories.sumOf { it.subcategories.size }
    }

    AdminLayout(
        viewModel = viewModel,
        title = "إدارة التصنيفات والدليل",
        currentRoute = ScreenRoute.AdminCategories.route
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("admin_categories_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Stats & Main Actions
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "هيكل دليل ميت غمر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MetGhamrNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${categories.size} تصنيف رئيسي • $totalSubcategories تخصص فرعي",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showResetDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_reset_categories")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MetGhamrNavy)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استعادة", fontSize = 11.sp, color = MetGhamrNavy)
                                }

                                Button(
                                    onClick = { isAddingNewCategory = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_add_category")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة تصنيف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Search box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث في التصنيفات والتخصصات الفرعية...", fontSize = 12.sp, color = TextMuted) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MetGhamrCream.copy(alpha = 0.5f),
                                unfocusedContainerColor = MetGhamrCream.copy(alpha = 0.3f),
                                focusedBorderColor = MetGhamrGold,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("input_search_categories")
                        )
                    }
                }
            }

            // Categories List
            if (filteredCategories.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد تصنيفات مطابقة للبحث", fontWeight = FontWeight.Bold, color = MetGhamrNavy, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(filteredCategories, key = { it.id }) { cat ->
                    val isExpanded = expandedCategoryId == cat.id
                    val registeredCount = remember(allActiveBusinesses, cat.id) {
                        allActiveBusinesses.count { it.categoryId == cat.id }
                    }
                    val catColor = Color(cat.colorHex)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_card_${cat.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Icon, Name, Meta, Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = catColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = getCategoryVectorIcon(cat.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                cat.nameAr,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MetGhamrNavy
                                            )
                                            if (cat.isFeatured) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = MetGhamrGold.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "مميز ⭐",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MetGhamrGoldDark,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "${cat.subcategories.size} تخصص فرعي • $registeredCount منشأة مسجلة",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Edit Button
                                    IconButton(
                                        onClick = { editingCategory = cat },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_edit_category_${cat.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "تعديل التصنيف",
                                            tint = MetGhamrNavy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_delete_category_${cat.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "حذف التصنيف",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Expand / Collapse Subcategories Button
                                    IconButton(
                                        onClick = { expandedCategoryId = if (isExpanded) null else cat.id },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "عرض التفاصيل",
                                            tint = MetGhamrNavy,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            if (cat.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    cat.description,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }

                            // Subcategories Preview or Full View
                            if (cat.subcategories.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "التخصصات الفرعية (${cat.subcategories.size}):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MetGhamrGoldDark
                                    )
                                    Text(
                                        if (isExpanded) "إخفاء التفاصيل" else "عرض الكل (${cat.subcategories.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MetGhamrNavy,
                                        modifier = Modifier
                                            .clickable { expandedCategoryId = if (isExpanded) null else cat.id }
                                            .padding(4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                val displayedSubs = if (isExpanded) cat.subcategories else cat.subcategories.take(5)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    displayedSubs.chunked(3).forEach { rowSubs ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowSubs.forEach { sub ->
                                                Surface(
                                                    color = MetGhamrCream,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            sub.nameAr,
                                                            fontSize = 11.sp,
                                                            color = MetGhamrNavy,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1
                                                        )
                                                        if (isExpanded && sub.keywords.isNotEmpty()) {
                                                            Text(
                                                                sub.keywords.joinToString("، "),
                                                                fontSize = 9.sp,
                                                                color = TextMuted,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            // Fill empty spaces in the row
                                            repeat(3 - rowSubs.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Dialog for Adding New Category
    if (isAddingNewCategory) {
        AdminAddEditCategoryDialog(
            category = null,
            onDismiss = { isAddingNewCategory = false },
            onSave = { newCategory ->
                viewModel.saveCategory(newCategory) {
                    isAddingNewCategory = false
                }
            }
        )
    }

    // Dialog for Editing Existing Category
    editingCategory?.let { cat ->
        AdminAddEditCategoryDialog(
            category = cat,
            onDismiss = { editingCategory = null },
            onSave = { updatedCategory ->
                viewModel.saveCategory(updatedCategory) {
                    editingCategory = null
                }
            }
        )
    }

    // Confirmation Dialog for Deleting Category
    categoryToDelete?.let { cat ->
        val registeredCount = allActiveBusinesses.count { it.categoryId == cat.id }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف التصنيف", fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                }
            },
            text = {
                Column {
                    Text("هل أنت متأكد من رغبتك في حذف تصنيف «${cat.nameAr}» نهائياً؟", fontSize = 13.sp)
                    if (registeredCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                "تنبيه: هناك $registeredCount منشأة مسجلة حالياً تتبع هذا التصنيف!",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat.id) {
                            categoryToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("نعم، حذف التصنيف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }

    // Confirmation Dialog for Resetting Categories
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MetGhamrNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استعادة التصنيفات الافتراضية", fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                }
            },
            text = {
                Text("هل تريد استعادة كافة التصنيفات والتخصصات الطبية والتجارية الافتراضية المعتمدة لدليل ميت غمر؟", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCategoriesToDefault()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                ) {
                    Text("استعادة الافتراضي", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء", color = TextMuted)
                }
            }
        )
    }
}

/**
 * High-End Add / Edit Category Dialog with Subcategories & Custom Color Palette
 */
@Composable
fun AdminAddEditCategoryDialog(
    category: CategoryItem?,
    onDismiss: () -> Unit,
    onSave: (CategoryItem) -> Unit
) {
    val isEdit = category != null
    var nameAr by remember { mutableStateOf(category?.nameAr ?: "") }
    var id by remember { mutableStateOf(category?.id ?: ("cat_" + UUID.randomUUID().toString().take(8))) }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var selectedColorHex by remember { mutableStateOf(category?.colorHex ?: 0xFF0A2540L) }
    var selectedIconName by remember { mutableStateOf(category?.iconName ?: "Folder") }
    var isFeatured by remember { mutableStateOf(category?.isFeatured ?: true) }
    var sortOrder by remember { mutableIntStateOf(category?.sortOrder ?: 1) }

    // Subcategories list state
    var subcategories by remember { mutableStateOf(category?.subcategories ?: emptyList()) }
    var newSubcategoryName by remember { mutableStateOf("") }
    var newSubcategoryKeywords by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetColors = listOf(
        0xFF0A2540L, // MetGhamr Navy
        0xFF0D9488L, // Teal
        0xFF059669L, // Emerald
        0xFFD97706L, // Amber
        0xFFDC2626L, // Crimson
        0xFF7C3AEDL, // Purple
        0xFF2563EBL, // Blue
        0xFFEA580CL  // Orange
    )

    val presetIcons = listOf(
        "Folder" to "مجلد عام",
        "LocalHospital" to "أطباء ومستشفيات",
        "Restaurant" to "مطاعم وكافيهات",
        "ShoppingBag" to "تسوق ومتاجر",
        "Build" to "حرف وصيانة",
        "School" to "تعليم وتدريب",
        "DirectionsCar" to "سيارات ومواصلات",
        "FitnessCenter" to "نوادي ورياضة",
        "LocalPharmacy" to "صيدليات وأدوية",
        "AccountBalance" to "بنوك ومؤسسات",
        "Spa" to "تجميل وعناية",
        "Star" to "خدمات مميزة"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("dialog_add_edit_category"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(selectedColorHex).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getCategoryVectorIcon(selectedIconName),
                                    contentDescription = null,
                                    tint = Color(selectedColorHex),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (isEdit) "تعديل التصنيف: ${category?.nameAr}" else "إضافة تصنيف جديد للدليل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MetGhamrNavy
                            )
                            Text(
                                "تخصيص البيانات، الألوان والتخصصات الفرعية",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.4f))

                // Scrollable Form Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category Name
                    item {
                        OutlinedTextField(
                            value = nameAr,
                            onValueChange = {
                                nameAr = it
                                if (!isEdit) {
                                    // Auto generate friendly slug if new
                                    id = "cat_" + it.trim().replace("\\s+".toRegex(), "_")
                                }
                            },
                            label = { Text("اسم التصنيف بالعربية *") },
                            placeholder = { Text("مثال: مستشفيات ومراكز طبية") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_category_name")
                        )
                    }

                    // Category ID & Sort Order
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = id,
                                onValueChange = { id = it },
                                label = { Text("معرف التصنيف (ID)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isEdit,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("input_category_id")
                            )

                            OutlinedTextField(
                                value = sortOrder.toString(),
                                onValueChange = { sortOrder = it.toIntOrNull() ?: 1 },
                                label = { Text("الترتيب") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_category_sort_order")
                            )
                        }
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("وصف التصنيف (اختياري)") },
                            placeholder = { Text("وصف مختصر للأنشطة والخدمات المندرجة...") },
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_category_description")
                        )
                    }

                    // Color Picker
                    item {
                        Column {
                            Text("اختر لون التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetColors) { colHex ->
                                    val isSelected = selectedColorHex == colHex
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(colHex))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MetGhamrNavy else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColorHex = colHex },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Icon Picker
                    item {
                        Column {
                            Text("اختر أيقونة التصنيف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetIcons) { (iconKey, label) ->
                                    val isSelected = selectedIconName == iconKey
                                    Surface(
                                        color = if (isSelected) Color(selectedColorHex).copy(alpha = 0.2f) else MetGhamrCream,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(selectedColorHex) else Color.LightGray.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.clickable { selectedIconName = iconKey }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = getCategoryVectorIcon(iconKey),
                                                contentDescription = null,
                                                tint = if (isSelected) Color(selectedColorHex) else MetGhamrNavy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                label,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color(selectedColorHex) else MetGhamrNavy,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Featured Switch
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MetGhamrCream.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("إظهار كتصنيف مميز في الرئيسية ⭐", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrNavy)
                                Text("يظهر مباشرة في بطاقات الصفحة الرئيسية للدليل", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = isFeatured,
                                onCheckedChange = { isFeatured = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MetGhamrNavy
                                )
                            )
                        }
                    }

                    // Subcategories Management Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MetGhamrCream.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "التخصصات والتصنيفات الفرعية (${subcategories.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MetGhamrNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Add subcategory input row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newSubcategoryName,
                                        onValueChange = { newSubcategoryName = it },
                                        placeholder = { Text("اسم التخصص الفرعي...", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("input_new_subcategory_name")
                                    )

                                    Button(
                                        onClick = {
                                            val trimmed = newSubcategoryName.trim()
                                            if (trimmed.isNotBlank()) {
                                                val kwList = newSubcategoryKeywords.split("،", ",")
                                                    .map { it.trim() }
                                                    .filter { it.isNotBlank() }
                                                val newSub = SubcategoryItem(
                                                    id = "sub_" + UUID.randomUUID().toString().take(8),
                                                    parentCategoryId = id,
                                                    nameAr = trimmed,
                                                    keywords = kwList
                                                )
                                                subcategories = subcategories + newSub
                                                newSubcategoryName = ""
                                                newSubcategoryKeywords = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("btn_add_subcategory_item")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إضافة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = newSubcategoryKeywords,
                                    onValueChange = { newSubcategoryKeywords = it },
                                    placeholder = { Text("كلمات مفتاحية ومترادفات للبحث (مفصولة بفاصلة)...", fontSize = 10.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("input_new_subcategory_keywords")
                                )

                                if (subcategories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("قائمة التخصصات المضافة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        subcategories.forEachIndexed { index, sub ->
                                            Surface(
                                                color = Color.White,
                                                shape = RoundedCornerShape(10.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            "${index + 1}.",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MetGhamrNavy
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                sub.nameAr,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MetGhamrNavy
                                                            )
                                                            if (sub.keywords.isNotEmpty()) {
                                                                Text(
                                                                    "مترادفات: " + sub.keywords.joinToString("، "),
                                                                    fontSize = 10.sp,
                                                                    color = TextMuted
                                                                )
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            subcategories = subcategories.filterNot { it.id == sub.id }
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "حذف التخصص",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(16.dp)
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

                    // Error Message
                    errorMessage?.let { msg ->
                        item {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    msg,
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.4f))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء", color = TextMuted, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val cleanName = nameAr.trim()
                            val cleanId = id.trim()
                            if (cleanName.isBlank()) {
                                errorMessage = "يرجى إدخال اسم التصنيف بالعربية"
                                return@Button
                            }
                            if (cleanId.isBlank()) {
                                errorMessage = "يرجى تحديد معرف التصنيف (ID)"
                                return@Button
                            }

                            val updatedCategory = CategoryItem(
                                id = cleanId,
                                nameAr = cleanName,
                                iconName = selectedIconName,
                                colorHex = selectedColorHex,
                                description = description.trim(),
                                isFeatured = isFeatured,
                                sortOrder = sortOrder,
                                subcategories = subcategories.map { it.copy(parentCategoryId = cleanId) }
                            )
                            onSave(updatedCategory)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_category_submit")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isEdit) "حفظ التعديلات" else "إضافة التصنيف للدليل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns matching Material Vector icon for the category icon name
 */
fun getCategoryVectorIcon(iconName: String): ImageVector {
    return when (iconName.trim().lowercase()) {
        "localhospital", "hospital", "doctor", "medical" -> Icons.Default.LocalHospital
        "restaurant", "food", "dining" -> Icons.Default.Restaurant
        "shoppingbag", "shopping", "store" -> Icons.Default.ShoppingBag
        "build", "handyman", "services" -> Icons.Default.Build
        "school", "education" -> Icons.Default.School
        "directionscar", "car", "taxi" -> Icons.Default.DirectionsCar
        "fitnesscenter", "gym", "sports" -> Icons.Default.FitnessCenter
        "localpharmacy", "pharmacy" -> Icons.Default.LocalPharmacy
        "accountbalance", "bank" -> Icons.Default.AccountBalance
        "spa", "beauty" -> Icons.Default.Spa
        "star", "favorite" -> Icons.Default.Star
        else -> Icons.Default.Folder
    }
}

