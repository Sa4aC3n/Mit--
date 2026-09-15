package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
fun WriteReviewScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val business by viewModel.selectedBusiness.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var rating by remember { mutableFloatStateOf(5.0f) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var existingReview by remember { mutableStateOf<ReviewEntity?>(null) }
    var initialLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(business?.id, currentUser?.id) {
        val bId = business?.id
        if (bId != null) {
            viewModel.getUserReviewForBusiness(bId) { review ->
                if (review != null) {
                    existingReview = review
                    rating = review.rating
                    comment = review.comment
                }
                initialLoaded = true
            }
        }
    }

    val ratingLabel = when (rating.toInt()) {
        5 -> "ممتاز جدًا (5/5) ⭐⭐⭐⭐⭐"
        4 -> "جيد جدًا (4/5) ⭐⭐⭐⭐"
        3 -> "جيد (3/5) ⭐⭐⭐"
        2 -> "مقبول (2/5) ⭐⭐"
        1 -> "ضعيف (1/5) ⭐"
        else -> "اختر تقييمك"
    }

    val commentLength = comment.length
    val isCommentValid = comment.trim().isEmpty() || (commentLength in 5..500)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existingReview != null) "تعديل تقييمك" else "كتابة تقييم ومراجعة",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Business Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = business?.name ?: "النشاط التجاري",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy,
                            fontSize = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = business?.specialty ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cloud Database Sync Indicator Badge
                    Surface(
                        color = MetGhamrNavy.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MetGhamrTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "يتم حفظ التقييم ومزامنته سحابياً في قاعدة بيانات Firebase ☁️",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MetGhamrNavy
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "كيف كانت تجربتك مع هذا النشاط؟",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5 Star Selector Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (star in 1..5) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = "تقييم $star نجوم",
                                tint = if (star <= rating) MetGhamrGold else Color.LightGray,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { rating = star.toFloat() }
                                    .testTag("star_select_$star")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = ratingLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy,
                            fontSize = 14.sp
                        )
                    )
                }
            }

            // Comment Box Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "شارك انطباعك ورأيك التفصيلي (اختياري)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 500) comment = it },
                        placeholder = { Text("اكتب تجربتك مع الخدمة، المعاملة، أو الأسعار...", fontSize = 13.sp) },
                        minLines = 5,
                        maxLines = 8,
                        isError = !isCommentValid,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MetGhamrNavy,
                            unfocusedBorderColor = BorderLight,
                            focusedPlaceholderColor = TextMuted,
                            unfocusedPlaceholderColor = TextMuted,
                            cursorColor = MetGhamrNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_comment_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (!isCommentValid) "التعليق يجب أن يكون بين 5 و 500 حرف" else "يمكنك النشر بنجوم فقط أو مع نص",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (!isCommentValid) MetGhamrRed else TextSecondary,
                                fontSize = 11.sp
                            )
                        )

                        Text(
                            text = "$commentLength / 500",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Submit Action Button
            Button(
                onClick = {
                    if (isCommentValid && !isSubmitting) {
                        isSubmitting = true
                        viewModel.submitReview(rating, comment.trim()) { success ->
                            isSubmitting = false
                            if (success) {
                                onBackClick()
                            }
                        }
                    }
                },
                enabled = isCommentValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_review_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MetGhamrNavy,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (existingReview != null) "تحديث التقييم" else "نشر التقييم والأنشطة",
                        fontWeight = FontWeight.Bold,
                        color = MetGhamrNavy,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
