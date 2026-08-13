package com.example.data.ai

import com.example.data.model.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar

class AiAssistantService(
    private val config: AiAssistantConfig = AiAssistantConfig()
) {

    private val generativeModel: GenerativeModel? by lazy {
        try {
            val apiKey = try {
                // Access via reflection or BuildConfig safely
                val buildConfigClass = Class.forName("com.example.BuildConfig")
                val field = buildConfigClass.getField("GEMINI_API_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }

            if (apiKey.isNotBlank() && apiKey != "null") {
                GenerativeModel(
                    modelName = config.modelName,
                    apiKey = apiKey
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Executes end-to-end AI Assistant pipeline:
     * Question -> Intent Extraction -> DB Candidate Query -> Ranking Engine -> AI Response Synthesis
     */
    suspend fun processUserQuery(
        userQuery: String,
        candidatesProvider: suspend (AiStructuredQuery) -> List<BusinessEntity>,
        userLat: Double?,
        userLng: Double?,
        isLocationPermissionGranted: Boolean
    ): AiChatMessage = withContext(Dispatchers.IO) {

        // Step 1: Parse Intent
        val structuredQuery = parseQueryWithFallback(userQuery)

        // Handle Out Of Domain Queries
        if (structuredQuery.isOutOfDomain) {
            return@withContext AiChatMessage(
                sender = AiSender.AI,
                text = "أستطيع مساعدتك فقط في العثور على الخدمات، الأطباء، والمطاعم والأنشطة المتاحة في دليل ميت غمر. 🏥🍽️☕\nكيف يمكنني مساعدتك في البحث داخل الدليل اليوم؟",
                structuredQuery = structuredQuery
            )
        }

        // Handle Ambiguous Queries
        if (structuredQuery.isAmbiguous) {
            return@withContext AiChatMessage(
                sender = AiSender.AI,
                text = "أهلاً بك! يرجى تحديد التخصص أو القسم المطلوب للحصول على أفضل التوصيات المتاحة في ميت غمر: 🎯",
                quickClarifications = listOf(
                    "🍽️ أفضل مطاعم",
                    "☕ أقرب كافيهات",
                    "👨‍⚕️ أطباء وعيادات",
                    "🏥 مستشفيات وطوارئ",
                    "🛠️ فنيين وصيانة",
                    "🏭 مصانع وورش"
                ),
                structuredQuery = structuredQuery
            )
        }

        // Handle Location Request if Nearest asked without permission
        val needsLocation = structuredQuery.isNearestRequested && !isLocationPermissionGranted && userLat == null
        if (needsLocation) {
            return@withContext AiChatMessage(
                sender = AiSender.AI,
                text = "لإظهار الأماكن والخدمات الأقرب لموقعك الحالي بالظبط، اسمح للتطبيق بالوصول إلى موقعك الجغرافي 📍\n\nأو يمكنك تحديد اسم شارع أو منطقة في ميت غمر (مثل: شارع الحرية، صهرجت الكبرى، السنانية، شارع المحطة).",
                isLocationPromptRequired = true,
                structuredQuery = structuredQuery
            )
        }

        // Step 2: Fetch DB Candidates
        val rawCandidates = candidatesProvider(structuredQuery)

        // Handle No DB Results
        if (rawCandidates.isEmpty()) {
            return@withContext AiChatMessage(
                sender = AiSender.AI,
                text = buildString {
                    append("لم أجد نتائج مطابقة تماماً لشروطك في دليل ميت غمر حالياً. 🔍\n")
                    if (structuredQuery.openNowOnly) {
                        append("💡 نصيحة: جرب البحث بدون شرط \"مفتوح الآن\" لرؤية جميع الأنشطة المسجلة وقوائم مواعيد العمل.\n")
                    }
                    if (!structuredQuery.targetArea.isNull_or_blank()) {
                        append("💡 أو يمكنك البحث في كامل مدينة ميت غمر بدلاً من منطقة (${structuredQuery.targetArea}).")
                    }
                },
                structuredQuery = structuredQuery
            )
        }

        // Step 3: Rank & Score Candidates
        val (rankedBusinesses, explanations) = AiRecommendationEngine.rankAndExplain(
            candidates = rawCandidates,
            query = structuredQuery,
            userLat = userLat,
            userLng = userLng
        )

        val topRecommendations = rankedBusinesses.take(5)
        val topExplanations = explanations.take(5)

        // Step 4: Synthesize Conversational Response
        val responseText = synthesizeResponseText(
            query = structuredQuery,
            recommendations = topRecommendations,
            totalFoundCount = rankedBusinesses.size,
            userLat = userLat
        )

        return@withContext AiChatMessage(
            sender = AiSender.AI,
            text = responseText,
            recommendedBusinesses = topRecommendations,
            explanation = topExplanations,
            structuredQuery = structuredQuery
        )
    }

    private suspend fun parseQueryWithFallback(userQuery: String): AiStructuredQuery {
        val model = generativeModel
        if (model == null) {
            return LocalArabicRuleEngine.parseQuery(userQuery)
        }

        return try {
            val promptText = """
                You are an Intent Parser for the Met Ghamr Directory in Egypt.
                User Query: "$userQuery"
                Output JSON ONLY with structure:
                {
                   "intent": "BEST|NEAREST|OPEN_NOW|TOP_RATED|SPECIALTY|CATEGORY|COMPARE|OUT_OF_DOMAIN|CLARIFICATION",
                   "categoryName": "مطاعم وكافيهات | أطباء وعيادات | مستشفيات وطوارئ | خدمات وصيانة | مصانع وورش | تسوق وخدمات شخصية | null",
                   "specialty": "extracted specialty or null",
                   "targetArea": "extracted Met Ghamr area or null",
                   "openNowOnly": boolean,
                   "isNearestRequested": boolean,
                   "isOutOfDomain": boolean,
                   "isAmbiguous": boolean
                }
            """.trimIndent()

            val response = model.generateContent(
                content { text(promptText) }
            )
            val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: ""
            if (jsonText.isBlank()) {
                LocalArabicRuleEngine.parseQuery(userQuery)
            } else {
                val json = JSONObject(jsonText)
                AiStructuredQuery(
                    rawQuery = userQuery,
                    intent = json.optString("intent", "RECOMMENDATION"),
                    categoryName = if (json.isNull("categoryName")) null else json.optString("categoryName"),
                    specialty = if (json.isNull("specialty")) null else json.optString("specialty"),
                    targetArea = if (json.isNull("targetArea")) null else json.optString("targetArea"),
                    openNowOnly = json.optBoolean("openNowOnly", false),
                    isNearestRequested = json.optBoolean("isNearestRequested", false),
                    isOutOfDomain = json.optBoolean("isOutOfDomain", false),
                    isAmbiguous = json.optBoolean("isAmbiguous", false)
                )
            }
        } catch (e: Exception) {
            LocalArabicRuleEngine.parseQuery(userQuery)
        }
    }

    private fun synthesizeResponseText(
        query: AiStructuredQuery,
        recommendations: List<BusinessEntity>,
        totalFoundCount: Int,
        userLat: Double?
    ): String {
        val count = recommendations.size
        return buildString {
            when {
                query.isNearestRequested && userLat != null -> {
                    append("بناءً على موقعك الجغرافي وتقييمات دليل ميت غمر، إليك أقرب الخيارات المتاحة:")
                }
                query.openNowOnly -> {
                    append("الأنشطة والخدمات المفتوحة الآن في ميت غمر وفق مواعيد العمل المحدثة:")
                }
                query.intent == "BEST" || query.intent == "TOP_RATED" -> {
                    append("بناءً على تقييمات المستخدمين الموثقة وعدد المراجعات في الدليل، هذه أفضل النتائج:")
                }
                else -> {
                    append("إليك أفضل $count خيارات ترشيحاً وفق بيانات دليل ميت غمر المعتمدة:")
                }
            }

            if (totalFoundCount > count) {
                append("\n(توجد ${totalFoundCount - count} خيارات إضافية متوفرة بالبحث المباشر).")
            }
        }
    }

    private fun String?.isNull_or_blank(): Boolean {
        return this == null || this.isBlank()
    }
}
