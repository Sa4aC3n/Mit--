package com.example.ui.screens.map

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BusinessEntity
import com.example.ui.components.RatingStarsDisplay
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.util.WorkingHoursUtils
import kotlin.math.*

enum class MapFilterCategory(val id: String, val titleAr: String, val icon: String, val color: Color) {
    ALL("all", "الكل", "🌟", MetGhamrNavy),
    HOSPITALS("cat_hospitals", "مستشفيات وطوارئ", "🏥", MetGhamrRed),
    MEDICAL("medical_group", "مراكز طبية وعيادات", "🩺", MetGhamrTeal),
    PHARMACIES("cat_pharmacies", "صيدليات 24 س", "💊", Color(0xFF10B981)),
    GOVERNMENT("cat_government", "مصالح حكومية", "🏛️", Color(0xFF6366F1)),
    BANKS("cat_banks", "بنوك وصرافة", "🏦", Color(0xFF0284C7)),
    EDUCATION("education_group", "تعليم وجامعات", "🎓", Color(0xFFF59E0B)),
    SERVICES("services_group", "ورش ومصانع", "🛠️", Color(0xFF8B5CF6)),
    RESTAURANTS("food_group", "مطاعم وكافيهات", "🍽️", Color(0xFFEC4899))
}

enum class MapViewMode {
    MAP_CANVAS,
    VITAL_CENTERS_LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMapScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val businesses by viewModel.allActiveBusinesses.collectAsState()

    var selectedCategory by remember { mutableStateOf(MapFilterCategory.ALL) }
    var viewMode by remember { mutableStateOf(MapViewMode.MAP_CANVAS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedBusinessId by remember { mutableStateOf<String?>(null) }
    var clusterBusinessesToDisplay by remember { mutableStateOf<List<BusinessEntity>?>(null) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showGoogleMapsSearchDialog by remember { mutableStateOf(false) }

    // Map Zoom & Pan State (Default 1.35f focused clearly on Met Ghamr center)
    var zoomScale by remember { mutableFloatStateOf(1.35f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Filter businesses based on search & category
    val filteredBusinesses = remember(businesses, selectedCategory, searchQuery) {
        businesses.filter { b ->
            // Category match
            val matchesCategory = when (selectedCategory) {
                MapFilterCategory.ALL -> true
                MapFilterCategory.HOSPITALS -> b.categoryId == "cat_hospitals"
                MapFilterCategory.MEDICAL -> b.categoryId in listOf("cat_doctors", "cat_medical_centers", "cat_radiology")
                MapFilterCategory.PHARMACIES -> b.categoryId == "cat_pharmacies"
                MapFilterCategory.GOVERNMENT -> b.categoryId in listOf("cat_government", "cat_syndicates", "cat_charities")
                MapFilterCategory.BANKS -> b.categoryId == "cat_banks"
                MapFilterCategory.EDUCATION -> b.categoryId in listOf("cat_universities", "cat_schools", "cat_education", "cat_libraries")
                MapFilterCategory.SERVICES -> b.categoryId in listOf("cat_factories", "cat_technicians", "cat_companies")
                MapFilterCategory.RESTAURANTS -> b.categoryId in listOf("cat_restaurants", "cat_cafes")
            }

            // Search query match
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                b.name.contains(searchQuery.trim(), ignoreCase = true) ||
                        b.categoryName.contains(searchQuery.trim(), ignoreCase = true) ||
                        b.specialty.contains(searchQuery.trim(), ignoreCase = true) ||
                        b.area.contains(searchQuery.trim(), ignoreCase = true) ||
                        b.address.contains(searchQuery.trim(), ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }
    }

    val selectedBusiness = remember(filteredBusinesses, selectedBusinessId) {
        businesses.find { it.id == selectedBusinessId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "خريطة الخدمات والمستشفيات",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MetGhamrGold,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ميت غمر 🗺️",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MetGhamrNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${filteredBusinesses.size} موقع حيوي معتمد",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("map_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Emergency Quick Numbers
                    IconButton(
                        onClick = { showEmergencyDialog = true },
                        modifier = Modifier.testTag("map_emergency_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalHospital,
                            contentDescription = "أرقام الطوارئ",
                            tint = MetGhamrGold
                        )
                    }

                    // Direct Google Maps Search
                    IconButton(
                        onClick = { showGoogleMapsSearchDialog = true },
                        modifier = Modifier.testTag("map_google_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "بحث خرائط Google",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Control Bar: Search, Category Filters, Mode Switch
                Surface(
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        // Search bar & Mode Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("ابحث عن مستشفى، صيدلية، بنك، جهة...", fontSize = 12.sp, color = TextSecondary)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Search, contentDescription = "بحث", tint = MetGhamrNavy, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetGhamrTeal,
                                    unfocusedBorderColor = BorderLight,
                                    focusedContainerColor = SurfaceLight,
                                    unfocusedContainerColor = SurfaceLight
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("map_search_field")
                            )

                            // View Mode Toggle (Map Canvas vs List)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (viewMode == MapViewMode.MAP_CANVAS) MetGhamrNavy else SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                                modifier = Modifier
                                    .clickable {
                                        viewMode = if (viewMode == MapViewMode.MAP_CANVAS) MapViewMode.VITAL_CENTERS_LIST else MapViewMode.MAP_CANVAS
                                    }
                                    .testTag("map_toggle_view_mode")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (viewMode == MapViewMode.MAP_CANVAS) Icons.Default.FormatListBulleted else Icons.Default.Map,
                                        contentDescription = "تبديل العرض",
                                        tint = if (viewMode == MapViewMode.MAP_CANVAS) Color.White else MetGhamrNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (viewMode == MapViewMode.MAP_CANVAS) "قائمة" else "خريطة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewMode == MapViewMode.MAP_CANVAS) Color.White else MetGhamrNavy
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Chips Filter
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(MapFilterCategory.entries) { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) cat.color else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) cat.color else BorderLight
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            selectedCategory = cat
                                        }
                                        .testTag("map_filter_${cat.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(cat.icon, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = cat.titleAr,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (viewMode == MapViewMode.MAP_CANVAS) {
                        // 1. Interactive High-Performance Canvas Map
                        MetGhamrMapCanvas(
                            businesses = filteredBusinesses,
                            selectedBusinessId = selectedBusinessId,
                            zoomScale = zoomScale,
                            panOffsetX = panOffsetX,
                            panOffsetY = panOffsetY,
                            onTransform = { panX, panY, zoom ->
                                panOffsetX += panX
                                panOffsetY += panY
                                zoomScale = (zoomScale * zoom).coerceIn(0.6f, 4.2f)
                            },
                            onSelectBusiness = { id ->
                                selectedBusinessId = if (selectedBusinessId == id) null else id
                            },
                            onSelectCluster = { clusterList ->
                                clusterBusinessesToDisplay = clusterList
                            },
                            onZoomIntoPoint = { cx, cy ->
                                panOffsetX -= (cx - (panOffsetX + 0f)) * 0.35f
                                panOffsetY -= (cy - (panOffsetY + 0f)) * 0.35f
                                zoomScale = (zoomScale * 1.5f).coerceAtMost(4.0f)
                            }
                        )

                        // Map Control Floating Buttons (Zoom in, Zoom out, Reset view, Direct Google Maps)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Zoom In
                            FloatingActionButton(
                                onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(4.2f) },
                                containerColor = Color.White,
                                contentColor = MetGhamrNavy,
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(3.dp, CircleShape)
                                    .testTag("map_zoom_in")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "تكبير", modifier = Modifier.size(20.dp))
                            }

                            // Zoom Out
                            FloatingActionButton(
                                onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f) },
                                containerColor = Color.White,
                                contentColor = MetGhamrNavy,
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(3.dp, CircleShape)
                                    .testTag("map_zoom_out")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "تصغير", modifier = Modifier.size(20.dp))
                            }

                            // Reset View / Recenter to Met Ghamr Center
                            FloatingActionButton(
                                onClick = {
                                    zoomScale = 1.35f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                },
                                containerColor = MetGhamrTeal,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(3.dp, CircleShape)
                                    .testTag("map_recenter")
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "إعادة ضبط الخريطة", modifier = Modifier.size(20.dp))
                            }

                            // Quick Google Maps Search Button
                            FloatingActionButton(
                                onClick = {
                                    showGoogleMapsSearchDialog = true
                                },
                                containerColor = Color.White,
                                contentColor = Color(0xFF0284C7),
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(3.dp, CircleShape)
                                    .testTag("map_google_shortcuts_btn")
                            ) {
                                Icon(Icons.Default.Public, contentDescription = "خرائط Google", modifier = Modifier.size(20.dp))
                            }
                        }

                        // Map Legend Watermark Badge (Top Left)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MetGhamrNavy.copy(alpha = 0.90f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MetGhamrGold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نيل ومراكز ميت غمر",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // 2. Vital Centers List View
                        VitalCentersListView(
                            businesses = filteredBusinesses,
                            onSelectBusiness = { b ->
                                viewModel.selectBusiness(b.id)
                            },
                            onOpenGoogleMaps = { b ->
                                openGoogleMapsNavigation(context, b)
                            },
                            onCallBusiness = { phone ->
                                dialPhoneNumber(context, phone)
                            }
                        )
                    }
                }
            }

            // Bottom Floating Sheet: Selected Business Detail Card
            AnimatedVisibility(
                visible = selectedBusiness != null && viewMode == MapViewMode.MAP_CANVAS,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedBusiness?.let { b ->
                    SelectedPlaceBottomSheetCard(
                        business = b,
                        onClose = { selectedBusinessId = null },
                        onOpenGoogleMaps = { openGoogleMapsNavigation(context, b) },
                        onViewFullDetails = {
                            viewModel.selectBusiness(b.id)
                        },
                        onCall = { dialPhoneNumber(context, b.phone) }
                    )
                }
            }
        }
    }

    // Emergency Quick Contact Dialog
    if (showEmergencyDialog) {
        EmergencyNumbersDialog(
            onDismiss = { showEmergencyDialog = false },
            onCall = { phone -> dialPhoneNumber(context, phone) }
        )
    }

    // Google Maps Direct Search Shortcuts Dialog
    if (showGoogleMapsSearchDialog) {
        GoogleMapsShortcutsDialog(
            onDismiss = { showGoogleMapsSearchDialog = false },
            onLaunchSearch = { query ->
                launchGoogleMapsQuery(context, query)
                showGoogleMapsSearchDialog = false
            }
        )
    }

    // Cluster Items Selection Dialog
    clusterBusinessesToDisplay?.let { clusterList ->
        ClusterItemsDialog(
            businesses = clusterList,
            onDismiss = { clusterBusinessesToDisplay = null },
            onSelectBusiness = { b ->
                selectedBusinessId = b.id
                clusterBusinessesToDisplay = null
            },
            onOpenGoogleMaps = { b ->
                openGoogleMapsNavigation(context, b)
            },
            onCallBusiness = { phone ->
                dialPhoneNumber(context, phone)
            }
        )
    }
}

/**
 * Pre-projected GPS Marker data class for fast rendering
 */
private data class ProjectedMarker(
    val business: BusinessEntity,
    val nx: Float,
    val ny: Float,
    val pinColor: Color
)

/**
 * Clustered Map Item on Screen
 */
private data class MapCluster(
    val id: String,
    val screenX: Float,
    val screenY: Float,
    val businesses: List<BusinessEntity>,
    val primaryColor: Color,
    val isSelected: Boolean
)

/**
 * Custom High-Performance Vector Map for Met Ghamr
 * - Zero continuous redraw loop: 0% CPU when idle, buttery 60fps pan/zoom
 * - Intelligent dynamic clustering for dense downtown areas
 * - Authentic cartography: Nile river with Corniche, bridges, major arterial roads
 * - Fast touch detection and viewport culling
 */
@Composable
fun MetGhamrMapCanvas(
    businesses: List<BusinessEntity>,
    selectedBusinessId: String?,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onTransform: (panX: Float, panY: Float, zoom: Float) -> Unit,
    onSelectBusiness: (String) -> Unit,
    onSelectCluster: (List<BusinessEntity>) -> Unit,
    onZoomIntoPoint: (Float, Float) -> Unit
) {
    // Focused geographic bounds for Met Ghamr & immediate district
    // Center: Lat 30.7185, Lng 31.2568
    val minLat = 30.6800
    val maxLat = 30.7600
    val minLng = 31.2300
    val maxLng = 31.2850

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(pan.x, pan.y, zoom)
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // 1. Precalculate normalized coordinates once when business list changes
        val projectedMarkers = remember(businesses) {
            businesses.map { b ->
                val lat = if (b.latitude != 0.0) b.latitude else 30.7183
                val lng = if (b.longitude != 0.0) b.longitude else 31.2568
                val nx = ((lng - minLng) / (maxLng - minLng)).toFloat().coerceIn(0.02f, 0.98f)
                val ny = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()).coerceIn(0.02f, 0.98f)
                val pinColor = when (b.categoryId) {
                    "cat_hospitals" -> MetGhamrRed
                    "cat_doctors", "cat_medical_centers", "cat_radiology" -> MetGhamrTeal
                    "cat_pharmacies" -> Color(0xFF10B981)
                    "cat_government", "cat_syndicates" -> Color(0xFF6366F1)
                    "cat_banks" -> Color(0xFF0284C7)
                    "cat_universities", "cat_schools" -> Color(0xFFF59E0B)
                    "cat_restaurants", "cat_cafes" -> Color(0xFFEC4899)
                    else -> MetGhamrNavy
                }
                ProjectedMarker(b, nx, ny, pinColor)
            }
        }

        // 2. Efficient spatial clustering based on current zoom scale
        val clusters = remember(projectedMarkers, zoomScale, panOffsetX, panOffsetY, selectedBusinessId, widthPx, heightPx) {
            val result = mutableListOf<MapCluster>()
            val clusterRadiusPx = if (zoomScale < 1.3f) 44f else if (zoomScale < 2.2f) 32f else 20f

            val screenPoints = projectedMarkers.map { pm ->
                val sx = (pm.nx * widthPx * zoomScale) + panOffsetX + (widthPx * (1f - zoomScale) / 2f)
                val sy = (pm.ny * heightPx * zoomScale) + panOffsetY + (heightPx * (1f - zoomScale) / 2f)
                Triple(pm, sx, sy)
            }

            val visited = BooleanArray(screenPoints.size)

            for (i in screenPoints.indices) {
                if (visited[i]) continue
                val (pm1, sx1, sy1) = screenPoints[i]

                // Fast Viewport Culling: Skip offscreen points
                if (sx1 < -80f || sx1 > widthPx + 80f || sy1 < -80f || sy1 > heightPx + 80f) {
                    visited[i] = true
                    continue
                }

                val groupedBiz = mutableListOf(pm1.business)
                var sumX = sx1
                var sumY = sy1
                var isSelected = pm1.business.id == selectedBusinessId

                for (j in i + 1 until screenPoints.size) {
                    if (visited[j]) continue
                    val (pm2, sx2, sy2) = screenPoints[j]
                    val dist = hypot(sx1 - sx2, sy1 - sy2)
                    if (dist < clusterRadiusPx) {
                        visited[j] = true
                        groupedBiz.add(pm2.business)
                        sumX += sx2
                        sumY += sy2
                        if (pm2.business.id == selectedBusinessId) isSelected = true
                    }
                }
                visited[i] = true

                val count = groupedBiz.size
                result.add(
                    MapCluster(
                        id = pm1.business.id,
                        screenX = sumX / count,
                        screenY = sumY / count,
                        businesses = groupedBiz,
                        primaryColor = pm1.pinColor,
                        isSelected = isSelected
                    )
                )
            }
            result
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(clusters, zoomScale) {
                    detectTapGestures { tapOffset ->
                        var closest: MapCluster? = null
                        var minDist = 48f
                        for (c in clusters) {
                            val d = hypot(tapOffset.x - c.screenX, tapOffset.y - c.screenY)
                            if (d < minDist) {
                                minDist = d
                                closest = c
                            }
                        }

                        closest?.let { c ->
                            if (c.businesses.size == 1) {
                                onSelectBusiness(c.businesses.first().id)
                            } else {
                                if (zoomScale < 2.4f) {
                                    onZoomIntoPoint(c.screenX, c.screenY)
                                } else {
                                    onSelectCluster(c.businesses)
                                }
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // --- 1. Draw Geography & Urban Zones ---
            drawMapGeography(w, h, zoomScale, panOffsetX, panOffsetY)

            // --- 2. Draw Nile River (Damietta Branch) & Corniche ---
            drawNileRiver(w, h, zoomScale, panOffsetX, panOffsetY)

            // --- 3. Draw Road Network & Bridges ---
            drawMajorRoads(w, h, zoomScale, panOffsetX, panOffsetY)

            // --- 4. Draw Districts & Villages ---
            drawAreaLabels(w, h, zoomScale, panOffsetX, panOffsetY)

            // --- 5. Draw High-Performance Markers & Clusters ---
            val countTextPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 26f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val pinLabelPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 22f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            for (cluster in clusters) {
                val cx = cluster.screenX
                val cy = cluster.screenY
                val count = cluster.businesses.size

                if (count > 1) {
                    // Clustered Hub Marker
                    val clusterRadius = if (count > 20) 23f else if (count > 5) 19f else 16f

                    // Drop Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.22f),
                        radius = clusterRadius + 3f,
                        center = Offset(cx, cy + 2f)
                    )

                    // Outer White Ring
                    drawCircle(
                        color = Color.White,
                        radius = clusterRadius + 2f,
                        center = Offset(cx, cy)
                    )

                    // Center Hub Circle
                    drawCircle(
                        color = if (cluster.isSelected) MetGhamrGold else cluster.primaryColor,
                        radius = clusterRadius,
                        center = Offset(cx, cy)
                    )

                    // Count Badge
                    val countStr = if (count > 99) "99+" else count.toString()
                    countTextPaint.textSize = if (count > 99) 18f else 22f
                    drawContext.canvas.nativeCanvas.drawText(
                        countStr,
                        cx,
                        cy + (countTextPaint.textSize / 3f),
                        countTextPaint
                    )
                } else {
                    // Single Pin
                    val b = cluster.businesses.first()

                    // Selection Halo
                    if (cluster.isSelected) {
                        drawCircle(
                            color = MetGhamrGold.copy(alpha = 0.35f),
                            radius = 24f,
                            center = Offset(cx, cy)
                        )
                    }

                    // Shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.20f),
                        radius = 11f,
                        center = Offset(cx, cy + 2f)
                    )

                    // White Rim
                    drawCircle(
                        color = Color.White,
                        radius = 10f,
                        center = Offset(cx, cy)
                    )

                    // Colored Core
                    drawCircle(
                        color = cluster.primaryColor,
                        radius = 7.5f,
                        center = Offset(cx, cy)
                    )

                    // Pin Eye Dot
                    drawCircle(
                        color = if (cluster.isSelected) MetGhamrGold else Color.White,
                        radius = 3f,
                        center = Offset(cx, cy)
                    )

                    // Title label when selected or zoomed in
                    if (cluster.isSelected || zoomScale > 2.2f) {
                        val namePreview = if (b.name.length > 14) b.name.take(13) + "…" else b.name
                        drawContext.canvas.nativeCanvas.drawText(
                            namePreview,
                            cx,
                            cy + 25f,
                            pinLabelPaint
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to view list of places within a cluster
 */
@Composable
fun ClusterItemsDialog(
    businesses: List<BusinessEntity>,
    onDismiss: () -> Unit,
    onSelectBusiness: (BusinessEntity) -> Unit,
    onOpenGoogleMaps: (BusinessEntity) -> Unit,
    onCallBusiness: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MetGhamrTeal.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${businesses.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MetGhamrTeal
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مواقع متقاربة (${businesses.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MetGhamrNavy
                            )
                            Text(
                                text = "انقر على أي موقع لعرضه على الخريطة",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(businesses) { b ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectBusiness(b)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MetGhamrNavy.copy(alpha = 0.08f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "📍", fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MetGhamrNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (b.specialty.isNotBlank()) b.specialty else b.categoryName,
                                        fontSize = 11.sp,
                                        color = MetGhamrTeal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (b.area.isNotBlank()) {
                                        Text(
                                            text = b.area,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (b.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = { onCallBusiness(b.phone) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "اتصال",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(17.dp)
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

// Helpers for screen projection
private inline fun projectX(nx: Float, w: Float, zoomScale: Float, panOffsetX: Float): Float {
    return (nx * w * zoomScale) + panOffsetX + (w * (1f - zoomScale) / 2f)
}

private inline fun projectY(ny: Float, h: Float, zoomScale: Float, panOffsetY: Float): Float {
    return (ny * h * zoomScale) + panOffsetY + (h * (1f - zoomScale) / 2f)
}

/**
 * Draw base geography: agricultural green belts & city footprints
 */
private fun DrawScope.drawMapGeography(
    w: Float,
    h: Float,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    // 1. Agricultural Green Land (Countryside)
    val agriColor = Color(0xFFF0FDF4)
    drawRect(
        color = agriColor,
        topLeft = Offset(0f, 0f),
        size = Size(w, h)
    )

    // 2. Met Ghamr Urban Center Footprint
    val downtownX = projectX(0.48f, w, zoomScale, panOffsetX)
    val downtownY = projectY(0.52f, h, zoomScale, panOffsetY)
    drawCircle(
        color = Color(0xFFE2E8F0),
        radius = 80f * zoomScale,
        center = Offset(downtownX, downtownY)
    )

    // 3. Zifta Urban Footprint (West Bank of Nile)
    val ziftaX = projectX(0.24f, w, zoomScale, panOffsetX)
    val ziftaY = projectY(0.52f, h, zoomScale, panOffsetY)
    drawCircle(
        color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
        radius = 60f * zoomScale,
        center = Offset(ziftaX, ziftaY)
    )

    // 4. Nile Island (جزيرة في النيل)
    val islandX = projectX(0.36f, w, zoomScale, panOffsetX)
    val islandY = projectY(0.58f, h, zoomScale, panOffsetY)
    drawOval(
        color = Color(0xFF86EFAC),
        topLeft = Offset(islandX - 8f * zoomScale, islandY - 20f * zoomScale),
        size = Size(16f * zoomScale, 40f * zoomScale)
    )
}

/**
 * Draw Nile River (Damietta Branch) with authentic curves, banks, and Corniche promenade
 */
private fun DrawScope.drawNileRiver(
    w: Float,
    h: Float,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    // River path points from South to North
    val p0 = Offset(projectX(0.34f, w, zoomScale, panOffsetX), projectY(1.05f, h, zoomScale, panOffsetY))
    val p1 = Offset(projectX(0.36f, w, zoomScale, panOffsetX), projectY(0.70f, h, zoomScale, panOffsetY))
    val p2 = Offset(projectX(0.37f, w, zoomScale, panOffsetX), projectY(0.52f, h, zoomScale, panOffsetY))
    val p3 = Offset(projectX(0.35f, w, zoomScale, panOffsetX), projectY(0.32f, h, zoomScale, panOffsetY))
    val p4 = Offset(projectX(0.32f, w, zoomScale, panOffsetX), projectY(-0.05f, h, zoomScale, panOffsetY))

    val riverPath = Path().apply {
        moveTo(p0.x, p0.y)
        quadraticTo(p1.x, p1.y, p2.x, p2.y)
        quadraticTo(p3.x, p3.y, p4.x, p4.y)
    }

    // Outer River Banks (Light Sky Blue)
    drawPath(
        path = riverPath,
        color = Color(0xFFBAE6FD),
        style = Stroke(width = 30f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Main River Stream (Sky Blue)
    drawPath(
        path = riverPath,
        color = Color(0xFF38BDF8),
        style = Stroke(width = 22f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Deep Stream Center (Deep Cyan)
    drawPath(
        path = riverPath,
        color = Color(0xFF0284C7),
        style = Stroke(width = 6f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Corniche Promenade along East Bank
    val cornicheP0 = Offset(projectX(0.365f, w, zoomScale, panOffsetX), projectY(0.75f, h, zoomScale, panOffsetY))
    val cornicheP1 = Offset(projectX(0.395f, w, zoomScale, panOffsetX), projectY(0.52f, h, zoomScale, panOffsetY))
    val cornicheP2 = Offset(projectX(0.375f, w, zoomScale, panOffsetX), projectY(0.30f, h, zoomScale, panOffsetY))

    val cornichePath = Path().apply {
        moveTo(cornicheP0.x, cornicheP0.y)
        quadraticTo(cornicheP1.x, cornicheP1.y, cornicheP2.x, cornicheP2.y)
    }

    drawPath(
        path = cornichePath,
        color = Color(0xFF10B981).copy(alpha = 0.85f),
        style = Stroke(width = 3.5f * zoomScale, cap = StrokeCap.Round)
    )
}

/**
 * Draw Road Network & Major Bridges connecting Met Ghamr and Zifta
 */
private fun DrawScope.drawMajorRoads(
    w: Float,
    h: Float,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    // 1. Banha - Mansoura Highway (East bypass)
    val hwyStart = Offset(projectX(0.58f, w, zoomScale, panOffsetX), projectY(-0.05f, h, zoomScale, panOffsetY))
    val hwyEnd = Offset(projectX(0.58f, w, zoomScale, panOffsetX), projectY(1.05f, h, zoomScale, panOffsetY))

    // Highway base
    drawLine(
        color = Color(0xFF94A3B8),
        start = hwyStart,
        end = hwyEnd,
        strokeWidth = 11f * zoomScale,
        cap = StrokeCap.Round
    )
    // Highway pavement
    drawLine(
        color = Color(0xFFF8FAFC),
        start = hwyStart,
        end = hwyEnd,
        strokeWidth = 7f * zoomScale,
        cap = StrokeCap.Round
    )
    // Center divider
    drawLine(
        color = Color(0xFFF59E0B),
        start = hwyStart,
        end = hwyEnd,
        strokeWidth = 1.8f * zoomScale,
        cap = StrokeCap.Round
    )

    // 2. Zagazig Road (Heading South-East)
    val zgStart = Offset(projectX(0.52f, w, zoomScale, panOffsetX), projectY(0.55f, h, zoomScale, panOffsetY))
    val zgEnd = Offset(projectX(0.92f, w, zoomScale, panOffsetX), projectY(0.88f, h, zoomScale, panOffsetY))
    drawLine(
        color = Color(0xFFCBD5E1),
        start = zgStart,
        end = zgEnd,
        strokeWidth = 7f * zoomScale,
        cap = StrokeCap.Round
    )

    // 3. Downtown Commercial Spine (Sharia Port Said)
    val psStart = Offset(projectX(0.47f, w, zoomScale, panOffsetX), projectY(0.36f, h, zoomScale, panOffsetY))
    val psEnd = Offset(projectX(0.48f, w, zoomScale, panOffsetX), projectY(0.68f, h, zoomScale, panOffsetY))
    drawLine(
        color = Color.White,
        start = psStart,
        end = psEnd,
        strokeWidth = 8f * zoomScale,
        cap = StrokeCap.Round
    )

    // 4. Central Boulevard (Sharia El-Horreya & Station St)
    val hrStart = Offset(projectX(0.38f, w, zoomScale, panOffsetX), projectY(0.52f, h, zoomScale, panOffsetY))
    val hrEnd = Offset(projectX(0.58f, w, zoomScale, panOffsetX), projectY(0.52f, h, zoomScale, panOffsetY))
    drawLine(
        color = Color.White,
        start = hrStart,
        end = hrEnd,
        strokeWidth = 8f * zoomScale,
        cap = StrokeCap.Round
    )

    // 5. Historic Steel Bridge: Met Ghamr - Zifta Bridge (كوبري ميت غمر - زفتى الأثري)
    val br1Start = Offset(projectX(0.24f, w, zoomScale, panOffsetX), projectY(0.52f, h, zoomScale, panOffsetY))
    val br1End = Offset(projectX(0.39f, w, zoomScale, panOffsetX), projectY(0.52f, h, zoomScale, panOffsetY))
    // Steel structure
    drawLine(
        color = Color(0xFF1E293B),
        start = br1Start,
        end = br1End,
        strokeWidth = 10f * zoomScale,
        cap = StrokeCap.Square
    )
    // Bridge deck
    drawLine(
        color = Color(0xFFF1F5F9),
        start = br1Start,
        end = br1End,
        strokeWidth = 5f * zoomScale,
        cap = StrokeCap.Square
    )

    // 6. Elevated Highway Nile Bridge (كوبري النيل العلوي)
    val br2Start = Offset(projectX(0.22f, w, zoomScale, panOffsetX), projectY(0.36f, h, zoomScale, panOffsetY))
    val br2End = Offset(projectX(0.40f, w, zoomScale, panOffsetX), projectY(0.36f, h, zoomScale, panOffsetY))
    drawLine(
        color = Color(0xFF0284C7),
        start = br2Start,
        end = br2End,
        strokeWidth = 8f * zoomScale,
        cap = StrokeCap.Square
    )
    drawLine(
        color = Color.White,
        start = br2Start,
        end = br2End,
        strokeWidth = 4f * zoomScale,
        cap = StrokeCap.Square
    )
}

/**
 * Draw District and Landmark Labels using clean Native Canvas Typography
 */
private fun DrawScope.drawAreaLabels(
    w: Float,
    h: Float,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float
) {
    val districtPaint = Paint().apply {
        color = android.graphics.Color.rgb(30, 41, 59)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val landmarkPaint = Paint().apply {
        color = android.graphics.Color.rgb(13, 148, 136)
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val waterPaint = Paint().apply {
        color = android.graphics.Color.rgb(2, 132, 199)
        textSize = 19f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val villagePaint = Paint().apply {
        color = android.graphics.Color.rgb(100, 116, 139)
        textSize = 19f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    // River Label
    val nileX = projectX(0.36f, w, zoomScale, panOffsetX)
    val nileY = projectY(0.44f, h, zoomScale, panOffsetY)
    drawContext.canvas.nativeCanvas.drawText("نهر النيل (فرع دمياط)", nileX, nileY, waterPaint)

    // Districts
    val downtownX = projectX(0.48f, w, zoomScale, panOffsetX)
    val downtownY = projectY(0.50f, h, zoomScale, panOffsetY)
    drawContext.canvas.nativeCanvas.drawText("وسط البلد", downtownX, downtownY, districtPaint)

    val stationX = projectX(0.44f, w, zoomScale, panOffsetX)
    val stationY = projectY(0.55f, h, zoomScale, panOffsetY)
    drawContext.canvas.nativeCanvas.drawText("ميدان المحطة", stationX, stationY, landmarkPaint)

    val ziftaX = projectX(0.24f, w, zoomScale, panOffsetX)
    val ziftaY = projectY(0.55f, h, zoomScale, panOffsetY)
    drawContext.canvas.nativeCanvas.drawText("مدينة زفتى", ziftaX, ziftaY, districtPaint)

    val cornicheX = projectX(0.40f, w, zoomScale, panOffsetX)
    val cornicheY = projectY(0.41f, h, zoomScale, panOffsetY)
    drawContext.canvas.nativeCanvas.drawText("كورنيش النيل", cornicheX, cornicheY, landmarkPaint)

    // Outlying Towns & Villages (visible on map canvas)
    val villages = listOf(
        Pair("تفهنا الأشراف", Offset(0.75f, 0.18f)),
        Pair("صهرجت الكبرى", Offset(0.55f, 0.82f)),
        Pair("دنديط", Offset(0.76f, 0.45f)),
        Pair("سنفا", Offset(0.30f, 0.25f)),
        Pair("بشلا", Offset(0.52f, 0.26f)),
        Pair("أتميدة", Offset(0.78f, 0.65f)),
        Pair("كوم النور", Offset(0.70f, 0.78f))
    )

    villages.forEach { (name, ratio) ->
        val vx = projectX(ratio.x, w, zoomScale, panOffsetX)
        val vy = projectY(ratio.y, h, zoomScale, panOffsetY)

        // Dot anchor
        drawCircle(
            color = MetGhamrNavy.copy(alpha = 0.35f),
            radius = 3.5f * zoomScale,
            center = Offset(vx, vy)
        )

        drawContext.canvas.nativeCanvas.drawText(name, vx, vy + 18f, villagePaint)
    }
}

/**
 * Card Sheet displayed at bottom when a marker is clicked on the interactive canvas
 */
@Composable
fun SelectedPlaceBottomSheetCard(
    business: BusinessEntity,
    onClose: () -> Unit,
    onOpenGoogleMaps: () -> Unit,
    onViewFullDetails: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_selected_place_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Title, Category Badge, and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when (business.categoryId) {
                                "cat_hospitals" -> MetGhamrRed.copy(alpha = 0.15f)
                                "cat_pharmacies" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                "cat_banks" -> Color(0xFF0284C7).copy(alpha = 0.15f)
                                else -> MetGhamrNavy.copy(alpha = 0.12f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = business.categoryName,
                                color = when (business.categoryId) {
                                    "cat_hospitals" -> MetGhamrRed
                                    "cat_pharmacies" -> Color(0xFF10B981)
                                    "cat_banks" -> Color(0xFF0284C7)
                                    else -> MetGhamrNavy
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        val statusInfo = WorkingHoursUtils.getStatusInfo(business.workingHours)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = statusInfo.containerColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusInfo.dotColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusInfo.label,
                                    color = statusInfo.contentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = business.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = TextSecondary
                    )
                }
            }

            // Specialty & Address
            Text(
                text = business.specialty,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MetGhamrTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${business.area} — ${business.address}",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Rating & Working Hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStarsDisplay(rating = business.ratingAverage)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${business.ratingCount} تقييم)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "⏰ ${business.workingHours}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = BorderLight)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Google Maps Navigation Button
                Button(
                    onClick = onOpenGoogleMaps,
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("map_open_google_maps_btn")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = "الملاحة", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فتح في خرائط Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Direct Call Button
                OutlinedButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MetGhamrTeal),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("map_quick_call_btn")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "اتصال", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // 3. View Full Detail
                OutlinedButton(
                    onClick = onViewFullDetails,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("map_full_detail_btn")
                ) {
                    Icon(Icons.Default.Info, contentDescription = "التفاصيل", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * List View of Vital Centers (Hospitals, Emergency, Banks, Government, etc.)
 */
@Composable
fun VitalCentersListView(
    businesses: List<BusinessEntity>,
    onSelectBusiness: (BusinessEntity) -> Unit,
    onOpenGoogleMaps: (BusinessEntity) -> Unit,
    onCallBusiness: (String) -> Unit
) {
    if (businesses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📍", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "لا توجد مواقع تطابق بحثك حالياً",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "جرب اختيار تصنيف آخر أو مسح عبارة البحث",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(businesses, key = { it.id }) { b ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectBusiness(b) }
                    .testTag("map_list_item_${b.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = when (b.categoryId) {
                                        "cat_hospitals" -> MetGhamrRed.copy(alpha = 0.12f)
                                        "cat_pharmacies" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                        "cat_banks" -> Color(0xFF0284C7).copy(alpha = 0.12f)
                                        else -> MetGhamrNavy.copy(alpha = 0.08f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = b.categoryName,
                                        color = when (b.categoryId) {
                                            "cat_hospitals" -> MetGhamrRed
                                            "cat_pharmacies" -> Color(0xFF10B981)
                                            "cat_banks" -> Color(0xFF0284C7)
                                            else -> MetGhamrNavy
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                val statusInfo = WorkingHoursUtils.getStatusInfo(b.workingHours)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = statusInfo.containerColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(statusInfo.dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = statusInfo.label,
                                            fontSize = 9.sp,
                                            color = statusInfo.contentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = b.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = b.specialty,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Rating badge
                        Surface(
                            color = MetGhamrGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MetGhamrGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", b.ratingAverage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MetGhamrNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MetGhamrTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${b.area} - ${b.address}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons: Navigation & Call
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenGoogleMaps(b) },
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = "خرائط Google", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خريطة Google 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onCallBusiness(b.phone) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "اتصال", modifier = Modifier.size(14.dp), tint = MetGhamrTeal)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اتصال مباشر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetGhamrTeal)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Emergency Quick Numbers Dialog (Ambulance, Police, Fire, Utilities)
 */
@Composable
fun EmergencyNumbersDialog(
    onDismiss: () -> Unit,
    onCall: (String) -> Unit
) {
    val emergencyNumbers = listOf(
        Triple("الإسعاف المصري بميت غمر", "123", "🚑"),
        Triple("شرطة النجدة وقسم ميت غمر", "122", "🚓"),
        Triple("الحماية المدنية والمطافئ", "180", "🚒"),
        Triple("طوارئ مستشفى ميت غمر العام", "0506900100", "🏥"),
        Triple("طوارئ الكهرباء بميت غمر", "121", "⚡"),
        Triple("طوارئ مياه الشرب والصرف", "125", "💧"),
        Triple("طوارئ الغاز الطبيعي", "129", "🔥")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أرقام الطوارئ السريعة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrRed
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                Text(
                    text = "اتصال مباشر بأجهزة الطوارئ والنجدة في مدينة ومركز ميت غمر",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(8.dp))

                emergencyNumbers.forEach { (name, number, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCall(number) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("الرقم: $number", fontSize = 11.sp, color = MetGhamrNavy, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            color = MetGhamrRed.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "اتصال",
                                    tint = MetGhamrRed,
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

/**
 * Google Maps Direct Search Shortcuts Dialog
 */
@Composable
fun GoogleMapsShortcutsDialog(
    onDismiss: () -> Unit,
    onLaunchSearch: (String) -> Unit
) {
    val shortcuts = listOf(
        Pair("مستشفيات ومراكز طوارئ ميت غمر", "مستشفيات ميت غمر"),
        Pair("صيدليات 24 ساعة ميت غمر", "صيدليات ميت غمر"),
        Pair("بنوك وماكينات صراف آلي ATM", "بنوك ميت غمر"),
        Pair("معامل تحاليل ومراكز أشعة", "معامل تحاليل ميت غمر"),
        Pair("مصالح حكومية ومكاتب بريد", "مكاتب بريد ميت غمر"),
        Pair("مطاعم وكافيهات قريبة", "مطاعم ميت غمر"),
        Pair("محطات وقود وغاز طبيعي", "محطات بنزين ميت غمر")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = MetGhamrNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بحث مباشر في خرائط Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                Text(
                    text = "افتح تطبيق خرائط Google للبحث التلقائي في نطاق ميت غمر",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                HorizontalDivider(color = BorderLight)
                Spacer(modifier = Modifier.height(8.dp))

                shortcuts.forEach { (label, query) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunchSearch(query) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MetGhamrTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// Helpers for Navigation & Intent
private fun openGoogleMapsNavigation(context: Context, business: BusinessEntity) {
    try {
        val uri = if (business.latitude != 0.0 && business.longitude != 0.0) {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${business.latitude},${business.longitude}")
        } else {
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(business.name + " " + business.address + " ميت غمر")}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to browser Google Maps
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(business.name + " ميت غمر")}")
        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
    }
}

private fun launchGoogleMapsQuery(context: Context, query: String) {
    try {
        val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("$query ميت غمر الدقهلية")}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun dialPhoneNumber(context: Context, phone: String) {
    try {
        val cleanPhone = phone.replace(" ", "").replace("-", "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
