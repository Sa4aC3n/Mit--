package com.example.ui.screens.ai

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DirectoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: DirectoryViewModel,
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val chatMessages by viewModel.aiChatMessages.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favIds = favorites.map { it.businessId }.toSet()

    var inputText by remember { mutableStateOf("") }
    var selectedExplanation by remember { mutableStateOf<RecommendationExplanation?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val suggestedQuestions = listOf(
        "🍽️ أفضل مطعم في ميت غمر؟",
        "☕ أقرب كافيه مني؟",
        "👨‍⚕️ أفضل طبيب قلب؟",
        "🏥 أقرب مستشفى؟",
        "🩺 عيادات تعمل الآن؟",
        "🛠️ فني تكييف قريب مني؟",
        "🏭 مصانع في ميت غمر؟",
        "💇 أفضل كوافير قريب؟",
        "🚗 أقرب مركز صيانة سيارات؟",
        "🍕 مطاعم مفتوحة الآن؟"
    )

    val quickCategories = listOf(
        "🍽️ مطاعم",
        "☕ كافيهات",
        "👨‍⚕️ أطباء",
        "🏥 مستشفيات",
        "🛠️ فنيين",
        "🏭 مصانع"
    )

    // Auto scroll to latest message when messages change
    LaunchedEffect(chatMessages.size, isAiLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(MetGhamrGold, MetGhamrTeal)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مساعد ميت غمر الذكي ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "اسألني عن أي مكان أو خدمة في ميت غمر",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAiChat() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "محادثة جديدة",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MetGhamrNavy
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Quick Categories Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(quickCategories) { categoryLabel ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.sendAiMessage("أين أجد $categoryLabel في ميت غمر؟")
                            },
                            label = { Text(categoryLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MetGhamrNavy.copy(alpha = 0.06f),
                                labelColor = MetGhamrNavy
                            )
                        )
                    }
                }

                // Input Field Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "اسأل عن مطعم، طبيب، خدمة، أو فني...",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetGhamrGold,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        ),
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { inputText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = Color.Gray)
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText
                                inputText = ""
                                viewModel.sendAiMessage(query)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isAiLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isAiLoading) MetGhamrNavy else Color.LightGray
                            )
                            .testTag("ai_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ النتائج تعتمد على البيانات المتاحة في دليل ميت غمر وقواعد التقييم المعتمدة.",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceLight)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                item(key = "ai_welcome_banner") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MetGhamrBlue.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MetGhamrNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "مساعد ميت غمر الذكي يغطي العامية المصرية والفصحى",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MetGhamrNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يمكنك السؤال بالطريقة العادية مثل: \"عايز مطعم كويس\" أو \"فين أقرب كافيه مفتوح دلوقتي\" وسيقوم بالبحث التلقائي في الدليل.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Suggested Questions Chips List
                item(key = "ai_suggested_questions") {
                    Column {
                        Text(
                            text = "💡 أسئلة واقتراحات شائعة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetGhamrNavy,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestedQuestions) { question ->
                                SuggestionChip(
                                    onClick = { viewModel.sendAiMessage(question.substringAfter(" ")) },
                                    label = { Text(question, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color.White,
                                        labelColor = MetGhamrNavy
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        borderColor = MetGhamrNavy.copy(alpha = 0.2f),
                                        borderWidth = 1.dp,
                                        enabled = true
                                    )
                                )
                            }
                        }
                    }
                }

                // Chat Messages Feed
                items(chatMessages, key = { it.id }) { message ->
                    AiChatMessageBubble(
                        message = message,
                        favIds = favIds,
                        onFavoriteToggle = { bizId -> viewModel.toggleFavorite(bizId, favIds.contains(bizId)) },
                        onDetailClick = { bizId -> viewModel.selectBusiness(bizId) },
                        onAskForLocation = {
                            viewModel.updateLocationPermission(
                                granted = true,
                                lat = com.example.data.ai.AiRecommendationEngine.MET_GHAMR_CENTER_LAT,
                                lng = com.example.data.ai.AiRecommendationEngine.MET_GHAMR_CENTER_LNG
                            )
                            if (message.structuredQuery != null) {
                                viewModel.sendAiMessage(message.structuredQuery.rawQuery)
                            }
                        },
                        onQuickClarificationClick = { choice ->
                            viewModel.sendAiMessage(choice)
                        },
                        onExplainClick = { explanation ->
                            selectedExplanation = explanation
                        }
                    )
                }

                // AI Thinking Loading Indicator
                if (isAiLoading) {
                    item(key = "ai_loading_indicator") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MetGhamrGold,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جاري البحث وتحليل بيانات الدليل بالذكاء الاصطناعي... ✨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MetGhamrNavy
                            )
                        }
                    }
                }
            }
        }
    }

    // Explanation BottomSheet / Dialog
    selectedExplanation?.let { exp ->
        AlertDialog(
            onDismissRequest = { selectedExplanation = null },
            icon = {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = MetGhamrNavy)
            },
            title = {
                Text(
                    text = "لماذا هذه التوصية؟ 💡",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MetGhamrNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "النشاط: ${exp.businessName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("التقييم العام:", fontSize = 12.sp)
                        Text("+${"%.1f".format(Locale.ENGLISH, exp.ratingScore)} نقطة", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MetGhamrGold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("وزن عدد التقييمات:", fontSize = 12.sp)
                        Text("+${"%.1f".format(Locale.ENGLISH, exp.reviewsWeight)} نقطة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (exp.openNowBonus > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مكافأة مفتوح الآن:", fontSize = 12.sp)
                            Text("+${"%.1f".format(Locale.ENGLISH, exp.openNowBonus)} نقطة", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OpenGreen)
                        }
                    }
                    if (exp.distanceBonus > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مكافأة القرب الجغرافي:", fontSize = 12.sp)
                            Text("+${"%.1f".format(Locale.ENGLISH, exp.distanceBonus)} نقطة", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MetGhamrBlue)
                        }
                    }
                    if (exp.verificationBonus > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مكافأة التوثيق الرسمي:", fontSize = 12.sp)
                            Text("+${"%.1f".format(Locale.ENGLISH, exp.verificationBonus)} نقطة", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MetGhamrNavy)
                        }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي المجموع:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${"%.1f".format(Locale.ENGLISH, exp.totalScore)} / 100", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MetGhamrGold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedExplanation = null }) {
                    Text("إغلاق", color = MetGhamrNavy, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatMessageBubble(
    message: AiChatMessage,
    favIds: Set<String>,
    onFavoriteToggle: (String) -> Unit,
    onDetailClick: (String) -> Unit,
    onAskForLocation: () -> Unit,
    onQuickClarificationClick: (String) -> Unit,
    onExplainClick: (RecommendationExplanation) -> Unit
) {
    val isUser = message.sender == AiSender.USER
    val context = LocalContext.current

    val timeStr = remember(message.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
        sdf.format(Date(message.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MetGhamrNavy),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Surface(
                color = if (isUser) MetGhamrNavy else Color.White,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                shadowElevation = if (isUser) 1.dp else 2.dp,
                modifier = Modifier.border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) Color.Transparent else Color.LightGray.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = if (isUser) Color.White else TextPrimary,
                        lineHeight = 19.sp
                    )

                    // Embedded Recommended Business Cards
                    if (!isUser && message.recommendedBusinesses.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            message.recommendedBusinesses.forEach { business ->
                                val explanation = message.explanation.firstOrNull { it.businessId == business.id }
                                AiBusinessCardItem(
                                    business = business,
                                    isFavorite = favIds.contains(business.id),
                                    explanation = explanation,
                                    onFavoriteToggle = { onFavoriteToggle(business.id) },
                                    onDetailClick = { onDetailClick(business.id) },
                                    onExplainClick = {
                                        if (explanation != null) onExplainClick(explanation)
                                    }
                                )
                            }
                        }
                    }

                    // Location Permission Prompt Button
                    if (message.isLocationPromptRequired) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onAskForLocation,
                            colors = ButtonDefaults.buttonColors(containerColor = MetGhamrGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MetGhamrNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📍 السماح بالموقع وتحديث النتائج", color = MetGhamrNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Quick Clarification Chips
                    if (message.quickClarifications.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            message.quickClarifications.forEach { choice ->
                                ElevatedFilterChip(
                                    selected = false,
                                    onClick = { onQuickClarificationClick(choice) },
                                    label = { Text(choice, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeStr,
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun AiBusinessCardItem(
    business: BusinessEntity,
    isFavorite: Boolean,
    explanation: RecommendationExplanation?,
    onFavoriteToggle: () -> Unit,
    onDetailClick: () -> Unit,
    onExplainClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = business.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MetGhamrNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (business.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "موثق",
                            tint = MetGhamrNavy,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "مفضلة",
                        tint = if (isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⭐ ${business.ratingAverage}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MetGhamrGold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${business.ratingCount} تقييم)",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📍 ${business.area}",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            if (explanation?.distanceKm != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🚗 يبعد حوالي ${"%.1f".format(Locale.ENGLISH, explanation.distanceKm)} كم عن موقعك",
                    fontSize = 10.sp,
                    color = MetGhamrBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Detail Button
                Button(
                    onClick = onDetailClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MetGhamrNavy),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("عرض التفاصيل", fontSize = 10.sp, color = Color.White)
                }

                // Call Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${business.phone}"))
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = MetGhamrNavy)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("اتصال", fontSize = 10.sp, color = MetGhamrNavy)
                }

                // Directions Button
                OutlinedButton(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${business.name} ${business.address}")}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode("${business.name} ${business.address}")}"))
                            context.startActivity(webMapIntent)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MetGhamrNavy)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("اتجاهات", fontSize = 10.sp, color = MetGhamrNavy)
                }

                // Why This Recommendation Button
                IconButton(
                    onClick = onExplainClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = "لماذا هذه التوصية", tint = MetGhamrGold, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
