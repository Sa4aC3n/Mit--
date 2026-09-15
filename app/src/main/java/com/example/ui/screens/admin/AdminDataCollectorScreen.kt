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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.MetGhamrGeoHierarchy
import com.example.data.engine.SearchPlanner
import com.example.data.model.*
import com.example.data.model.seed.InitialDataSeed
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import com.example.util.FileExportUtils

enum class DataCollectorTab(val titleAr: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONTROL("لوحة الجمع والتحكم", Icons.Default.PlayCircle),
    CANDIDATES("قائمة المراجعة والأنشطة", Icons.Default.Checklist),
    EXPANDED_DISCOVERY_TESTS("اختبارات التوسيع الشامل", Icons.Default.Explore),
    MULTI_SOURCE_TESTS("اختبارات توحيد المصادر", Icons.Default.Science),
    SUGGESTED_AREAS("المناطق المقترحة", Icons.Default.Map),
    SUGGESTED_CATEGORIES("التصنيفات المقترحة", Icons.Default.Category),
    CONFLICTS("حل التعارضات", Icons.Default.CompareArrows),
    MERGE_HISTORY("سجل الدمج والتراجع", Icons.Default.History),
    COVERAGE("تغطية النطاق الجغرافي وجودة البيانات", Icons.Default.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDataCollectorScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(DataCollectorTab.CONTROL) }

    val activeJob by viewModel.activeDiscoveryJob.collectAsState()
    val candidates by viewModel.discoveredCandidates.collectAsState()
    val conflicts by viewModel.activeDataConflicts.collectAsState()
    val suggestedCategories by viewModel.suggestedCategories.collectAsState()
    val suggestedAreas by viewModel.suggestedAreas.collectAsState()
    val isPaused by viewModel.isEnginePaused.collectAsState()

    var candidateFilter by remember { mutableStateOf("ALL") } // "ALL", "NEW", "DUPLICATE", "POSSIBLE_DUP", "CONFLICT", "APPROVED"
    var selectedCandidateForDetail by remember { mutableStateOf<CandidateBusiness?>(null) }
    var selectedConflictForDialog by remember { mutableStateOf<DataConflictItem?>(null) }

    AdminLayout(
        viewModel = viewModel,
        title = "محرك جمع وإثراء البيانات الذكي",
        currentRoute = ScreenRoute.AdminDataCollector.route,
        onBackClick = onBackClick
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            // Header Bar with Quick Stats
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth(),
                border = borderBrush()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🧠", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Smart Directory Data Collector",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "نظام اكتشاف وجمع وتنظيف وإثراء بيانات أنشطة ميت غمر والقرى التابعة",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        if (activeJob?.status == JobStatus.RUNNING) {
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري الجمع الآن", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary KPI Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricCard(
                            label = "تم اكتشافه",
                            value = "${candidates.size}",
                            color = SkyBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricCard(
                            label = "أنشطة جديدة",
                            value = "${candidates.count { it.status == CandidateStatus.NEW || it.status == CandidateStatus.APPROVED }}",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricCard(
                            label = "مكرر / فروع",
                            value = "${candidates.count { it.status == CandidateStatus.DEFINITE_DUPLICATE || it.isBranch }}",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        QuickMetricCard(
                            label = "تعارض بيانات",
                            value = "${conflicts.count { it.status == "OPEN" }}",
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tab Navigation Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF1E293B),
                contentColor = SkyBluePrimary,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
            ) {
                DataCollectorTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val badgeCount = when (tab) {
                        DataCollectorTab.CANDIDATES -> candidates.size
                        DataCollectorTab.CONFLICTS -> conflicts.count { it.status == "OPEN" }
                        DataCollectorTab.SUGGESTED_CATEGORIES -> suggestedCategories.count { it.status == "PENDING" }
                        DataCollectorTab.SUGGESTED_AREAS -> suggestedAreas.count { it.status == "PENDING" }
                        else -> 0
                    }

                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    tab.titleAr,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) SkyBluePrimary else Color(0xFF94A3B8)
                                )
                                if (badgeCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (tab == DataCollectorTab.CONFLICTS) Color(0xFFEF4444) else SkyBluePrimary,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "$badgeCount",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    DataCollectorTab.CONTROL -> {
                        DiscoveryControlTab(
                            viewModel = viewModel,
                            activeJob = activeJob,
                            isPaused = isPaused
                        )
                    }
                    DataCollectorTab.CANDIDATES -> {
                        ReviewQueueTab(
                            viewModel = viewModel,
                            candidates = candidates,
                            filter = candidateFilter,
                            onFilterChange = { candidateFilter = it },
                            onSelectCandidate = { selectedCandidateForDetail = it }
                        )
                    }
                    DataCollectorTab.EXPANDED_DISCOVERY_TESTS -> {
                        ExpandedDiscoveryTestsTab(viewModel = viewModel)
                    }
                    DataCollectorTab.MULTI_SOURCE_TESTS -> {
                        MultiSourceTestsTab(viewModel = viewModel)
                    }
                    DataCollectorTab.SUGGESTED_AREAS -> {
                        SuggestedAreasTab(
                            viewModel = viewModel,
                            suggestedAreas = suggestedAreas
                        )
                    }
                    DataCollectorTab.SUGGESTED_CATEGORIES -> {
                        SuggestedCategoriesTab(
                            viewModel = viewModel,
                            suggestedCategories = suggestedCategories
                        )
                    }
                    DataCollectorTab.CONFLICTS -> {
                        ConflictsResolutionTab(
                            viewModel = viewModel,
                            conflicts = conflicts,
                            onSelectConflict = { selectedConflictForDialog = it }
                        )
                    }
                    DataCollectorTab.MERGE_HISTORY -> {
                        MergeHistoryTab(viewModel = viewModel)
                    }
                    DataCollectorTab.COVERAGE -> {
                        CoverageAnalyticsTab(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Candidate Detail BottomSheet / Dialog
    selectedCandidateForDetail?.let { candidate ->
        CandidateDetailDialog(
            candidate = candidate,
            onDismiss = { selectedCandidateForDetail = null },
            onApprove = {
                viewModel.commitDiscoveredCandidatesBatch(listOf(candidate))
                selectedCandidateForDetail = null
            },
            onMerge = { masterId ->
                viewModel.mergeDuplicateBusiness(masterId, candidate)
                selectedCandidateForDetail = null
            }
        )
    }

    // Conflict Resolver Dialog
    selectedConflictForDialog?.let { conflict ->
        ConflictResolverDialog(
            conflict = conflict,
            onDismiss = { selectedConflictForDialog = null },
            onResolve = { chosenVal ->
                viewModel.resolveDataConflict(
                    conflictId = conflict.conflictId,
                    chosenValue = chosenVal,
                    candidateId = conflict.candidateId,
                    fieldName = conflict.fieldName
                )
                selectedConflictForDialog = null
            }
        )
    }
}

@Composable
fun DiscoveryControlTab(
    viewModel: DirectoryViewModel,
    activeJob: DiscoveryJob?,
    isPaused: Boolean
) {
    var jobTitle by remember { mutableStateOf("حملة جمع شاملة لكافة الأنشطة والخدمات") }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var selectedAreaName by remember { mutableStateOf("ALL") }
    var targetCount by remember { mutableStateOf(100) }

    var useGoogle by remember { mutableStateOf(true) }
    var useFacebook by remember { mutableStateOf(true) }
    var useYellowPages by remember { mutableStateOf(true) }
    var useOfficialWebsites by remember { mutableStateOf(true) }

    val categories = listOf(CategoryItem(id = "ALL", nameAr = "جميع التصنيفات والخدمات", iconName = "apps", colorHex = 0xFF0284C7)) + InitialDataSeed.categories
    val areas = listOf("ALL" to "كافة مناطق وقرى مركز ميت غمر (60+ قرية وحي)") +
            MetGhamrGeoHierarchy.allLocations.map { it.nameAr to "${it.nameAr} (${if (it.type == "VILLAGE") "قرية" else "شارع رئيسي"})" }

    val plannedQueries = remember(selectedCategoryId, selectedAreaName) {
        SearchPlanner.generateComprehensivePlan(selectedCategoryId, selectedAreaName, maxQueries = 6)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Job Progress Card
        if (activeJob != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyBluePrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    activeJob.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "الحالة: ${activeJob.status.titleAr}",
                                    color = when (activeJob.status) {
                                        JobStatus.RUNNING -> Color(0xFF10B981)
                                        JobStatus.PAUSED -> Color(0xFFF59E0B)
                                        JobStatus.COMPLETED -> Color(0xFF06B6D4)
                                        else -> Color(0xFFEF4444)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (activeJob.status == JobStatus.RUNNING) {
                                    IconButton(
                                        onClick = { viewModel.pauseDataDiscoveryJob() },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = "إيقاف مؤقت", tint = Color(0xFFF59E0B))
                                    }
                                } else if (activeJob.status == JobStatus.PAUSED) {
                                    IconButton(
                                        onClick = { viewModel.resumeDataDiscoveryJob() },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "استئناف", tint = Color(0xFF10B981))
                                    }
                                }

                                if (activeJob.status == JobStatus.RUNNING || activeJob.status == JobStatus.PAUSED) {
                                    IconButton(
                                        onClick = { viewModel.cancelDataDiscoveryJob() },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f))
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "إلغاء", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Bar & Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "الاستعلام الجاري: ${activeJob.currentQuery.ifBlank { "جاري التحضير والتوسيع..." }}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${activeJob.progressPercent}%",
                                color = SkyBluePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { activeJob.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SkyBluePrimary,
                            trackColor = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Job Statistics Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatChip(label = "مكتشف", value = "${activeJob.totalDiscovered}")
                            StatChip(label = "جديد", value = "${activeJob.newCount}", color = Color(0xFF10B981))
                            StatChip(label = "مكرر", value = "${activeJob.duplicateCount}", color = Color(0xFFEF4444))
                            StatChip(label = "تعارض", value = "${activeJob.conflictsCount}", color = Color(0xFF8B5CF6))
                            StatChip(label = "جودة", value = "${activeJob.qualityScoreAvg}%", color = SkyBluePrimary)
                        }
                    }
                }
            }
        }

        // Job Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚙️ إعدادات مهمة الجمع والاستكشاف المستهدفة",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Title Input
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("عنوان أو وصف المهمة") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selector
                    Text("التصنيف المستهدف:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.nameAr, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SkyBluePrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF334155),
                                    labelColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Count Selector
                    Text("العدد المطلوب جمعه:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(50, 100, 250, 500).forEach { count ->
                            Surface(
                                selected = targetCount == count,
                                onClick = { targetCount = count },
                                color = if (targetCount == count) SkyBluePrimary else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "$count نشاط",
                                    color = if (targetCount == count) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Source Toggles
                    Text("مصادر الجمع المعتمدة (Multi-Source Connectors):", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    SourceToggleRow(title = "خرائط وجوجل بليسز (Google Places API / Maps)", isChecked = useGoogle, onCheckedChange = { useGoogle = it })
                    SourceToggleRow(title = "صفحات فيسبوك الرسمية (Facebook Business Pages)", isChecked = useFacebook, onCheckedChange = { useFacebook = it })
                    SourceToggleRow(title = "دليل الصفحات الصفراء (Yellow Pages & Local Directories)", isChecked = useYellowPages, onCheckedChange = { useYellowPages = it })
                    SourceToggleRow(title = "المواقع الإلكترونية الرسمية للأنشطة (Official Websites)", isChecked = useOfficialWebsites, onCheckedChange = { useOfficialWebsites = it })

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Planner Query Expansion Preview
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ManageSearch, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("معاينة خطة الاستعلامات الذكية (Query Expansion):", color = SkyBluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            plannedQueries.forEach { q ->
                                Text("• ${q.queryText} (${q.targetArea})", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Start Job Button
                    Button(
                        onClick = {
                            val sourcesList = mutableListOf<DiscoverySourceType>()
                            if (useGoogle) sourcesList.add(DiscoverySourceType.GOOGLE_PLACES)
                            if (useFacebook) sourcesList.add(DiscoverySourceType.FACEBOOK_PAGES)
                            if (useYellowPages) sourcesList.add(DiscoverySourceType.YELLOW_PAGES)
                            if (useOfficialWebsites) sourcesList.add(DiscoverySourceType.OFFICIAL_WEBSITE)

                            viewModel.startDataDiscoveryJob(
                                title = jobTitle,
                                targetCategory = selectedCategoryId,
                                targetArea = selectedAreaName,
                                sources = sourcesList,
                                targetCount = targetCount
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                    ) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚀 بدء محرك الاستكشاف والجمع الفوري", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewQueueTab(
    viewModel: DirectoryViewModel,
    candidates: List<CandidateBusiness>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onSelectCandidate: (CandidateBusiness) -> Unit
) {
    val filteredCandidates = remember(candidates, filter) {
        when (filter) {
            "NEW" -> candidates.filter { it.status == CandidateStatus.NEW }
            "DUPLICATE" -> candidates.filter { it.status == CandidateStatus.DEFINITE_DUPLICATE }
            "POSSIBLE_DUP" -> candidates.filter { it.status == CandidateStatus.POSSIBLE_DUPLICATE || it.status == CandidateStatus.LIKELY_DUPLICATE }
            "CONFLICT" -> candidates.filter { it.hasConflict }
            "APPROVED" -> candidates.filter { it.status == CandidateStatus.APPROVED }
            else -> candidates
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filterOptions = listOf(
                "ALL" to "الكل (${candidates.size})",
                "NEW" to "جديد (${candidates.count { it.status == CandidateStatus.NEW }})",
                "DUPLICATE" to "مكرر مؤكد (${candidates.count { it.status == CandidateStatus.DEFINITE_DUPLICATE }})",
                "POSSIBLE_DUP" to "مكرر محتمل (${candidates.count { it.status == CandidateStatus.POSSIBLE_DUPLICATE || it.status == CandidateStatus.LIKELY_DUPLICATE }})",
                "CONFLICT" to "تعارض بيانات (${candidates.count { it.hasConflict }})",
                "APPROVED" to "معتمد (${candidates.count { it.status == CandidateStatus.APPROVED }})"
            )

            items(filterOptions) { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { onFilterChange(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyBluePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bulk Actions Bar
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "عدد المعروض: ${filteredCandidates.size}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val validToApprove = candidates.filter { it.status == CandidateStatus.NEW || it.status == CandidateStatus.ENRICHED }
                            viewModel.commitDiscoveredCandidatesBatch(validToApprove)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اعتماد ونشر بالسيرفر ☁️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.syncAllBusinessesToCloudServer()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = SkyBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مزامنة السحاب", fontSize = 11.sp, color = SkyBluePrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredCandidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("لا توجد سجلات في هذا التصنيف حالياً", color = Color(0xFF94A3B8), fontSize = 15.sp)
                    Text("يمكنك بدء مهمة جمع جديدة من علامة تبويب 'لوحة التحكم'", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCandidates, key = { it.id }) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onClick = { onSelectCandidate(candidate) },
                        onQuickApprove = { viewModel.commitDiscoveredCandidatesBatch(listOf(candidate)) },
                        onQuickMerge = {
                            if (candidate.matchCandidateId != null) {
                                viewModel.mergeDuplicateBusiness(candidate.matchCandidateId, candidate)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CandidateCard(
    candidate: CandidateBusiness,
    onClick: () -> Unit,
    onQuickApprove: () -> Unit,
    onQuickMerge: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (candidate.status) {
                CandidateStatus.DEFINITE_DUPLICATE -> Color(0xFFEF4444).copy(alpha = 0.5f)
                CandidateStatus.LIKELY_DUPLICATE, CandidateStatus.POSSIBLE_DUPLICATE -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                CandidateStatus.APPROVED -> Color(0xFF10B981).copy(alpha = 0.5f)
                else -> Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Status & Quality Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(candidate.status.colorHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        candidate.status.displayNameAr,
                        color = Color(candidate.status.colorHex),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("جودة البيانات: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Surface(
                        color = if (candidate.qualityScore >= 80) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${candidate.qualityScore}%",
                            color = if (candidate.qualityScore >= 80) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Business Name & Category
            Text(
                candidate.businessName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${candidate.categoryName} • ${candidate.specialty}",
                color = SkyBluePrimary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Address & Phone
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(candidate.displayPhone ?: candidate.phone ?: "غير متوفر", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                Spacer(modifier = Modifier.width(16.dp))

                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(candidate.area.ifBlank { candidate.city }, color = Color(0xFFCBD5E1), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Duplicate match reason if any
            if (candidate.duplicateReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("عوامل التطابق والمطابقة:", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        candidate.duplicateReasons.take(2).forEach { reason ->
                            Text("• $reason", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF334155),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        candidate.primarySource.displayNameAr,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (candidate.status == CandidateStatus.NEW) {
                    FilledTonalButton(
                        onClick = onQuickApprove,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("اعتماد وإضافة", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (candidate.matchCandidateId != null && candidate.status != CandidateStatus.UPDATED) {
                    FilledTonalButton(
                        onClick = onQuickMerge,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("دمج وإثراء", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConflictsResolutionTab(
    viewModel: DirectoryViewModel,
    conflicts: List<DataConflictItem>,
    onSelectConflict: (DataConflictItem) -> Unit
) {
    if (conflicts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✨", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("لا توجد تعارضات في البيانات حالياً", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("محرك المطابقة والدمج يعمل بدقة عالية وتوافق تام بين المصادر", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conflicts, key = { it.conflictId }) { conflict ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                conflict.businessName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (conflict.status == "RESOLVED") Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    if (conflict.status == "RESOLVED") "تم الحل" else "تعارض نشط",
                                    color = if (conflict.status == "RESOLVED") Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text("حقل التعارض: ${conflict.fieldLabelAr}", color = Color(0xFFF59E0B), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Comparison Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Source A Box
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.resolveDataConflict(
                                            conflictId = conflict.conflictId,
                                            chosenValue = conflict.valueA,
                                            candidateId = conflict.candidateId,
                                            fieldName = conflict.fieldName
                                        )
                                    },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(conflict.sourceA.displayNameAr, color = SkyBluePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(conflict.valueA, color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("اعتماد هذه القيمة 👈", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Source B Box
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.resolveDataConflict(
                                            conflictId = conflict.conflictId,
                                            chosenValue = conflict.valueB,
                                            candidateId = conflict.candidateId,
                                            fieldName = conflict.fieldName
                                        )
                                    },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(conflict.sourceB.displayNameAr, color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(conflict.valueB, color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("اعتماد هذه القيمة 👈", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedCategoriesTab(
    viewModel: DirectoryViewModel,
    suggestedCategories: List<SuggestedCategoryItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "💡 التصنيفات والخدمات الجديدة المكتشفة تلقائياً من الويب ومواقع التواصل:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(suggestedCategories, key = { it.id }) { cat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cat.nameAr,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = when (cat.status) {
                                "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                "REJECTED" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                when (cat.status) {
                                    "APPROVED" -> "تمت الموافقة"
                                    "REJECTED" -> "مرفوض"
                                    else -> "بانتظار القرار"
                                },
                                color = when (cat.status) {
                                    "APPROVED" -> Color(0xFF10B981)
                                    "REJECTED" -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text("التصنيف الأب المقترح: ${cat.parentCategoryName}", color = SkyBluePrimary, fontSize = 12.sp)
                    Text("مثال لنشاط مرتبط: ${cat.sampleBusinessName}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    if (cat.status == "PENDING") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.rejectSuggestedCategory(cat.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("رفض", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.approveSuggestedCategory(cat.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("الموافقة والإضافة للتصنيفات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedAreasTab(
    viewModel: DirectoryViewModel,
    suggestedAreas: List<SuggestedAreaItem>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = borderBrush()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = SkyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "🗺️ المناطق والشوارع والمجمعات المكتشفة تلقائياً:",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "يقوم المحرك برصد التجمعات والعزب والشوارع التجارية والمناطق الصناعية التي تظهر في عناوين الأنشطة ولكنها غير مسجلة في الهيكل الجغرافي الأساسي لميت غمر.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(suggestedAreas, key = { it.id }) { area ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                border = borderBrush()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            area.nameAr,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = when (area.status) {
                                "APPROVED" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                "REJECTED" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                when (area.status) {
                                    "APPROVED" -> "تمت الإضافة للهيكل"
                                    "REJECTED" -> "مستبعد"
                                    else -> "بانتظار الاعتماد (${area.confidence}%)"
                                },
                                color = when (area.status) {
                                    "APPROVED" -> Color(0xFF10B981)
                                    "REJECTED" -> Color(0xFFEF4444)
                                    else -> Color(0xFFF59E0B)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text("النوع المصنف: ${area.typeLabelAr}", color = SkyBluePrimary, fontSize = 12.sp)
                    Text("نشاط مرجعي رُصد فيه: ${area.sampleBusinessName}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    if (area.status == "PENDING") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.rejectSuggestedArea(area.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("استبعاد", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.approveSuggestedArea(area.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("الموافقة والإضافة للنطاق الجغرافي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandedDiscoveryTestsTab(
    viewModel: DirectoryViewModel
) {
    var test1Result by remember { mutableStateOf<String?>(null) }
    var test2Result by remember { mutableStateOf<String?>(null) }
    var isRunningTest1 by remember { mutableStateOf(false) }
    var isRunningTest2 by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                border = borderBrush(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = SkyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "مختبر اختبارات التوسيع الشامل (Expanded Discovery Lab)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "يتيح تشغيل سيناريوهات التوسيع واكتشاف الأنشطة عبر مصفوفة الاستعلامات المتعددة (Category × Subcategory × Area × Village × Synonyms × Language) مع تطبيق قواعد الجودة (QUALITY > QUANTITY) وتوحيد المصادر ومنع التكرار وحماية الفروع.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Expanded Scenario 1: Restaurants Comprehensive
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                border = borderBrush(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "سيناريو 1: التوسيع الشامل لقطاع المطاعم والمأكولات",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "تغطية كافة التصنيفات الفرعية (مشويات، أسماك، فراخ، بيتزا، كريب، سندوتشات، كشري، حلويات) والقرى والشوارع الرئيسية.",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                isRunningTest1 = true
                                viewModel.runExpandedRestaurantsDiscoveryTest { res ->
                                    test1Result = res
                                    isRunningTest1 = false
                                }
                            },
                            enabled = !isRunningTest1,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                        ) {
                            if (isRunningTest1) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل الاختبار")
                            }
                        }
                    }

                    test1Result?.let { output ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = output,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expanded Scenario 2: Doctors & Medical Specialties
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(14.dp),
                border = borderBrush(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "سيناريو 2: التوسيع الدقيق للأطباء والتخصصات الطبية",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "توليد استعلامات تخصصية دقيقة (قلب، عظام، أطفال، عيون، باطنة، أنف وأذن، جلدية، نساء وتوليد) دون تخمين عشوائي.",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                isRunningTest2 = true
                                viewModel.runExpandedDoctorsDiscoveryTest { res ->
                                    test2Result = res
                                    isRunningTest2 = false
                                }
                            },
                            enabled = !isRunningTest2,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            if (isRunningTest2) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل الاختبار", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    test2Result?.let { output ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = output,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoverageAnalyticsTab(viewModel: DirectoryViewModel) {
    val coverageReports = remember { viewModel.getCategoryCoverageReport() }
    val areaReports = remember { viewModel.computeAreaCoverageReport() }
    val analytics = remember { viewModel.getEngineAnalytics() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBluePrimary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 مؤشرات جودة وتغطية قاعدة بيانات ميت غمر", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBox("نسبة التوثيق", "${analytics.verifiedPercentage}%", Color(0xFF10B981), Modifier.weight(1f))
                        MetricBox("نقص الهواتف", "${analytics.missingPhonePercentage}%", Color(0xFFF59E0B), Modifier.weight(1f))
                        MetricBox("نقص المواقع", "${analytics.missingWebsitePercentage}%", Color(0xFF8B5CF6), Modifier.weight(1f))
                        MetricBox("معدل التكرار", "${analytics.duplicateDetectionRate}%", SkyBluePrimary, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text("📈 تقرير التغطية حسب التصنيفات والخدمات:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        items(coverageReports, key = { it.categoryId }) { report ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(report.categoryNameAr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${report.existingCount} مسجل / ${report.estimatedTotal} مقدّر (${report.coveragePercent}%)", color = SkyBluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { report.coveragePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (report.coveragePercent >= 80) Color(0xFF10B981) else if (report.coveragePercent >= 50) SkyBluePrimary else Color(0xFFF59E0B),
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("🗺️ تقرير التغطية الجغرافية (القرى والأحياء والشوارع التجارية):", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        items(areaReports, key = { it.areaName }) { areaStat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(areaStat.areaName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(areaStat.areaType, color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Text("${areaStat.totalBusinessesFound} نشاط (${areaStat.coverageScorePercent}%)", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { areaStat.coverageScorePercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (areaStat.coverageScorePercent >= 80) Color(0xFF10B981) else SkyBluePrimary,
                        trackColor = Color(0xFF334155)
                    )
                }
            }
        }
    }
}

// Dialogs & Small Helper Components

@Composable
fun CandidateDetailDialog(
    candidate: CandidateBusiness,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onMerge: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text(candidate.businessName, color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("التصنيف: ${candidate.categoryName} (${candidate.specialty})", color = SkyBluePrimary, fontSize = 13.sp)
                Text("الهاتف: ${candidate.displayPhone ?: candidate.phone ?: "غير متوفر"}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("العنوان: ${candidate.address}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("المنطقة/القرية: ${candidate.area}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                if (!candidate.website.isNullOrBlank()) Text("الموقع: ${candidate.website}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                if (!candidate.facebookPage.isNullOrBlank()) Text("فيسبوك: ${candidate.facebookPage}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text("المصدر الأساسي: ${candidate.primarySource.displayNameAr}", color = Color(0xFFF59E0B), fontSize = 12.sp)
                Text("درجة الجودة: ${candidate.qualityScore}%", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("اعتماد وإضافة للقاعدة", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun ConflictResolverDialog(
    conflict: DataConflictItem,
    onDismiss: () -> Unit,
    onResolve: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("حل تعارض في بيانات (${conflict.fieldLabelAr})", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("النشاط: ${conflict.businessName}", color = SkyBluePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("اختر القيمة الصحيحة لاعتمادها ومزامنتها على الخادم الرئيسي:", color = Color(0xFFCBD5E1), fontSize = 13.sp)

                Button(
                    onClick = { onResolve(conflict.valueA) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("${conflict.sourceA.displayNameAr}: ${conflict.valueA}", color = Color.White)
                }

                Button(
                    onClick = { onResolve(conflict.valueB) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Text("${conflict.sourceB.displayNameAr}: ${conflict.valueB}", color = Color(0xFFF59E0B))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun QuickMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SourceToggleRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SkyBluePrimary,
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

private fun borderBrush() = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SkyBluePrimary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = SkyBluePrimary,
    unfocusedLabelColor = Color(0xFF94A3B8)
)

@Composable
fun MultiSourceTestsTab(
    viewModel: DirectoryViewModel
) {
    var scenario1Output by remember { mutableStateOf<String?>(null) }
    var scenario2Output by remember { mutableStateOf<String?>(null) }
    var isRunning1 by remember { mutableStateOf(false) }
    var isRunning2 by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp),
                border = borderBrush(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = SkyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "مختبر اختبارات مطابقة وتوحيد المصادر (Entity Resolution Laboratory)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "يقوم هذا المعمل بفحص وتأكيد آلية تعامل النظام مع المنصات الخارجية (Google, Facebook, Yellow Pages, Official Website) باعتبارها مصادر بيانات (Data Sources) يتم توحيدها في كيان نشاط تجاري واحد، بالإضافة إلى التحقق من حماية الفروع.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Test Scenario 1 Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = borderBrush(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "السيناريو 1: توحيد 4 مصادر خارجية في نشاط واحد",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Google Places + Facebook Page + Official Website + Yellow Pages ➔ 1 Master BusinessEntity",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                isRunning1 = true
                                viewModel.runMultiSourceScenarioTest(1) { res ->
                                    scenario1Output = res
                                    isRunning1 = false
                                }
                            },
                            enabled = !isRunning1,
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                        ) {
                            if (isRunning1) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل الاختبار")
                            }
                        }
                    }

                    scenario1Output?.let { output ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = output,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Test Scenario 2 Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                border = borderBrush(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "السيناريو 2: التحقق من حماية الفروع المستقلة (Branch Protection)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "نفس الاسم التجاري (مطعم البركة) لكن هاتف وعنوان مختلف (كوم النور) ➔ منع الدمج التلقائي وتسجيله كفرع جديد",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                isRunning2 = true
                                viewModel.runMultiSourceScenarioTest(2) { res ->
                                    scenario2Output = res
                                    isRunning2 = false
                                }
                            },
                            enabled = !isRunning2,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            if (isRunning2) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل الاختبار", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    scenario2Output?.let { output ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF0F172A),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = output,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MergeHistoryTab(
    viewModel: DirectoryViewModel
) {
    val histories by viewModel.mergeHistories.collectAsState()

    if (histories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("لا توجد عمليات دمج سابقة", color = Color(0xFF94A3B8), fontSize = 14.sp)
                Text("عند دمج نشاطين مكررين سيتم حفظ سجل كامل هنا يتيح التراجع في أي وقت.", color = Color(0xFF64748B), fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(histories, key = { it.id }) { history ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (history.isReverted) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = borderBrush(),
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
                                    "دمج: ${history.candidateName} ➔ ${history.masterBusinessName}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "بواسطة: ${history.mergedBy}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            if (history.isReverted) {
                                Surface(
                                    color = Color(0xFF64748B).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                                ) {
                                    Text(
                                        "تم التراجع عنه",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.rollbackMerge(history.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تراجع عن الدمج (Undo)")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "الحقول التي تم تحديثها: ${history.mergedFieldsSummary}",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

