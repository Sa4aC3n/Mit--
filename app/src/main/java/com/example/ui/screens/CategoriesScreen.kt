package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.SubcategoryItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.getCategoryPastelColors
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

enum class CategoryGroupFilter(val titleAr: String) {
    ALL("الكل"),
    HEALTH("صحة وأطباء"),
    FOOD("مطاعم وكافيهات"),
    TECHNICAL("فنيين وصناعة"),
    SHOPPING("تسوق وتجار"),
    SERVICES("خدمات ومناسبات")
}

enum class GridColumnsMode(val columns: Int, val label: String) {
    TWO(2, "شبكة 2x"),
    THREE(3, "شبكة 3x"),
    ONE(1, "قائمة")
}

enum class SubcatViewMode {
    GRID,
    LIST
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    viewModel: DirectoryViewModel,
    onCategorySelected: (CategoryItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var gridMode by remember { mutableStateOf(GridColumnsMode.TWO) }
    var activeCategoryForSubModal by remember { mutableStateOf<CategoryItem?>(null) }

    val allCategories by viewModel.categoriesWithCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val totalActivities = remember(allCategories) { allCategories.sumOf { it.count } }

    // Search and filter categories
    val filteredCategories = remember(allCategories, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            allCategories
        } else {
            allCategories.filter { cat ->
                cat.nameAr.contains(query, ignoreCase = true) ||
                        cat.description.contains(query, ignoreCase = true) ||
                        cat.id.contains(query, ignoreCase = true) ||
                        cat.subcategories.any { sub -> sub.nameAr.contains(query, ignoreCase = true) || sub.keywords.any { it.contains(query, ignoreCase = true) } }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // --- 1. Header & Identity ---
        Surface(
            color = SurfaceCard,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = SkyBlueContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "دليل التصنيفات والخدمات 🗂️",
                                color = SkyBlueDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تصفح خدمات ميت غمر حسب التخصص",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "${allCategories.size} قسم رئيسي • $totalActivities نشاط مسجل",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Layout Grid Mode Switcher with clear active states
                    Surface(
                        color = SurfaceLight,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (gridMode == GridColumnsMode.TWO) SkyBluePrimary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { gridMode = GridColumnsMode.TWO }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("grid_mode_two"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "شبكة 2x",
                                        tint = if (gridMode == GridColumnsMode.TWO) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            Surface(
                                color = if (gridMode == GridColumnsMode.THREE) SkyBluePrimary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { gridMode = GridColumnsMode.THREE }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("grid_mode_three"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = "شبكة 3x",
                                        tint = if (gridMode == GridColumnsMode.THREE) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            Surface(
                                color = if (gridMode == GridColumnsMode.ONE) SkyBluePrimary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { gridMode = GridColumnsMode.ONE }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("grid_mode_one"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewList,
                                        contentDescription = "قائمة",
                                        tint = if (gridMode == GridColumnsMode.ONE) Color.White else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- 2. Category Search Bar ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث في التصنيفات (مطاعم، أطباء، فنيين...)",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "بحث",
                            tint = SkyBluePrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceLight,
                        unfocusedContainerColor = SurfaceLight,
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = BorderLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("categories_search_input")
                )
            }
        }

        // --- Main Category Grid ---
        if (filteredCategories.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Category,
                title = "لم نجد تصنيفاً مطابقاً لبحثك",
                description = "جرب البحث بكلمة أخرى مثل 'طبيب'، 'مطعم'، 'صيدلية'، أو 'كهربائي'.",
                actionButtonText = "إعادة ضبط البحث",
                onActionClick = { searchQuery = "" }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridMode.columns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("categories_grid_list")
            ) {
                items(filteredCategories, key = { it.id }) { category ->
                    CategoryGridCard(
                        category = category,
                        compactMode = gridMode == GridColumnsMode.THREE,
                        isListMode = gridMode == GridColumnsMode.ONE,
                        onClick = {
                            if (category.subcategories.isNotEmpty()) {
                                activeCategoryForSubModal = category
                            } else {
                                viewModel.selectCategory(category)
                                onCategorySelected(category)
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Subcategories Bottom Sheet Modal ---
    activeCategoryForSubModal?.let { cat ->
        ModalBottomSheet(
            onDismissRequest = { activeCategoryForSubModal = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            SubcategorySelectionContent(
                category = cat,
                onSubcategorySelect = { sub ->
                    activeCategoryForSubModal = null
                    viewModel.selectCategory(cat)
                    viewModel.selectSubcategory(sub)
                    onCategorySelected(cat)
                },
                onViewAllCategory = {
                    activeCategoryForSubModal = null
                    viewModel.selectCategory(cat)
                    viewModel.selectSubcategory(null)
                    onCategorySelected(cat)
                }
            )
        }
    }
}

@Composable
fun PopularCategoryChip(
    category: CategoryItem,
    onClick: () -> Unit
) {
    val (catBg, catIconColor) = getCategoryPastelColors(category.id)
    val icon = remember(category.iconName, category.id) { resolveCategoryIcon(category.iconName, category.id) }

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderLight),
        shadowElevation = 1.dp,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("featured_category_chip_${category.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(catBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.nameAr,
                    tint = catIconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = category.nameAr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${category.count} نشاط",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun CategoryGridCard(
    category: CategoryItem,
    compactMode: Boolean,
    isListMode: Boolean = false,
    onClick: () -> Unit
) {
    val (catBg, catIconColor) = getCategoryPastelColors(category.id)
    val iconVector = remember(category.iconName, category.id) { resolveCategoryIcon(category.iconName, category.id) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_grid_${category.id}")
    ) {
        if (isListMode) {
            // Row / List layout when 1-column is selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(catBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = category.nameAr,
                            tint = catIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.nameAr,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (category.description.isNotEmpty()) {
                            Text(
                                text = category.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = catBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${category.count} نشاط",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = catIconColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (category.subcategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${category.subcategories.size} تخصص فرعي",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            // Square / Grid card layout (2x or 3x)
            Column(
                modifier = Modifier
                    .padding(if (compactMode) 10.dp else 14.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(if (compactMode) 44.dp else 54.dp)
                        .clip(CircleShape)
                        .background(catBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = category.nameAr,
                        tint = catIconColor,
                        modifier = Modifier.size(if (compactMode) 22.dp else 28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(if (compactMode) 6.dp else 10.dp))

                // Category Title
                Text(
                    text = category.nameAr,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compactMode) 12.sp else 14.sp,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (!compactMode && category.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Count & Subcategories indicator badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = catBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${category.count} نشاط",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = catIconColor,
                                fontSize = if (compactMode) 10.sp else 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (category.subcategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = SurfaceSubtle,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${category.subcategories.size} فروع",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun resolveSubcategoryIcon(subcatName: String, categoryId: String, fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector): androidx.compose.ui.graphics.vector.ImageVector {
    val name = subcatName.lowercase()
    return when {
        name.contains("مجلس") || name.contains("محلية") || name.contains("حي") -> Icons.Default.AccountBalance
        name.contains("سجل") || name.contains("عقاري") || name.contains("توثيق") || name.contains("بطاق") -> Icons.Default.Badge
        name.contains("بريد") || name.contains("تأمين") || name.contains("معاش") -> Icons.Default.LocalPostOffice
        name.contains("تموين") || name.contains("بطاقة") -> Icons.Default.ShoppingCart
        name.contains("مرور") || name.contains("رخص") -> Icons.Default.Traffic
        name.contains("صحة") || name.contains("تأمين صحي") -> Icons.Default.LocalHospital
        name.contains("أزهر") || name.contains("جامع") || name.contains("كلي") -> Icons.Default.School
        name.contains("مدرس") || name.contains("ثانوي") || name.contains("إعدادي") || name.contains("ابتدائي") -> Icons.Default.MenuBook
        name.contains("صيدلي") || name.contains("دواء") -> Icons.Default.LocalPharmacy
        name.contains("معمل") || name.contains("تحاليل") || name.contains("أشعة") -> Icons.Default.Biotech
        name.contains("عياد") || name.contains("طبيب") || name.contains("دكتور") -> Icons.Default.MedicalServices
        name.contains("مستشف") || name.contains("طوارئ") -> Icons.Default.LocalHospital
        name.contains("بنك") || name.contains("صراف") || name.contains("atm") -> Icons.Default.AccountBalanceWallet
        name.contains("نقاب") || name.contains("مهندس") || name.contains("معلم") || name.contains("محام") -> Icons.Default.Groups
        name.contains("جمعية") || name.contains("خير") || name.contains("أيتام") -> Icons.Default.VolunteerActivism
        name.contains("كهرب") || name.contains("سباك") || name.contains("صيانة") || name.contains("نجار") -> Icons.Default.Handyman
        name.contains("مصنع") || name.contains("ألمونيوم") || name.contains("ورشة") -> Icons.Default.Factory
        name.contains("مطعم") || name.contains("مشويات") || name.contains("وجب") || name.contains("أسماك") -> Icons.Default.Restaurant
        name.contains("كافيه") || name.contains("مقهى") || name.contains("عصير") -> Icons.Default.Coffee
        name.contains("سيار") || name.contains("ميكانيك") || name.contains("قطع غيار") -> Icons.Default.DirectionsCar
        name.contains("جيم") || name.contains("نادي") || name.contains("رياض") -> Icons.Default.FitnessCenter
        name.contains("ملابس") || name.contains("أحذي") || name.contains("محل") || name.contains("ماركت") -> Icons.Default.Storefront
        else -> fallbackIcon
    }
}

@Composable
fun SubcategorySquareCard(
    subcategory: SubcategoryItem,
    categoryColor: Color,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    categoryId: String,
    onClick: () -> Unit
) {
    val subcatIcon = remember(subcategory.nameAr, categoryId) {
        resolveSubcategoryIcon(subcategory.nameAr, categoryId, fallbackIcon)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable { onClick() }
            .testTag("subcat_item_${subcategory.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subcatIcon,
                    contentDescription = subcategory.nameAr,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Title
            Text(
                text = subcategory.nameAr,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = TextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Activity count or explore pill
            Surface(
                color = if (subcategory.count > 0) categoryColor.copy(alpha = 0.12f) else SurfaceSubtle,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (subcategory.count > 0) "${subcategory.count} نشاط" else "استكشاف",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (subcategory.count > 0) categoryColor else TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SubcategorySelectionContent(
    category: CategoryItem,
    onSubcategorySelect: (SubcategoryItem) -> Unit,
    onViewAllCategory: () -> Unit
) {
    var subcatViewMode by remember { mutableStateOf(SubcatViewMode.GRID) }
    val categoryColor = Color(category.colorHex)
    val iconVector = remember(category.iconName, category.id) { resolveCategoryIcon(category.iconName, category.id) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
    ) {
        // Top Header Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nameAr,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = category.description.ifEmpty { "اختر التخصص الفرعي المناسب في ميت غمر" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title and Mode Switcher Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "التخصصات والأقسام الفرعية (${category.subcategories.size}):",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            // Switcher for Subcategories View: Grid (بطاقات مربعة) vs List (قائمة)
            Surface(
                color = SurfaceSubtle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (subcatViewMode == SubcatViewMode.GRID) MetGhamrNavy else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { subcatViewMode = SubcatViewMode.GRID }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "بطاقات مربعة",
                                tint = if (subcatViewMode == SubcatViewMode.GRID) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مربعات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (subcatViewMode == SubcatViewMode.GRID) Color.White else TextSecondary
                            )
                        }
                    }

                    Surface(
                        color = if (subcatViewMode == SubcatViewMode.LIST) MetGhamrNavy else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { subcatViewMode = SubcatViewMode.LIST }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewList,
                                contentDescription = "قائمة",
                                tint = if (subcatViewMode == SubcatViewMode.LIST) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "قائمة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (subcatViewMode == SubcatViewMode.LIST) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "View All" Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MetGhamrNavy.copy(alpha = 0.06f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrNavy.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewAllCategory() }
                .testTag("subcat_all_${category.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MetGhamrNavy.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MetGhamrNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "عرض جميع أنشطة ${category.nameAr}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy
                        )
                    )
                }

                Surface(
                    color = MetGhamrNavy,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${category.count} نشاط",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (subcatViewMode == SubcatViewMode.GRID) {
            // Display Subcategories as Beautiful Square Cards in 2 Columns Grid
            val chunkedSubcats = category.subcategories.chunked(2)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                chunkedSubcats.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { subcat ->
                            Box(modifier = Modifier.weight(1f)) {
                                SubcategorySquareCard(
                                    subcategory = subcat,
                                    categoryColor = categoryColor,
                                    fallbackIcon = iconVector,
                                    categoryId = category.id,
                                    onClick = { onSubcategorySelect(subcat) }
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Display Subcategories as Horizontal List Rows
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                category.subcategories.forEach { subcat ->
                    val subcatIcon = remember(subcat.nameAr, category.id) {
                        resolveSubcategoryIcon(subcat.nameAr, category.id, iconVector)
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubcategorySelect(subcat) }
                            .testTag("subcat_item_${subcat.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(categoryColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = subcatIcon,
                                        contentDescription = null,
                                        tint = categoryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = subcat.nameAr,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                            }

                            if (subcat.count > 0) {
                                Surface(
                                    color = categoryColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${subcat.count} نشاط",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = categoryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
