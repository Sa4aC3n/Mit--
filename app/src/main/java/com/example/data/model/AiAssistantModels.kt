package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * Search Intents for AI Directory Assistant
 */
enum class AiSearchIntent(val titleAr: String) {
    BEST("الأفضل تقييماً والشعبية"),
    NEAREST("الأقرب جغرافياً"),
    OPEN_NOW("مفتوح الآن"),
    TOP_RATED("الأعلى تقييماً"),
    CHEAPEST("الأنسب سعراً"),
    SPECIALTY("حسب التخصص المحدد"),
    CATEGORY("حسب القسم الرئيسي"),
    LOCATION("حسب المنطقة/الشارع"),
    CONTACT("معلومات الاتصال والوسائل"),
    COMPARE("مقارنة بين أنشطة"),
    RECOMMENDATION("توصيات ذكية شاملة"),
    CLARIFICATION("طلب توضيح"),
    OUT_OF_DOMAIN("خارج نطاق دليل ميت غمر")
}

/**
 * Structured Query Extracted by AI Intent Parser
 */
@Immutable
data class AiStructuredQuery(
    val rawQuery: String,
    val intent: String = "RECOMMENDATION",
    val categoryName: String? = null,
    val specialty: String? = null,
    val targetArea: String? = null,
    val keyword: String? = null,
    val openNowOnly: Boolean = false,
    val minRating: Double = 0.0,
    val isNearestRequested: Boolean = false,
    val compareBusinessNames: List<String> = emptyList(),
    val isOutOfDomain: Boolean = false,
    val isAmbiguous: Boolean = false
)

/**
 * Transparency breakdown for AI recommendations
 */
@Immutable
data class RecommendationExplanation(
    val businessId: String,
    val businessName: String,
    val totalScore: Double,
    val ratingScore: Double,
    val reviewsWeight: Double,
    val distanceKm: Double?,
    val distanceBonus: Double,
    val openNowBonus: Double,
    val verificationBonus: Double,
    val summaryAr: String
)

/**
 * Chat Message Entity in AI Session
 */
@Immutable
data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: AiSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendedBusinesses: List<BusinessEntity> = emptyList(),
    val explanation: List<RecommendationExplanation> = emptyList(),
    val comparisonList: List<BusinessEntity> = emptyList(),
    val quickClarifications: List<String> = emptyList(),
    val structuredQuery: AiStructuredQuery? = null,
    val isLocationPromptRequired: Boolean = false
)

enum class AiSender {
    USER, AI, SYSTEM
}

/**
 * AI System Configuration & Controls (Admin)
 */
@Immutable
data class AiAssistantConfig(
    val isAiEnabled: Boolean = true,
    val providerName: String = "Gemini 3.5 Flash",
    val modelName: String = "gemini-3.5-flash",
    val dailyGuestLimit: Int = 15,
    val dailyUserLimit: Int = 50,
    val systemPromptAr: String = "أنت مساعد ميت غمر الذكي، خبير بدليل الأنشطة والخدمات في مدينة ميت غمر والقرى التابعة لها.",
    val ratingWeight: Double = 15.0,
    val reviewsWeightFactor: Double = 0.05,
    val openNowBoost: Double = 20.0,
    val verificationBoost: Double = 10.0
)

/**
 * AI Usage Analytics Metrics (Admin Dashboard)
 */
data class AiAnalyticsSummary(
    val questionsToday: Int = 42,
    val questionsThisMonth: Int = 1180,
    val mostAskedCategories: List<Pair<String, Int>> = listOf(
        "مطاعم وكافيهات" to 420,
        "أطباء وعيادات" to 310,
        "خدمات وصيانة" to 190,
        "مستشفيات وطوارئ" to 140,
        "مصانع وورش" to 120
    ),
    val topQueries: List<Pair<String, Int>> = listOf(
        "أفضل مطعم مشويات" to 88,
        "أقرب كافيه مفتوح دلوقتي" to 75,
        "دكتور أطفال شاطر" to 62,
        "فني تكييف" to 54,
        "مصانع في ميت غمر" to 41
    ),
    val errorRatePercentage: Double = 0.8,
    val avgResponseTimeMs: Long = 620,
    val estimatedMonthlyCostUsd: Double = 0.14
)
