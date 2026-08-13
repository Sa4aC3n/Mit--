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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RatingDistribution
import com.example.data.model.ReviewEntity
import com.example.data.model.ReviewSortOption
import com.example.ui.components.ProviderBadge
import com.example.ui.components.ReportReviewDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onWriteReviewClick: () -> Unit
) {
    val business by viewModel.selectedBusiness.collectAsState()
    val allReviews by viewModel.selectedBusinessReviews.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedStarFilter by remember { mutableStateOf(0) } // 0 = All
    var selectedSort by remember { mutableStateOf(ReviewSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var distribution by remember { mutableStateOf<RatingDistribution?>(null) }
    var reportingReviewId by remember { mutableStateOf<String?>(null) }
    var deletingReviewId by remember { mutableStateOf<String?>(null) }

    val userExistingReview = remember(allReviews, currentUser) {
        val uId = currentUser?.id
        if (uId != null) allReviews.find { it.userId == uId } else null
    }

    LaunchedEffect(business?.id, allReviews) {
        val bId = business?.id
        if (bId != null) {
            viewModel.getRatingDistribution(bId) { dist ->
                distribution = dist
            }
        }
    }

    // Filter & Sort logic
    val filteredAndSortedReviews = remember(allReviews, selectedStarFilter, selectedSort) {
        var list = allReviews.filter { it.status == "APPROVED" }

        if (selectedStarFilter in 1..5) {
            list = list.filter { it.rating.toInt() == selectedStarFilter }
        }

        when (selectedSort) {
            ReviewSortOption.NEWEST -> list.sortedByDescending { it.timestamp }
            ReviewSortOption.OLDEST -> list.sortedBy { it.timestamp }
            ReviewSortOption.HIGHEST_RATED -> list.sortedByDescending { it.rating }
            ReviewSortOption.LOWEST_RATED -> list.sortedBy { it.rating }
            ReviewSortOption.MOST_HELPFUL -> list.sortedByDescending { it.helpfulCount }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "مراجعات وتقييمات الزوار",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = business?.name ?: "",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MetGhamrNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.requestProtectedAction {
                        onWriteReviewClick()
                    }
                },
                containerColor = MetGhamrGold,
                contentColor = MetGhamrNavy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_write_review")
            ) {
                Icon(
                    if (userExistingReview != null) Icons.Default.Edit else Icons.Default.Star,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (userExistingReview != null) "تعديل تقييمك" else "أضف تقييمك",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Rating Summary & Distribution Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Column: Big Average Score
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", business?.ratingAverage ?: 0.0f),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MetGhamrNavy,
                                        fontSize = 42.sp
                                    )
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val avg = business?.ratingAverage ?: 0.0f
                                    for (i in 1..5) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i <= avg) MetGhamrGold else Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${business?.ratingCount ?: 0} تقييم ومراجعة",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .height(100.dp)
                                    .padding(horizontal = 12.dp),
                                color = BorderLight
                            )

                            // Right Column: Progress Bars per Star (5 down to 1)
                            Column(
                                modifier = Modifier.weight(1.5f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val dist = distribution
                                for (star in 5 downTo 1) {
                                    val pct = dist?.getStarPercentage(star) ?: 0f
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "$star ★",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.width(24.dp)
                                        )

                                        LinearProgressIndicator(
                                            progress = { pct },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MetGhamrGold,
                                            trackColor = SurfaceLight
                                        )

                                        Text(
                                            text = "${(pct * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = TextSecondary,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Filters & Sort Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تصفية التقييمات",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MetGhamrNavy
                            )
                        )

                        // Sort Selector Button
                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("sort_reviews_button")
                            ) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MetGhamrNavy
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedSort.displayNameAr,
                                    fontSize = 12.sp,
                                    color = MetGhamrNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                ReviewSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayNameAr, fontSize = 13.sp) },
                                        onClick = {
                                            selectedSort = option
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Filter Star Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = (selectedStarFilter == 0),
                                onClick = { selectedStarFilter = 0 },
                                label = { Text("الكل (${allReviews.size})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MetGhamrNavy,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        for (star in 5 downTo 1) {
                            item {
                                FilterChip(
                                    selected = (selectedStarFilter == star),
                                    onClick = { selectedStarFilter = star },
                                    label = { Text("$star ★", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MetGhamrGold,
                                        selectedLabelColor = MetGhamrNavy
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Review Cards List
            if (filteredAndSortedReviews.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.RateReview,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "لا توجد تقييمات مطابقة حتى الآن",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "كن أول من يشارك تجربته مع أهل ميت غمر عن هذا النشاط!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredAndSortedReviews, key = { it.id }) { review ->
                    ReviewCardItem(
                        review = review,
                        isOwner = (currentUser?.id == review.userId),
                        onHelpfulClick = { viewModel.toggleHelpfulVote(review.id) },
                        onReportClick = { reportingReviewId = review.id },
                        onEditClick = onWriteReviewClick,
                        onDeleteClick = { deletingReviewId = review.id },
                        hasUserVotedHelpful = viewModel.hasUserVotedHelpful(review.id).collectAsState(initial = false).value
                    )
                }
            }
        }
    }

    // Report Dialog
    if (reportingReviewId != null) {
        ReportReviewDialog(
            onDismiss = { reportingReviewId = null },
            onSubmitReport = { reason, desc ->
                viewModel.reportReview(reportingReviewId!!, reason, desc) {
                    reportingReviewId = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingReviewId != null) {
        AlertDialog(
            onDismissRequest = { deletingReviewId = null },
            title = { Text("حذف التقييم", fontWeight = FontWeight.Bold, color = MetGhamrRed) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا التقييم والمراجعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReview(deletingReviewId!!)
                        deletingReviewId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReviewId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ReviewCardItem(
    review: ReviewEntity,
    isOwner: Boolean,
    onHelpfulClick: () -> Unit,
    onReportClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    hasUserVotedHelpful: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: User Avatar, Name, Provider, Star Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MetGhamrNavy,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = review.userName.take(1).uppercase(),
                            color = MetGhamrGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProviderBadge(providerName = review.userProvider)

                        Text(
                            text = formatTimestampArabic(review.timestamp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )

                        if (review.updatedAt > review.timestamp) {
                            Text(
                                text = "• تم التعديل",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Stars Rating Badge
                Surface(
                    color = MetGhamrGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${review.rating.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MetGhamrNavy
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MetGhamrGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Comment Body
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Actions: Helpful button, Report button, Edit/Delete if Owner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Helpful Action
                Surface(
                    color = if (hasUserVotedHelpful) MetGhamrNavy.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onHelpfulClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = if (hasUserVotedHelpful) MetGhamrNavy else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (review.helpfulCount > 0) "مفيد (${review.helpfulCount})" else "مفيد",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (hasUserVotedHelpful) MetGhamrNavy else TextSecondary,
                                fontWeight = if (hasUserVotedHelpful) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOwner) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = MetGhamrNavy,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MetGhamrRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onReportClick,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إبلاغ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatTimestampArabic(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours < 24 -> "منذ $hours ساعة"
        days < 30 -> "منذ $days يوم"
        else -> {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
            sdf.format(Date(timestamp))
        }
    }
}
