package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AuthProvider
import com.example.data.model.BusinessEntity
import com.example.data.model.ReviewEntity
import com.example.ui.components.InteractiveRatingSelector
import com.example.ui.components.ProviderBadge
import com.example.ui.components.RatingStarsDisplay
import com.example.ui.screens.admin.AdminEditBusinessDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import com.example.ui.viewmodel.ScreenRoute
import com.example.util.WorkingHoursUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDetailScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val business by viewModel.selectedBusiness.collectAsState()
    val reviews by viewModel.selectedBusinessReviews.collectAsState()
    val isFavorite by viewModel.isSelectedBusinessFavorite.collectAsState()
    val relatedBusinesses by viewModel.relatedBusinesses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showReviewDialog by remember { mutableStateOf(false) }
    var showPhoneSelectionSheet by remember { mutableStateOf(false) }
    var showAdminEditDialog by remember { mutableStateOf(false) }
    var selectedImageForFullscreen by remember { mutableStateOf<String?>(null) }
    var selectedStarRating by remember { mutableFloatStateOf(5.0f) }
    var reviewCommentText by remember { mutableStateOf("") }

    val isSuperAdmin = currentUser?.isSuperAdmin == true || currentUser?.email?.trim()?.equals("m.k3shka@gmail.com", ignoreCase = true) == true

    if (business == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MetGhamrNavy)
                Spacer(modifier = Modifier.height(12.dp))
                Text("جاري تحميل بيانات المنشأة...", color = TextSecondary, fontSize = 13.sp)
            }
        }
        return
    }

    val b = business!!

    // Generate list of images for carousel (hero image + fallback gallery images per category)
    val galleryImages = remember(b) {
        getGalleryImagesForBusiness(b)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = b.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Super Admin Direct Edit Button
                    if (isSuperAdmin) {
                        IconButton(
                            onClick = { showAdminEditDialog = true },
                            modifier = Modifier.testTag("detail_super_admin_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل النشاط (Super Admin)",
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }

                    // Share Button
                    IconButton(
                        onClick = { shareBusinessInfo(context, b) },
                        modifier = Modifier.testTag("detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "مشاركة المنشأة",
                            tint = TextPrimary
                        )
                    }

                    // Favorite Button
                    IconButton(
                        onClick = { viewModel.requestProtectedAction { viewModel.toggleFavorite(b.id, isFavorite) } },
                        modifier = Modifier.testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) MetGhamrRed else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCard,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Business Images Carousel / Gallery Header ---
            item {
                BusinessImageCarouselHeader(
                    images = galleryImages,
                    onImageClick = { url -> selectedImageForFullscreen = url }
                )
            }

            // --- Super Admin Exclusive Management Card ---
            if (isSuperAdmin) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD4AF37).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "لوحة تحكم مدير النظام الأعلى (Super Admin) 👑",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "تعديل الاسم، العنوان، خريطة Google Maps، أرقام الهاتف والصور",
                                                fontSize = 10.sp,
                                                color = Color(0xFFFFD700)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { showAdminEditDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("super_admin_open_edit_dialog_button")
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "تعديل النشاط",
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 2. Business Header & Identity Info ---
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Category & Verification Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MetGhamrNavy.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.updateSearchQuery(b.categoryName)
                                        viewModel.navigateTo("search")
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = b.categoryName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MetGhamrNavy,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MetGhamrNavy,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                if (b.isVerified) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(VerifiedBlue.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "موثق",
                                            tint = VerifiedBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "نشاط موثق رسمياً ✔️",
                                            color = VerifiedBlue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = b.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 20.sp
                                )
                            )

                            if (b.specialty.isNotBlank()) {
                                Text(
                                    text = "تخصص: ${b.specialty}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(color = BorderLight)

                            Spacer(modifier = Modifier.height(12.dp))

                            // Rating Stars, Open Now Status, View Count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RatingStarsDisplay(rating = b.ratingAverage)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${b.ratingAverage} (${b.ratingCount} تقييم)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                }

                                val statusInfo = WorkingHoursUtils.getStatusInfo(b.workingHours)
                                Surface(
                                    color = statusInfo.containerColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(statusInfo.dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = statusInfo.label,
                                            color = statusInfo.contentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. Quick Actions Grid Card ---
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "التواصل السريع والاتجاهات ⚡",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call Button
                                Button(
                                    onClick = {
                                        if (!b.phoneSecondary.isNullOrBlank()) {
                                            showPhoneSelectionSheet = true
                                        } else {
                                            dialPhoneNumber(context, b.phone)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SkyBlueDark,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_call_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "اتصال",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "اتصال",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                // WhatsApp Button
                                if (!b.whatsapp.isNullOrBlank()) {
                                    Button(
                                        onClick = { openWhatsAppChat(context, b.whatsapp!!) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF22C55E),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("detail_whatsapp_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "واتساب",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "واتساب",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Facebook Button
                                if (!b.facebookUrl.isNullOrBlank()) {
                                    Button(
                                        onClick = { openFacebookPage(context, b.facebookUrl!!) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1877F2),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("detail_facebook_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "فيسبوك",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "فيسبوك",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Website Button
                                if (!b.websiteUrl.isNullOrBlank()) {
                                    Button(
                                        onClick = { openWebsite(context, b.websiteUrl!!) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0369A1),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("detail_website_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "الموقع",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "موقعنا",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Map Directions Button
                                Button(
                                    onClick = { openMapDirections(context, b) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4F46E5),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_map_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = "الاتجاهات",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الاتجاهات",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Address & Location Specifications ---
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "العنوان والموقع بالتفصيل 📍",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            )

                            // City / Village Field
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("المدينة / القرية التابعة:", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        text = if (b.city.isNotBlank()) b.city else "مدينة ميت غمر",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MetGhamrNavy
                                    )
                                }
                            }

                            HorizontalDivider(color = BorderLight)

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MetGhamrTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("العنوان والمنطقة التفصيلية:", fontSize = 11.sp, color = TextSecondary)
                                    Text("${b.area} — ${b.address}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                            }

                            HorizontalDivider(color = BorderLight)

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MetGhamrGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("مواعيد العمل الرسمية:", fontSize = 11.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val statusInfo = WorkingHoursUtils.getStatusInfo(b.workingHours)
                                        Surface(
                                            color = statusInfo.containerColor,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusInfo.contentColor
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = b.workingHours,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            if (!b.phoneSecondary.isNullOrBlank()) {
                                HorizontalDivider(color = BorderLight)
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("أرقام هاتف إضافية للعيادة / المنشأة:", fontSize = 11.sp, color = TextSecondary)
                                        Text(b.phoneSecondary!!, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    }
                                }
                            }

                            if (!b.facebookUrl.isNullOrBlank()) {
                                HorizontalDivider(color = BorderLight)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openFacebookPage(context, b.facebookUrl!!) }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("صفحة الفيسبوك الرسمية:", fontSize = 11.sp, color = TextSecondary)
                                        Text(b.facebookUrl!!, fontSize = 12.sp, color = Color(0xFF1877F2), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF1877F2))
                                }
                            }

                            if (!b.websiteUrl.isNullOrBlank()) {
                                HorizontalDivider(color = BorderLight)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openWebsite(context, b.websiteUrl!!) }
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = MetGhamrNavy, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الموقع الإلكتروني الرسمي:", fontSize = 11.sp, color = TextSecondary)
                                        Text(b.websiteUrl!!, fontSize = 12.sp, color = MetGhamrNavy, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MetGhamrNavy)
                                }
                            }

                            HorizontalDivider(color = BorderLight)

                            // Phase 9 Suggest Edit / Report Issue CTAs / Super Admin Direct Edit
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSuperAdmin) {
                                    Surface(
                                        onClick = { showAdminEditDialog = true },
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("super_admin_direct_edit_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "تعديل فوري لبيانات النشاط (Super Admin) 👑",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD700)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.requestProtectedAction {
                                                viewModel.navigateTo(ScreenRoute.SuggestEdit.route)
                                            }
                                        },
                                        modifier = Modifier.testTag("suggest_edit_button")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اقتراح تعديل البيانات ✏️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrNavy)
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.requestProtectedAction {
                                                viewModel.navigateTo(ScreenRoute.ReportIncorrectData.route)
                                            }
                                        },
                                        modifier = Modifier.testTag("report_incorrect_data_button")
                                    ) {
                                        Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("الإبلاغ عن خطأ ⚠️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MetGhamrRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 5. Specialty Tags / Services Offered ---
            if (b.specialty.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "الخدمات والتخصصات المتاحة 🛠️",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val tags = b.specialty.split("،", ",", "و").map { it.trim() }.filter { it.length > 1 }
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    tags.forEach { tag ->
                                        Surface(
                                            color = SurfaceCard,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MetGhamrNavy,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tag,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
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

            // --- 6. About / Description Card (Expandable) ---
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "عن المنشأة ورؤية الخدمة ℹ️",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            ExpandableDescriptionText(text = b.description)
                        }
                    }
                }
            }

            // --- 7. Rating Breakdown & Reviews Section ---
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                        text = "آراء وتقييمات الأهالي ⭐",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = "إجمالي التقييمات: ${b.ratingCount}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.requestProtectedAction {
                                            viewModel.navigateTo(ScreenRoute.WriteReview.route)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrGold),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("add_review_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RateReview,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("أضف تقييمك", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Rating Breakdown Bars
                            RatingDistributionView(reviews = reviews, overallRating = b.ratingAverage)

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick 1-Tap Star Rating Section with Firebase Database Sync
                            val currentUser by viewModel.currentUser.collectAsState()
                            val myReview = reviews.find { it.userId == currentUser?.id }
                            val currentStar = myReview?.rating ?: 0f

                            Surface(
                                color = MetGhamrGoldLight.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = MetGhamrNavy,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تقييم سريع بالنجوم (تخزين فوري في Firebase ☁️):",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MetGhamrNavy,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (star in 1..5) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.requestProtectedAction {
                                                        viewModel.submitReview(star.toFloat(), myReview?.comment ?: "") { success ->
                                                            if (success) {
                                                                viewModel.showToast("تم حفظ تقييمك ($star نجوم) في Firebase بنجاح 🌟")
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .testTag("quick_star_rate_$star")
                                            ) {
                                                Icon(
                                                    imageVector = if (star <= currentStar) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "تقييم $star نجوم",
                                                    tint = if (star <= currentStar) MetGhamrGold else Color.Gray.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (myReview != null) {
                                        Text(
                                            text = "تقييمك الحالي: ${myReview.rating.toInt()} نجوم ⭐ (محفوظ في Firebase Database)",
                                            color = MetGhamrTeal,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "اضغط على أي نجمة لحفظ تقييمك مباشرة ومزامنته سحابياً",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(ScreenRoute.Reviews.route) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("view_all_reviews_button")
                            ) {
                                Text(
                                    text = "شاهد كافة التقييمات والمراجعات (${b.ratingCount}) 💬",
                                    color = MetGhamrNavy,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- 8. Reviews Preview Items ---
            if (reviews.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد مراجعات مكتوبة بعد. كن أول من يكتب تقييماً لمساعدة أهالي ميت غمر!",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                items(reviews.take(5)) { review ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ReviewItemCard(review = review)
                    }
                }
            }

            // --- 9. Related Businesses Section ---
            if (relatedBusinesses.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "منشآت وأنشطة مشابهة في ميت غمر 🏬",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(relatedBusinesses, key = { "related_${it.id}" }) { related ->
                                RelatedBusinessCard(
                                    business = related,
                                    onClick = { viewModel.selectBusiness(related.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Fullscreen Image Dialog ---
    selectedImageForFullscreen?.let { imageUrl ->
        Dialog(
            onDismissRequest = { selectedImageForFullscreen = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "صورة جودة عالية",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { selectedImageForFullscreen = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }
            }
        }
    }

    // --- Phone Number Selection Bottom Sheet ---
    if (showPhoneSelectionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPhoneSelectionSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "اختر رقم الاتصال المطلوب 📞",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Primary Phone
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialPhoneNumber(context, b.phone)
                            showPhoneSelectionSheet = false
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = MetGhamrNavy)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("الرقم الرئيسي", fontSize = 11.sp, color = TextSecondary)
                            Text(b.phone, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Phone
                if (!b.phoneSecondary.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                dialPhoneNumber(context, b.phoneSecondary!!)
                                showPhoneSelectionSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneForwarded, contentDescription = null, tint = MetGhamrTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("الرقم الإضافي / العيادة", fontSize = 11.sp, color = TextSecondary)
                                Text(b.phoneSecondary!!, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- Submit Review Modal Dialog ---
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Text(
                    text = "أضف تقييمك ورأيك في ${b.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentUser == null) {
                        Surface(
                            color = MetGhamrGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚠️ يشترط تسجيل الدخول ببريد Google أو Facebook أو Microsoft أولاً للتقييم.",
                                fontSize = 12.sp,
                                color = MetGhamrNavy,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("مرحباً: ", fontSize = 12.sp)
                            Text(currentUser!!.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            ProviderBadge(provider = currentUser!!.provider)
                        }
                    }

                    Text("اختر عدد النجوم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    InteractiveRatingSelector(
                        currentRating = selectedStarRating,
                        onRatingSelected = { selectedStarRating = it }
                    )

                    OutlinedTextField(
                        value = reviewCommentText,
                        onValueChange = { reviewCommentText = it },
                        placeholder = { Text("اكتب تجربتك ورأيك بالتفصيل هنا...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUser == null) {
                            viewModel.showToast("يرجى اختيار منصة الدخول أولاً من صفحة الحساب")
                            viewModel.navigateTo("profile")
                        } else {
                            viewModel.submitReview(selectedStarRating, reviewCommentText)
                            showReviewDialog = false
                            reviewCommentText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                ) {
                    Text(if (currentUser != null) "نشر التقييم" else "التسجيل أولاً")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Super Admin Edit Business Modal Dialog ---
    if (showAdminEditDialog && isSuperAdmin) {
        AdminEditBusinessDialog(
            business = b,
            viewModel = viewModel,
            onDismiss = { showAdminEditDialog = false }
        )
    }
}

// --- Image Carousel Header Composable ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BusinessImageCarouselHeader(
    images: List<String>,
    onImageClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val imageUrl = images[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onImageClick(imageUrl) }
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "صورة المنشأة",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Gradient at bottom for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
            }
        }

        // Image Page Indicator Tag (e.g., "1 / 3")
        if (images.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// --- Expandable Text Component ---
@Composable
fun ExpandableDescriptionText(text: String) {
    var expanded by remember { mutableStateOf(false) }
    val maxLines = if (expanded) Int.MAX_VALUE else 3

    Column {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 20.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )

        if (text.length > 120) {
            Text(
                text = if (expanded) "عرض أقل ▲" else "عرض المزيد ▼",
                color = MetGhamrNavy,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

// --- Rating Distribution Bars ---
@Composable
fun RatingDistributionView(reviews: List<ReviewEntity>, overallRating: Float) {
    val totalReviews = reviews.size.coerceAtLeast(1)
    val starCounts = remember(reviews) {
        val map = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0)
        reviews.forEach { r ->
            val star = r.rating.toInt().coerceIn(1, 5)
            map[star] = (map[star] ?: 0) + 1
        }
        map
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Score Summary Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Text(
                text = "%.1f".format(overallRating),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MetGhamrNavy
            )
            RatingStarsDisplay(rating = overallRating)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "من 5 نجوم",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }

        VerticalDivider(modifier = Modifier.height(80.dp), color = BorderLight)

        Spacer(modifier = Modifier.width(12.dp))

        // Distribution Bars
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (5 downTo 1).forEach { star ->
                val count = starCounts[star] ?: 0
                val progress = count.toFloat() / totalReviews

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("$star ★", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MetGhamrGold,
                        trackColor = SurfaceCard
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$count", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(20.dp))
                }
            }
        }
    }
}

// --- Related Business Item Card ---
@Composable
fun RelatedBusinessCard(
    business: BusinessEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = business.imageUrl ?: getCategoryPlaceholderImage(business.categoryId),
                    contentDescription = business.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (business.isVerified) {
                    Surface(
                        color = VerifiedBlue,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "موثق",
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = business.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary
                )

                Text(
                    text = business.specialty,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MetGhamrGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "%.1f".format(business.ratingAverage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = business.area.take(12),
                        fontSize = 9.sp,
                        color = MetGhamrNavy,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- Helper Functions for Intent Launching & Sharing ---
private fun dialPhoneNumber(context: Context, phone: String) {
    try {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openWhatsAppChat(context: Context, whatsapp: String) {
    try {
        var cleanNum = whatsapp.replace(Regex("[^0-9]"), "")
        if (cleanNum.startsWith("01")) {
            cleanNum = "20" + cleanNum.substring(1)
        }
        val url = "https://api.whatsapp.com/send?phone=$cleanNum"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openFacebookPage(context: Context, facebookUrl: String) {
    try {
        val formattedUrl = if (facebookUrl.startsWith("http://") || facebookUrl.startsWith("https://")) {
            facebookUrl
        } else {
            "https://$facebookUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openWebsite(context: Context, websiteUrl: String) {
    try {
        val formattedUrl = if (websiteUrl.startsWith("http://") || websiteUrl.startsWith("https://")) {
            websiteUrl
        } else {
            "https://$websiteUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openMapDirections(context: Context, business: BusinessEntity) {
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
            val webIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        try {
            val fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(business.name + " " + business.address + " ميت غمر")}")
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        } catch (err: Exception) {
            err.printStackTrace()
        }
    }
}

private fun shareBusinessInfo(context: Context, b: BusinessEntity) {
    try {
        val shareText = """
            📍 *${b.name}*
            🏷️ القسم: ${b.categoryName} (${b.specialty})
            🗺️ العنوان: ${b.area} - ${b.address}
            📞 الهاتف: ${b.phone}
            ${if (!b.whatsapp.isNullOrBlank()) "💬 واتساب: ${b.whatsapp}\n" else ""}
            تمت المشاركة عبر *دليل ميت غمر الشامل* 🇪🇬
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة بيانات ${b.name}")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getGalleryImagesForBusiness(b: BusinessEntity): List<String> {
    val list = mutableListOf<String>()
    if (!b.imageUrl.isNullOrBlank()) {
        list.add(b.imageUrl)
    }

    // High quality category-matched fallback gallery images
    val fallbackList = when (b.categoryId) {
        "cat_restaurants" -> listOf(
            "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80"
        )
        "cat_doctors" -> listOf(
            "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1579684385127-1ef15d508118?auto=format&fit=crop&w=800&q=80"
        )
        "cat_technicians" -> listOf(
            "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=800&q=80"
        )
        "cat_pharmacies" -> listOf(
            "https://images.unsplash.com/photo-1586015555751-63c23e85e2ef?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1576602976047-174e57a47881?auto=format&fit=crop&w=800&q=80"
        )
        "cat_factories" -> listOf(
            "https://images.unsplash.com/photo-1504917599217-d4dc5ebe6122?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=800&q=80"
        )
        else -> listOf(
            "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80"
        )
    }

    fallbackList.forEach { url ->
        if (!list.contains(url)) {
            list.add(url)
        }
    }
    return list.take(4)
}

private fun getCategoryPlaceholderImage(categoryId: String): String {
    return when (categoryId) {
        "cat_restaurants" -> "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80"
        "cat_doctors" -> "https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&w=800&q=80"
        "cat_technicians" -> "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=800&q=80"
        "cat_pharmacies" -> "https://images.unsplash.com/photo-1586015555751-63c23e85e2ef?auto=format&fit=crop&w=800&q=80"
        else -> "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80"
    }
}

@Composable
fun ReviewItemCard(review: ReviewEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!review.userAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = review.userAvatarUrl,
                            contentDescription = review.userName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            color = MetGhamrNavy.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = review.userName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = MetGhamrNavy,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = review.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val provider = try { AuthProvider.valueOf(review.userProvider) } catch (e: Exception) { AuthProvider.GOOGLE }
                            ProviderBadge(provider = provider)
                        }
                        RatingStarsDisplay(rating = review.rating)
                    }
                }

                val dateStr = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale("ar")).format(java.util.Date(review.timestamp))
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = review.comment,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
