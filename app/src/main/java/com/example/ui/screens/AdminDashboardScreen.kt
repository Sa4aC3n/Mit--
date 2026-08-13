package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessEntity
import com.example.data.model.ReviewEntity
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit
) {
    val stats by viewModel.adminStats.collectAsState()
    val allBusinesses by viewModel.allBusinessesAdmin.collectAsState()
    val allReviews by viewModel.allReviewsAdmin.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Businesses, 2: Reviews, 3: Send Push Notif
    var showAddDialog by remember { mutableStateOf(false) }

    // Add Form State Variables
    var nameInput by remember { mutableStateOf("") }
    var categoryIdInput by remember { mutableStateOf("cat_restaurants") }
    var categoryNameInput by remember { mutableStateOf("مطاعم") }
    var specialtyInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var phoneSecInput by remember { mutableStateOf("") }
    var whatsappInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var areaInput by remember { mutableStateOf("شارع الحرية") }
    var hoursInput by remember { mutableStateOf("09:00 ص - 10:00 م") }
    var descInput by remember { mutableStateOf("") }

    // Push Notif Form State
    var pushTitle by remember { mutableStateOf("") }
    var pushBody by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة التحكم وإحصائيات ميت غمر", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = MetGhamrNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("الإحصائيات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("إدارة المنشآت", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("المراجعات (${allReviews.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("إرسال إشعار", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Contents
            when (selectedTab) {
                0 -> {
                    // Analytics Overview & Charts
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "نظرة عامة على أداء الدليل المحلي",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMetricCard(
                                    title = "إجمالي الأنشطة",
                                    value = (stats?.totalBusinesses ?: 0).toString(),
                                    icon = Icons.Default.Store,
                                    color = MetGhamrNavy,
                                    modifier = Modifier.weight(1f)
                                )
                                StatMetricCard(
                                    title = "أنشطة موثقة",
                                    value = (stats?.verifiedBusinesses ?: 0).toString(),
                                    icon = Icons.Default.Verified,
                                    color = VerifiedBlue,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMetricCard(
                                    title = "إجمالي المستخدمين",
                                    value = (stats?.totalUsers ?: 0).toString(),
                                    icon = Icons.Default.People,
                                    color = MetGhamrTeal,
                                    modifier = Modifier.weight(1f)
                                )
                                StatMetricCard(
                                    title = "إجمالي المراجعات",
                                    value = (stats?.totalReviews ?: 0).toString(),
                                    icon = Icons.Default.RateReview,
                                    color = MetGhamrGold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            SimpleBarChart(data = stats?.categoryCounts ?: emptyMap())
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MetGhamrNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.navigateTo(com.example.ui.viewmodel.ScreenRoute.AdminDataManagement.route) }
                                    .testTag("admin_data_management_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.15f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Backup, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "🗄️ إدارة البيانات والنسخ الاحتياطي",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "النسخ الاحتياطي، الاستعادة، والاستيراد الجماعي Excel/CSV",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Business CRUD
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الأنشطة المضافة (${allBusinesses.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("admin_add_business_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة نشاط جديد", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(allBusinesses) { bus ->
                                AdminBusinessItemCard(
                                    business = bus,
                                    onToggleActive = { viewModel.toggleBusinessActive(bus.id, bus.isActive) },
                                    onToggleVerified = { viewModel.toggleBusinessVerified(bus.id, bus.isVerified) },
                                    onDelete = { viewModel.deleteBusiness(bus.id) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Reviews Moderation Queue
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allReviews) { rev ->
                            AdminReviewCard(
                                review = rev,
                                onApprove = { viewModel.moderateReview(rev.id, "APPROVED") },
                                onReject = { viewModel.moderateReview(rev.id, "REJECTED") }
                            )
                        }
                    }
                }

                3 -> {
                    // Send FCM Broadcast Notification
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "إرسال إشعار فوري لجميع مستخدمي ميت غمر 🔔",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                OutlinedTextField(
                                    value = pushTitle,
                                    onValueChange = { pushTitle = it },
                                    label = { Text("عنوان الإشعار", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = pushBody,
                                    onValueChange = { pushBody = it },
                                    label = { Text("محتوى الإشعار بالتفصيل", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth().height(100.dp)
                                )

                                Button(
                                    onClick = {
                                        if (pushTitle.isNotBlank() && pushBody.isNotBlank()) {
                                            viewModel.sendBroadcastNotification(pushTitle, pushBody)
                                            pushTitle = ""
                                            pushBody = ""
                                        } else {
                                            viewModel.showToast("يرجى ملء العنوان والنص الإشعاري")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("send_push_notif_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بث الإشعار الفوري الآن", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add New Business Dialog Form
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة نشاط جديد إلى دليل ميت غمر", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("اسم النشاط التجاري / العيادة", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = categoryNameInput,
                            onValueChange = { categoryNameInput = it },
                            label = { Text("الفئة الرئيسية (مطاعم، أطباء، فنيين...)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = specialtyInput,
                            onValueChange = { specialtyInput = it },
                            label = { Text("التخصص الدقيق", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("رقم الهاتف الرئيسي", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = whatsappInput,
                            onValueChange = { whatsappInput = it },
                            label = { Text("رقم الواتساب (مثال: 201012345678)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = areaInput,
                            onValueChange = { areaInput = it },
                            label = { Text("المنطقة أو القرية (شارع الحرية، صهرجت الكبرى...)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("العنوان التفصيلي", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = hoursInput,
                            onValueChange = { hoursInput = it },
                            label = { Text("مواعيد العمل", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("وصف مختصر", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && phoneInput.isNotBlank()) {
                            viewModel.saveBusiness(
                                id = null,
                                name = nameInput,
                                categoryId = categoryIdInput,
                                categoryName = categoryNameInput,
                                specialty = specialtyInput,
                                phone = phoneInput,
                                phoneSec = phoneSecInput,
                                whatsapp = whatsappInput,
                                address = addressInput,
                                area = areaInput,
                                workingHours = hoursInput,
                                description = descInput
                            )
                            showAddDialog = false
                            nameInput = ""
                            phoneInput = ""
                        } else {
                            viewModel.showToast("يرجى إدخال الاسم ورقم التليفون الرئيسي")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy)
                ) {
                    Text("حفظ النشاط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun AdminBusinessItemCard(
    business: BusinessEntity,
    onToggleActive: () -> Unit,
    onToggleVerified: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = business.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row {
                    IconButton(onClick = onToggleVerified, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (business.isVerified) VerifiedBlue else Color.Gray
                        )
                    }
                    IconButton(onClick = onToggleActive, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (business.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (business.isActive) OpenGreen else ClosedRed
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MetGhamrRed
                        )
                    }
                }
            }

            Text(
                text = "${business.categoryName} • ${business.area} • ${business.phone}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun AdminReviewCard(
    review: ReviewEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${review.userName} (⭐ ${review.rating})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Surface(
                    color = when (review.status) {
                        "APPROVED" -> OpenGreen.copy(alpha = 0.15f)
                        "REJECTED" -> ClosedRed.copy(alpha = 0.15f)
                        else -> MetGhamrGold.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = review.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (review.status) {
                            "APPROVED" -> OpenGreen
                            "REJECTED" -> ClosedRed
                            else -> MetGhamrNavy
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = review.comment, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = OpenGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("موافقة ونشر", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ClosedRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("حظر / إخفاء", fontSize = 11.sp)
                }
            }
        }
    }
}
