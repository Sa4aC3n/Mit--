package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.ui.components.BusinessCard
import com.example.ui.components.CompactFeaturedCard
import com.example.ui.components.SkeletonBusinessCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DirectoryViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAiAssistant: () -> Unit = {}
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

    val favIds = favorites.map { it.businessId }.toSet()

    val villagesAndAreas = listOf(
        "الكل", "مدينة ميت غمر", "بشلا", "صهرجت الكبرى", "ميت ناجي", "تفهنا الأشراف", "دنديط",
        "أتميدة", "كوم النور", "سنفا", "أوليلة", "دماص", "شارع الحرية", "شارع المحطة"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- 1. Header & Identity ---
        item(key = "home_header") {
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
                                text = "أهلاً بك 👋",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MetGhamrGoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "دليلك لكل ما تحتاجه في ميت غمر",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }

                        // Offline/Cached Badge Indicator
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { viewModel.refreshData() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(OpenGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRefreshing) "جاري التحديث..." else "بيانات محليّة ⚡",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Prominent Main AI Assistant Button ---
                    Surface(
                        onClick = onNavigateToAiAssistant,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(MetGhamrGold, Color.White.copy(alpha = 0.6f))
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("main_ai_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(MetGhamrGold, MetGhamrTeal)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✨", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🤖 مساعد ميت غمر الذكي",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "اسأل بالعامية عن أي مطعم، طبيب، أو خدمة بالدليل",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Surface(
                                color = MetGhamrGold,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "اسأل الآن 💬",
                                    color = MetGhamrNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- 2. Prominent Search Bar ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            viewModel.updateSearchQuery(it)
                        },
                        placeholder = {
                            Text(
                                text = "ابحث عن مطعم، طبيب، صنايعي، مصنع...",
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
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = Color.Gray
                                    )
                                }
                            } else {
                                IconButton(onClick = onNavigateToSearch) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "بحث متقدم",
                                        tint = MetGhamrNavy
                                    )
                                }
                            }
                        },
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
                            .testTag("home_search_input")
                    )
                }
            }
        }

        // --- 3. Quick Categories Section ---
        item(key = "categories_section") {
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
                    TextButton(onClick = onNavigateToCategories) {
                        Text(
                            text = "عرض الكل (${categories.size})",
                            color = MetGhamrTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        CategoryCarouselChip(
                            category = category,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    viewModel.selectCategory(null)
                                } else {
                                    viewModel.selectCategory(category)
                                }
                            }
                        )
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
                            color = MetGhamrNavy.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "موصى بها",
                                color = MetGhamrNavy,
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
                                selectedContainerColor = MetGhamrNavy,
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
                            selectedContainerColor = OpenGreen,
                            selectedLabelColor = Color.White
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
                            selectedContainerColor = VerifiedBlue,
                            selectedLabelColor = Color.White
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
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory != null) "نتائج ${selectedCategory?.nameAr}" else "جميع الأنشطة والخدمات",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${businesses.size} نشاط",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // --- 8. Businesses List or Empty State ---
        if (businesses.isEmpty()) {
            item(key = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "عذراً، لا توجد نتائج مطابقة لمحددات البحث الحالية.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateSearchQuery("")
                                viewModel.selectCategory(null)
                                viewModel.selectArea("الكل")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                        ) {
                            Text("إعادة ضبط الفلاتر", fontSize = 12.sp)
                        }
                    }
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

@Composable
fun CategoryCarouselChip(
    category: CategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MetGhamrNavy else SurfaceCard
        ),
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
                    .background(Color(category.colorHex).copy(alpha = if (isSelected) 0.9f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category.id) {
                        "cat_restaurants" -> Icons.Default.Restaurant
                        "cat_cafes" -> Icons.Default.Coffee
                        "cat_doctors" -> Icons.Default.MedicalServices
                        "cat_hospitals" -> Icons.Default.LocalHospital
                        "cat_radiology" -> Icons.Default.Biotech
                        "cat_technicians" -> Icons.Default.Handyman
                        "cat_factories" -> Icons.Default.Factory
                        "cat_clubs" -> Icons.Default.FitnessCenter
                        "cat_pharmacies" -> Icons.Default.LocalPharmacy
                        "cat_shops" -> Icons.Default.ShoppingBag
                        "cat_automotive" -> Icons.Default.DirectionsCar
                        "cat_education" -> Icons.Default.School
                        "cat_home_events" -> Icons.Default.Event
                        else -> Icons.Default.AccountBalance
                    },
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(category.colorHex),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.nameAr,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 12.sp
                )
            )
        }
    }
}
