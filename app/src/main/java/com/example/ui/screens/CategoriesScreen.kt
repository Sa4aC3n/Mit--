package com.example.ui.screens

import androidx.compose.animation.*
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
    THREE(3, "شبكة 3x")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(
    viewModel: DirectoryViewModel,
    onCategorySelected: (CategoryItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(CategoryGroupFilter.ALL) }
    var gridMode by remember { mutableStateOf(GridColumnsMode.TWO) }
    var activeCategoryForSubModal by remember { mutableStateOf<CategoryItem?>(null) }

    val allCategories by viewModel.categoriesWithCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val totalActivities = remember(allCategories) { allCategories.sumOf { it.count } }
    val featuredCategories = remember(allCategories) { allCategories.filter { it.isFeatured } }

    // Search and filter categories
    val filteredCategories = remember(allCategories, selectedGroup, searchQuery) {
        allCategories.filter { cat ->
            val matchesGroup = when (selectedGroup) {
                CategoryGroupFilter.ALL -> true
                CategoryGroupFilter.HEALTH -> cat.id in listOf("cat_doctors", "cat_hospitals", "cat_radiology", "cat_pharmacies", "cat_medical_centers")
                CategoryGroupFilter.FOOD -> cat.id in listOf("cat_restaurants", "cat_cafes")
                CategoryGroupFilter.TECHNICAL -> cat.id in listOf("cat_technicians", "cat_factories", "cat_automotive", "cat_companies")
                CategoryGroupFilter.SHOPPING -> cat.id in listOf("cat_shops", "cat_libraries")
                CategoryGroupFilter.SERVICES -> cat.id in listOf("cat_clubs", "cat_education", "cat_home_events", "cat_government", "cat_universities", "cat_schools", "cat_banks", "cat_syndicates", "cat_charities")
            }

            val query = searchQuery.trim()
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                cat.nameAr.contains(query, ignoreCase = true) ||
                        cat.description.contains(query, ignoreCase = true) ||
                        cat.id.contains(query, ignoreCase = true) ||
                        cat.subcategories.any { sub -> sub.nameAr.contains(query, ignoreCase = true) || sub.keywords.any { it.contains(query, ignoreCase = true) } }
            }

            matchesGroup && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // --- 1. Header & Identity ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MetGhamrNavy, MetGhamrBlue)
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = MetGhamrGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "دليل التصنيفات والخدمات 🗂️",
                                color = MetGhamrGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تصفح خدمات ميت غمر حسب التخصص",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "${allCategories.size} قسم رئيسي • $totalActivities نشاط ومحل مسجل",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Layout Grid Mode Switcher
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { gridMode = GridColumnsMode.TWO },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "شبكة مكبرة",
                                    tint = if (gridMode == GridColumnsMode.TWO) MetGhamrGold else Color.White.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(
                                onClick = { gridMode = GridColumnsMode.THREE },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridOn,
                                    contentDescription = "شبكة مصغرة",
                                    tint = if (gridMode == GridColumnsMode.THREE) MetGhamrGold else Color.White.copy(alpha = 0.6f)
                                )
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
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "بحث",
                            tint = MetGhamrNavy
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MetGhamrGold,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("categories_search_input")
                )
            }
        }

        // --- 3. Featured / Popular Categories Section ---
        if (searchQuery.isEmpty() && selectedGroup == CategoryGroupFilter.ALL && featuredCategories.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Text(
                    text = "الأكثر استخداماً ⭐",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(featuredCategories, key = { "feat_cat_${it.id}" }) { category ->
                        PopularCategoryChip(
                            category = category,
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

        // --- 4. Group Filter Chips ---
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(CategoryGroupFilter.values()) { group ->
                val isSelected = selectedGroup == group
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGroup = group },
                    label = {
                        Text(
                            text = group.titleAr,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetGhamrNavy,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = TextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MetGhamrNavy else BorderLight
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("category_group_chip_${group.name}")
                )
            }
        }

        // --- 5. Main Category Grid ---
        if (filteredCategories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لم نجد تصنيفاً مطابقاً لبحثك",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جرب كلمة أخرى مثل 'مطعم'، 'عيادة'، 'سباك'، 'صيدلية'",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedGroup = CategoryGroupFilter.ALL
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                    ) {
                        Text("إعادة ضبط البحث")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridMode.columns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
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
    val categoryColor = Color(category.colorHex)
    val icon = remember(category.iconName, category.id) { resolveCategoryIcon(category.iconName, category.id) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.nameAr,
                    tint = categoryColor,
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
    onClick: () -> Unit
) {
    val categoryColor = Color(category.colorHex)
    val iconVector = remember(category.iconName, category.id) { resolveCategoryIcon(category.iconName, category.id) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = BorderLight,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("category_grid_${category.id}")
    ) {
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
                    .size(if (compactMode) 44.dp else 56.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = category.nameAr,
                    tint = categoryColor,
                    modifier = Modifier.size(if (compactMode) 24.dp else 30.dp)
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
                    color = categoryColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${category.count} نشاط",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = categoryColor,
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
                        shape = RoundedCornerShape(10.dp)
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

@Composable
fun SubcategorySelectionContent(
    category: CategoryItem,
    onSubcategorySelect: (SubcategoryItem) -> Unit,
    onViewAllCategory: () -> Unit
) {
    val categoryColor = Color(category.colorHex)
    val iconVector = remember(category.iconName, category.id) { resolveCategoryIcon(category.iconName, category.id) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "التخصصات والأقسام الفرعية:",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // "View All" Option
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MetGhamrNavy.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetGhamrNavy.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewAllCategory() }
                    .testTag("subcat_all_${category.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MetGhamrNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "عرض جميع أنشطة ${category.nameAr}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            )
                        )
                    }

                    Text(
                        text = "${category.count} نشاط",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetGhamrNavy
                    )
                }
            }

            // List of subcategories
            category.subcategories.forEach { subcat ->
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
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SubdirectoryArrowLeft,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
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

        Spacer(modifier = Modifier.height(20.dp))
    }
}
