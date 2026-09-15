package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.model.CategoryItem
import com.example.ui.components.BusinessCard
import com.example.ui.components.CompactFeaturedCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SkeletonBusinessCard
import com.example.ui.components.getCategoryPastelColors
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DirectoryViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val categories by viewModel.categoriesWithCounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedArea by viewModel.selectedArea.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val openNowOnly by viewModel.openNowOnly.collectAsState()
    val verifiedOnly by viewModel.verifiedOnly.collectAsState()
    val businesses by viewModel.filteredBusinesses.collectAsState()
    val featuredBusinesses by viewModel.featuredBusinesses.collectAsState()
    val topRatedBusinesses by viewModel.topRatedBusinesses.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()

    val favIds = favorites.map { it.businessId }.toSet()

    val villagesAndAreas = listOf(
        "الكل", "مدينة ميت غمر", "بشلا", "صهرجت الكبرى", "ميت ناجي", "تفهنا الأشراف", "دنديط",
        "أتميدة", "كوم النور", "سنفا", "أوليلة", "دماص", "شارع الحرية", "شارع المحطة"
    )

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // حساب موضع بداية نتائج البحث بدقة
    val resultsHeaderIndex = remember(featuredBusinesses.size, topRatedBusinesses.size, searchQuery, selectedCategory) {
        var idx = 2 // home_header (0) + categories_section (1)
        if (featuredBusinesses.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == null) {
            idx++
        }
        if (topRatedBusinesses.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == null) {
            idx++
        }
        idx++ // area_filters_section
        idx // main_list_header
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != null) {
            delay(100)
            listState.animateScrollToItem(index = resultsHeaderIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- 1. Header & Identity: Search & Smart AI Assistant Side-by-Side ---
        item(key = "home_header") {
            Surface(
                color = SurfaceCard,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // شريط البحث الموحد والشامل (All-in-One Search & AI Bar)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            viewModel.updateSearchQuery(it)
                        },
                        placeholder = {
                            Text(
                                text = "ابحث عن مطعم، محل، عيادة، خدمة...",
                                fontSize = 13.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "بحث",
                                tint = SkyBlueDark,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.updateSearchQuery("") },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                // زر المساعد الذكي المدمج بتدرج لوني انسيابي (All-in-One AI Pill)
                                Surface(
                                    onClick = onNavigateToAiAssistant,
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Transparent,
                                    shadowElevation = 2.dp,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFF00C6FF),
                                                    Color(0xFF0072FF)
                                                )
                                            )
                                        )
                                        .testTag("home_ai_assistant_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SmartToy,
                                                contentDescription = "المساعد الذكي",
                                                tint = Color(0xFF0072FF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = "المساعد الذكي",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.5.sp
                                            ),
                                            maxLines = 1
                                        )

                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(26.dp),
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
                            .heightIn(min = 52.dp)
                            .testTag("home_search_input")
                    )
                }
            }
        }

        // --- 4. Categories Colorful Cubes Matrix (4 cubes per row default) ---
        item(key = "categories_section") {
            var gridColumns by remember { mutableIntStateOf(4) }

            val cubesList12 = remember {
                listOf(
                    CategoryCubeItem("cat_restaurants", "مطاعم", Icons.Default.Restaurant, Color(0xFFFF5252), "cat_restaurants"),
                    CategoryCubeItem("cat_shops", "محلات", Icons.Default.Storefront, Color(0xFF66BB6A), "cat_shops"),
                    CategoryCubeItem("cat_doctors", "أطباء", Icons.Default.MedicalServices, Color(0xFF9575CD), "cat_doctors"),
                    CategoryCubeItem("cat_hospitals", "مستشفيات", Icons.Default.LocalHospital, Color(0xFF26C6DA), "cat_hospitals"),
                    CategoryCubeItem("cat_cafes", "كافيهات", Icons.Default.LocalCafe, Color(0xFFEC407A), "cat_cafes"),
                    CategoryCubeItem("cat_technicians", "فنيين", Icons.Default.Build, Color(0xFF48CFAD), "cat_technicians"),
                    CategoryCubeItem("cat_factories", "مصانع", Icons.Default.Factory, Color(0xFFFFA726), "cat_factories"),
                    CategoryCubeItem("cat_companies", "شركات", Icons.Default.Business, Color(0xFF29B6F6), "cat_companies"),
                    CategoryCubeItem("cat_pharmacies", "صيدليات", Icons.Default.LocalPharmacy, Color(0xFF00ACC1), "cat_pharmacies"),
                    CategoryCubeItem("cat_universities", "تعليم", Icons.Default.School, Color(0xFF5C6BC0), "cat_universities"),
                    CategoryCubeItem("cat_automotive", "سيارات", Icons.Default.DirectionsCar, Color(0xFF7E57C2), "cat_automotive"),
                    CategoryCubeItem("more", "المزيد", Icons.Default.Apps, Color(0xFFAB47BC), null)
                )
            }

            val cubesList9 = remember {
                listOf(
                    CategoryCubeItem("cat_restaurants", "مطاعم", Icons.Default.Restaurant, Color(0xFFFF5252), "cat_restaurants"),
                    CategoryCubeItem("cat_shops", "محلات", Icons.Default.Storefront, Color(0xFF66BB6A), "cat_shops"),
                    CategoryCubeItem("cat_doctors", "أطباء", Icons.Default.MedicalServices, Color(0xFF9575CD), "cat_doctors"),
                    CategoryCubeItem("cat_hospitals", "مستشفيات", Icons.Default.LocalHospital, Color(0xFF26C6DA), "cat_hospitals"),
                    CategoryCubeItem("cat_cafes", "كافيهات", Icons.Default.LocalCafe, Color(0xFFEC407A), "cat_cafes"),
                    CategoryCubeItem("cat_technicians", "فنيين", Icons.Default.Build, Color(0xFF48CFAD), "cat_technicians"),
                    CategoryCubeItem("cat_factories", "مصانع", Icons.Default.Factory, Color(0xFFFFA726), "cat_factories"),
                    CategoryCubeItem("cat_companies", "شركات", Icons.Default.Business, Color(0xFF29B6F6), "cat_companies"),
                    CategoryCubeItem("more", "المزيد", Icons.Default.Apps, Color(0xFFAB47BC), null)
                )
            }

            val activeCubes = if (gridColumns == 4) cubesList12 else cubesList9

            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التصنيفات الرئيسية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Matrix Column Toggle (4 cubes vs 3 cubes)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceLight,
                            border = BorderStroke(1.dp, BorderLight),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (gridColumns == 4) SkyBluePrimary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { gridColumns = 4 }
                                ) {
                                    Text(
                                        text = "4 مكعبات",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (gridColumns == 4) FontWeight.Bold else FontWeight.Normal,
                                        color = if (gridColumns == 4) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (gridColumns == 3) SkyBluePrimary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { gridColumns = 3 }
                                ) {
                                    Text(
                                        text = "3 مكعبات",
                                        fontSize = 10.5.sp,
                                        fontWeight = if (gridColumns == 3) FontWeight.Bold else FontWeight.Normal,
                                        color = if (gridColumns == 3) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = onNavigateToCategories,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "عرض الكل",
                                color = SkyBluePrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Matrix of Cubes Grid
                val chunkedRows = activeCubes.chunked(gridColumns)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunkedRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { cube ->
                                Box(modifier = Modifier.weight(1f)) {
                                    val isSelected = selectedCategory?.id == cube.categoryId
                                    CategoryCubeCard(
                                        title = cube.title,
                                        icon = cube.icon,
                                        backgroundColor = cube.color,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (cube.id == "more") {
                                                onNavigateToCategories()
                                            } else {
                                                val found = categories.find {
                                                    it.id == cube.categoryId || it.nameAr.contains(cube.title)
                                                }
                                                val isDeselecting = found != null && selectedCategory?.id == found.id
                                                if (found != null) {
                                                    if (isDeselecting) {
                                                        viewModel.selectCategory(null)
                                                    } else {
                                                        viewModel.selectCategory(found)
                                                    }
                                                } else {
                                                    viewModel.updateSearchQuery(cube.title)
                                                }
                                                if (!isDeselecting) {
                                                    coroutineScope.launch {
                                                        delay(120)
                                                        listState.animateScrollToItem(index = 3)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Featured Businesses Section ---
        if (featuredBusinesses.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == null) {
            item(key = "featured_section") {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "منشآت مميزة ⭐",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Surface(
                            color = SkyBlueContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "موصى بها",
                                color = SkyBlueDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(featuredBusinesses, key = { "feat_${it.id}" }) { business ->
                            CompactFeaturedCard(
                                business = business,
                                isFavorite = favIds.contains(business.id),
                                onClick = { viewModel.selectBusiness(business.id) },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(business.id, favIds.contains(business.id))
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 5. Top Rated Businesses Section ---
        if (topRatedBusinesses.isNotEmpty() && searchQuery.isEmpty() && selectedCategory == null) {
            item(key = "top_rated_section") {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(
                        text = "الأعلى تقييمًا في ميت غمر 🏆",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topRatedBusinesses, key = { "top_${it.id}" }) { business ->
                            CompactFeaturedCard(
                                business = business,
                                isFavorite = favIds.contains(business.id),
                                onClick = { viewModel.selectBusiness(business.id) },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(business.id, favIds.contains(business.id))
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 6. Area Filters & Quick Badges ---
        item(key = "area_filters_section") {
            Column(modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
                Text(
                    text = "تصفية حسب المنطقة أو القرية:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(villagesAndAreas) { area ->
                        val isSelected = selectedArea == area
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectArea(area) },
                            label = {
                                Text(
                                    text = area,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyBluePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceCard,
                                labelColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = openNowOnly,
                        onClick = { viewModel.toggleOpenNowFilter() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("مفتوح الآن فقط", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LettuceGreen,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = verifiedOnly,
                        onClick = { viewModel.toggleVerifiedFilter() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("موثق فقط ✔️", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyBluePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }

        // --- 7. Main Directory Header ---
        item(key = "main_list_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (selectedCategory != null) "نتائج ${selectedCategory?.nameAr}" else "جميع الأنشطة والخدمات",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    if (selectedCategory != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SkyBlueContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.selectCategory(null)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "إلغاء التصفية ✕",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBlueDark
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "${businesses.size} نشاط",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // --- 8. Businesses List or Empty State / Loading ---
        if (businesses.isEmpty()) {
            if (firestoreSyncStatus.isSyncing || isRefreshing) {
                items(3, key = { "loading_skeleton_$it" }) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SkeletonBusinessCard()
                    }
                }
            } else {
                item(key = "empty_state") {
                    EmptyStateView(
                        icon = Icons.Default.SearchOff,
                        title = "لا توجد نتائج مطابقة",
                        description = "لم نعثر على أي أنشطة تطابق محددات البحث الحالية في ميت غمر. جرب تغيير الكلمات المفتاحية أو إعادة ضبط الفلاتر.",
                        actionButtonText = "إعادة ضبط الفلاتر",
                        onActionClick = {
                            viewModel.updateSearchQuery("")
                            viewModel.selectCategory(null)
                            viewModel.selectArea("الكل")
                        }
                    )
                }
            }
        } else {
            items(businesses, key = { "main_${it.id}" }) { business ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    BusinessCard(
                        business = business,
                        isFavorite = favIds.contains(business.id),
                        onClick = { viewModel.selectBusiness(business.id) },
                        onFavoriteToggle = {
                            viewModel.toggleFavorite(business.id, favIds.contains(business.id))
                        }
                    )
                }
            }
        }
    }
}

data class CategoryCubeItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val categoryId: String? = null
)

@Composable
fun CategoryCubeCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = if (isSelected) BorderStroke(2.5.dp, Color.White) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp, pressedElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("category_cube_$title")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryCarouselChip(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (catBg, catIconColor) = getCategoryPastelColors(category.id)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SkyBluePrimary else SurfaceCard
        ),
        border = BorderStroke(1.dp, if (isSelected) SkyBluePrimary else BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .clickable { onClick() }
            .testTag("category_chip_${category.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.25f) else catBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveCategoryIcon(category.iconName, category.id),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else catIconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.nameAr,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 12.sp
                )
            )
        }
    }
}
