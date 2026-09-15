package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.ContributionStatus
import com.example.data.model.ContributionType
import com.example.data.model.UserContributionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContributionsScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onNavigateToAddBusiness: () -> Unit
) {
    val contributions by viewModel.myContributions.collectAsState()
    var selectedTabStatus by remember { mutableStateOf<String?>("ALL") }

    val filteredContributions = remember(contributions, selectedTabStatus) {
        if (selectedTabStatus == "ALL") {
            contributions
        } else {
            contributions.filter { it.status == selectedTabStatus }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مساهماتي واقتراحاتي",
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddBusiness,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("أضف نشاطاً", fontWeight = FontWeight.Bold) },
                containerColor = MetGhamrGold,
                contentColor = MetGhamrNavy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_business_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
        ) {
            // Status Tabs Header
            ScrollableTabRow(
                selectedTabIndex = when (selectedTabStatus) {
                    "ALL" -> 0
                    "PENDING" -> 1
                    "APPROVED" -> 2
                    "REJECTED" -> 3
                    else -> 0
                },
                containerColor = Color.White,
                contentColor = MetGhamrNavy,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedTabStatus == "ALL",
                    onClick = { selectedTabStatus = "ALL" },
                    text = { Text("الكل (${contributions.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabStatus == "PENDING",
                    onClick = { selectedTabStatus = "PENDING" },
                    text = {
                        Text(
                            "قيد المراجعة (${contributions.count { it.status == "PENDING" }})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTabStatus == "APPROVED",
                    onClick = { selectedTabStatus = "APPROVED" },
                    text = {
                        Text(
                            "مقبولة (${contributions.count { it.status == "APPROVED" }})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                Tab(
                    selected = selectedTabStatus == "REJECTED",
                    onClick = { selectedTabStatus = "REJECTED" },
                    text = {
                        Text(
                            "مرفوضة (${contributions.count { it.status == "REJECTED" }})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }

            if (filteredContributions.isEmpty()) {
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
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "لا توجد مساهمات في هذا التبويب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "يمكنك إضافة نشاط جديد أو اقتراح تعديل بيانات لمساعدة أهل ميت غمر.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToAddBusiness,
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إضافة نشاط تجاري جديد ➕")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredContributions, key = { it.id }) { item ->
                        ContributionCard(
                            item = item,
                            onClick = { viewModel.selectContribution(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionCard(
    item: UserContributionEntity,
    onClick: () -> Unit
) {
    val statusEnum = try {
        ContributionStatus.valueOf(item.status)
    } catch (_: Exception) {
        ContributionStatus.PENDING
    }

    val typeEnum = try {
        ContributionType.valueOf(item.type)
    } catch (_: Exception) {
        ContributionType.OTHER
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("contribution_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(statusEnum.badgeColorHex).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusEnum.displayNameAr,
                        color = Color(statusEnum.badgeColorHex),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = item.humanReadableId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MetGhamrNavy
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.businessName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MetGhamrNavy,
                    fontSize = 15.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "نوع الطلب: ${typeEnum.displayNameAr}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (item.fieldName != null) {
                Text(
                    text = "الحقل التعديلي: ${item.fieldName}",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (item.syncStatus == "WAITING_FOR_UPLOAD") {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⏳ محفوظ محلياً - بانتظار الاتصال بالإنترنت للرفع",
                        fontSize = 11.sp,
                        color = Color(0xFF856404),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestampArabic(item.createdAt),
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "عرض المسار والتفاصيل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetGhamrNavy
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MetGhamrNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
