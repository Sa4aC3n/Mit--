package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.SubcategoryItem
import com.example.ui.components.BusinessCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import com.example.ui.viewmodel.SearchSortOption

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchDirectoryScreen(
    viewModel: DirectoryViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val businesses by viewModel.filteredBusinesses.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSubcategory by viewModel.selectedSubcategory.collectAsState()
    val selectedArea by viewModel.selectedArea.collectAsState()
    val openNowOnly by viewModel.openNowOnly.collectAsState()
    val verifiedOnly by viewModel.verifiedOnly.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    val currentSortOption by viewModel.searchSortOption.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val categories by viewModel.categoriesWithCounts.collectAsState()

    val favIds = remember(favorites) { favorites.map { it.businessId }.toSet() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var showFilterBottomSheet by remember { mutableStateOf(false) }

    val popularSearches = remember {
        listOf(
            "مطاعم مشويات",
            "أطباء أطفال",
            "صيدليات 24 ساعة",
            "كهربائي منازل",
            "ورش ألومنيوم",
            "طوارئ عظام",
            "سباك صيانة",
            "قطع غيار سيارات"
        )
    }

    val activeFiltersCount = remember(
        selectedCategory, selectedSubcategory, selectedArea,
        openNowOnly, verifiedOnly, minRating, currentSortOption
    ) {
        var count = 0
        if (selectedCategory != null) count++
        if (selectedSubcategory != null) count++
        if (selectedArea != "الكل") count++
        if (openNowOnly) count++
        if (verifiedOnly) count++
        if (minRating > 0f) count++
        if (currentSortOption != SearchSortOption.RELEVANCE) count++
        count
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // --- 1. Header & Search Input Box ---
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
                    Column {
                        Text(
                            text = "محرك البحث والاكتشاف 🔍",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetGhamrGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = "دليل ميت غمر الشامل",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }

                    // Filter Button with Badge
                    Surface(
                        color = if (activeFiltersCount > 0) MetGhamrGold else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { showFilterBottomSheet = true }
                            .testTag("open_filter_bottom_sheet_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "تصفية وترتيب",
                                tint = if (activeFiltersCount > 0) MetGhamrNavy else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeFiltersCount > 0) "فلاتر ($activeFiltersCount)" else "تصفية",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeFiltersCount > 0) MetGhamrNavy else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "ابحث بالاسم، التخصص، الهاتف، أو المنطقة...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MetGhamrNavy
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح الكلمة",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.addRecentSearch(searchQuery)
                            }
                            keyboardController?.hide()
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MetGhamrGold,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("directory_search_input")
                )
            }
        }

        // --- 2. Active Quick Filter Chips ---
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Category Filter Chip
            selectedCategory?.let { cat ->
                item {
                    AssistChip(
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("القسم: ${cat.nameAr}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MetGhamrNavy.copy(alpha = 0.1f), labelColor = MetGhamrNavy)
                    )
                }
            }

            // Subcategory Filter Chip
            selectedSubcategory?.let { sub ->
                item {
                    AssistChip(
                        onClick = { viewModel.selectSubcategory(null) },
                        label = { Text("التخصص: ${sub.nameAr}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MetGhamrTeal.copy(alpha = 0.15f), labelColor = MetGhamrTeal)
                    )
                }
            }

            // Area Filter Chip
            if (selectedArea != "الكل") {
                item {
                    AssistChip(
                        onClick = { viewModel.selectArea("الكل") },
                        label = { Text("المنطقة: $selectedArea", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = SurfaceCard, labelColor = TextPrimary)
                    )
                }
            }

            // Open Now Filter Chip
            item {
                FilterChip(
                    selected = openNowOnly,
                    onClick = { viewModel.toggleOpenNow() },
                    label = { Text("مفتوح الآن 🟢", fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Verified Only Filter Chip
            item {
                FilterChip(
                    selected = verifiedOnly,
                    onClick = { viewModel.toggleVerified() },
                    label = { Text("موثق ⚡", fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Clear All Button
            if (activeFiltersCount > 0 || searchQuery.isNotEmpty()) {
                item {
                    TextButton(onClick = { viewModel.clearAllFilters() }) {
                        Text("مسح الكل ❌", color = MetGhamrRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 3. Content Body (Search Suggestions / Recent or Results) ---
        if (searchQuery.isBlank() && activeFiltersCount == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Recent Searches
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MetGhamrNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "عمليات البحث الأخيرة 🕒",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                    Text("مسح السجل", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                recentSearches.forEach { query ->
                                    InputChip(
                                        selected = false,
                                        onClick = {
                                            viewModel.updateSearchQuery(query)
                                            viewModel.addRecentSearch(query)
                                        },
                                        label = { Text(query, fontSize = 12.sp) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeRecentSearch(query) }
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = InputChipDefaults.inputChipColors(containerColor = Color.White),
                                        border = InputChipDefaults.inputChipBorder(selected = false, enabled = true, borderColor = BorderLight),
                                        modifier = Modifier.testTag("recent_search_chip_$query")
                                    )
                                }
                            }
                        }
                    }
                }

                // Popular Searches
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MetGhamrGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الأكثر بحثاً في ميت غمر 🔥",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            popularSearches.forEach { term ->
                                SuggestionChip(
                                    onClick = {
                                        viewModel.updateSearchQuery(term)
                                        viewModel.addRecentSearch(term)
                                    },
                                    label = { Text(term, fontSize = 12.sp, color = TextPrimary) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White),
                                    border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = BorderLight),
                                    modifier = Modifier.testTag("popular_search_chip_$term")
                                )
                            }
                        }
                    }
                }

                // Top Categories Fast Selector
                item {
                    Column {
                        Text(
                            text = "تصفح حسب القسم السريع 🗂️",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories, key = { "search_cat_${it.id}" }) { cat ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                    modifier = Modifier
                                        .clickable { viewModel.selectCategory(cat) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(cat.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cat.nameAr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(${cat.count})", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- 4. Search Results View ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Status Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "نتائج البحث (${businesses.size})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCard)
                            .clickable { showFilterBottomSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "الترتيب: ${currentSortOption.titleAr}",
                            fontSize = 11.sp,
                            color = MetGhamrNavy,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MetGhamrNavy,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (businesses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FindInPage,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لم نجد نتائج متطابقة في ميت غمر",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "لم تجد النشاط الذي تبحث عنه؟ يمكنك إضافته الآن مجاناً وسيظهر للجميع بعد المراجعة.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.requestProtectedAction {
                                            viewModel.navigateTo(ScreenRoute.AddBusiness.route)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("أضف نشاطاً جديداً ➕")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearAllFilters() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ضبط الفلاتر")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_list")
                    ) {
                        items(businesses, key = { it.id }) { business ->
                            BusinessCard(
                                business = business,
                                isFavorite = favIds.contains(business.id),
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.addRecentSearch(searchQuery)
                                    }
                                    viewModel.selectBusiness(business.id)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(business.id, favIds.contains(business.id))
                                },
                                modifier = Modifier.testTag("search_business_item_${business.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Filter & Sort Bottom Sheet ---
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            SearchFilterBottomSheetContent(
                viewModel = viewModel,
                onApply = { showFilterBottomSheet = false }
            )
        }
    }
}

@Composable
fun SearchFilterBottomSheetContent(
    viewModel: DirectoryViewModel,
    onApply: () -> Unit
) {
    val currentSort by viewModel.searchSortOption.collectAsState()
    val currentArea by viewModel.selectedArea.collectAsState()
    val minRating by viewModel.minRating.collectAsState()
    val openNowOnly by viewModel.openNowOnly.collectAsState()
    val verifiedOnly by viewModel.verifiedOnly.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categoriesWithCounts.collectAsState()

    val areasList = remember {
        listOf("الكل", "ميت غمر - المدينة", "صهرجت الكبرى", "تفهنا الأشراف", "أتميدة", "بشلا", "كفر المقدام", "دنديط", "كوم النور")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "تصفية وترتيب النتائج ⚙️",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            )

            TextButton(onClick = { viewModel.clearAllFilters() }) {
                Text("إعادة ضبط", color = MetGhamrRed, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Sort Options ---
        Text(
            text = "ترتيب النتائج حسب:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SearchSortOption.values().forEach { option ->
                val isSelected = currentSort == option
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSortOption(option) },
                    label = { Text(option.titleAr, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetGhamrNavy,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Category Selection ---
        Text(
            text = "القسم المطلوب:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    label = { Text("الكل", fontSize = 11.sp) }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory?.id == cat.id,
                    onClick = { viewModel.selectCategory(cat) },
                    label = { Text(cat.nameAr, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Area Selection ---
        Text(
            text = "المنطقة / القرية في ميت غمر:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(areasList) { area ->
                FilterChip(
                    selected = currentArea == area,
                    onClick = { viewModel.selectArea(area) },
                    label = { Text(area, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Rating Filter ---
        Text(
            text = "الحد الأدنى للتقييم:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0f to "الكل", 3.0f to "3+ ⭐", 4.0f to "4+ ⭐", 4.5f to "4.5+ ⭐").forEach { (rating, label) ->
                FilterChip(
                    selected = minRating == rating,
                    onClick = { viewModel.setMinRating(rating) },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Status Toggles ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مفتوح الآن فقط 🟢", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = openNowOnly,
                onCheckedChange = { viewModel.toggleOpenNow() }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("أنشطة ومحلات موثقة فقط ⚡", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = verifiedOnly,
                onCheckedChange = { viewModel.toggleVerified() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onApply,
            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تطبيق الفلاتر والنتائج", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
