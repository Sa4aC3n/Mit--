package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReviewEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewsScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onNavigateToBusiness: (String) -> Unit
) {
    val myReviews by viewModel.myReviews.collectAsState()
    var deletingReviewId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تقييماتي ومراجعاتي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
        ) {
            if (myReviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "لم تقم بكتابة أي تقييم بعد",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "تصفح الأنشطة التجارية في ميت غمر وشارِك تجربتك لتقييم الخدمات المتاحة.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 13.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myReviews, key = { it.id }) { review ->
                        MyReviewCard(
                            review = review,
                            onClickBusiness = {
                                viewModel.selectBusiness(review.businessId)
                            },
                            onDeleteClick = { deletingReviewId = review.id }
                        )
                    }
                }
            }
        }
    }

    if (deletingReviewId != null) {
        AlertDialog(
            onDismissRequest = { deletingReviewId = null },
            title = { Text("حذف التقييم", fontWeight = FontWeight.Bold, color = MetGhamrRed) },
            text = { Text("هل أنت متأكد من حذف هذا التقييم نهائياً؟") },
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
fun MyReviewCard(
    review: ReviewEntity,
    onClickBusiness: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickBusiness() }
            .testTag("my_review_card_${review.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عرض النشاط التجاري 🔗",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MetGhamrNavy,
                        fontSize = 14.sp
                    )
                )

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

            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestampArabic(review.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف التقييم",
                        tint = MetGhamrRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
