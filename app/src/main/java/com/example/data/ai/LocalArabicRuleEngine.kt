package com.example.data.ai

import com.example.data.model.AiSearchIntent
import com.example.data.model.AiStructuredQuery

object LocalArabicRuleEngine {

    /**
     * Parses user query in natural Arabic (MSA or Egyptian Colloquial) into a structured query
     */
    fun parseQuery(userQuery: String): AiStructuredQuery {
        val raw = userQuery.trim()
        val lower = raw.lowercase()

        // 1. Check Out Of Domain
        val outOfDomainKeywords = listOf("اختراق", "هكر", "برمجة كود", "سياسة", "حروب", "رياضيات", "كيمياء", "فيزياء", "ترجمة نص")
        if (outOfDomainKeywords.any { lower.contains(it) }) {
            return AiStructuredQuery(
                rawQuery = raw,
                intent = AiSearchIntent.OUT_OF_DOMAIN.name,
                isOutOfDomain = true
            )
        }

        // 2. Check Comparison Query
        if (lower.contains("قارن") || lower.contains("المقارنة") || lower.contains("مين أفضل بين") || lower.contains("إيه الفرق بين")) {
            val names = extractComparisonNames(raw)
            return AiStructuredQuery(
                rawQuery = raw,
                intent = AiSearchIntent.COMPARE.name,
                compareBusinessNames = names
            )
        }

        // 3. Ambiguous Query Check
        if (raw.length < 4 || lower == "أفضل واحد" || lower == "مين الأحسن" || lower == "عايز حاجة" || lower == "فين المكان") {
            return AiStructuredQuery(
                rawQuery = raw,
                intent = AiSearchIntent.CLARIFICATION.name,
                isAmbiguous = true
            )
        }

        // 4. Intent Extraction
        val isNearest = lower.contains("أقرب") || lower.contains("قريب") || lower.contains("جنبي") || lower.contains("قريبة")
        val isOpenNow = lower.contains("دلوقتي") || lower.contains("مفتوح") || lower.contains("شغال") || lower.contains("الان") || lower.contains("الآن") || lower.contains("تشتغل")
        val isBest = lower.contains("أفضل") || lower.contains("أحسن") || lower.contains("شاطر") || lower.contains("حلو") || lower.contains("ممتاز") || lower.contains("أعلى") || lower.contains("تاوب") || lower.contains("top")

        val intentStr = when {
            isNearest -> AiSearchIntent.NEAREST.name
            isOpenNow -> AiSearchIntent.OPEN_NOW.name
            isBest -> AiSearchIntent.BEST.name
            else -> AiSearchIntent.RECOMMENDATION.name
        }

        // 5. Category and Specialty Extraction
        var detectedCategory: String? = null
        var detectedSpecialty: String? = null

        // Restaurant & Cafe synonyms
        if (lower.contains("مطعم") || lower.contains("أكل") || lower.contains("مشويات") || lower.contains("بيتزا") || lower.contains("كباب") || lower.contains("وجبات") || lower.contains("سندوتش") || lower.contains("فول") || lower.contains("طعمية")) {
            detectedCategory = "مطاعم وكافيهات"
            if (lower.contains("مشويات")) detectedSpecialty = "مشويات"
            else if (lower.contains("بيتزا")) detectedSpecialty = "بيتزا"
            else if (lower.contains("أسماك") || lower.contains("سمك")) detectedSpecialty = "أسماك"
            else if (lower.contains("شاورما")) detectedSpecialty = "شاورما"
        } else if (lower.contains("كافيه") || lower.contains("قهوة") || lower.contains("مقهى") || lower.contains("مشروبات")) {
            detectedCategory = "مطاعم وكافيهات"
            detectedSpecialty = "كافيهات"
        }

        // Doctors & Clinics
        else if (lower.contains("دكتور") || lower.contains("طبيب") || lower.contains("عيادة") || lower.contains("شاطر") || lower.contains("أطفال") || lower.contains("قلب") || lower.contains("جلدية") || lower.contains("أسنان") || lower.contains("عظام") || lower.contains("باطنة") || lower.contains("عيادات")) {
            detectedCategory = "أطباء وعيادات"
            if (lower.contains("أطفال")) detectedSpecialty = "أطفال"
            else if (lower.contains("قلب")) detectedSpecialty = "قلب وأوعية دموية"
            else if (lower.contains("جلدية")) detectedSpecialty = "جلدية وتجميل"
            else if (lower.contains("أسنان")) detectedSpecialty = "طب وجراحة الأسنان"
            else if (lower.contains("عظام")) detectedSpecialty = "جراحة العظام"
            else if (lower.contains("باطنة")) detectedSpecialty = "أمراض باطنة"
            else if (lower.contains("نساء") || lower.contains("توليد")) detectedSpecialty = "نساء وتوليد"
        }

        // Hospitals & Emergency
        else if (lower.contains("مستشفى") || lower.contains("طوارئ") || lower.contains("عناية") || lower.contains("مستشفيات")) {
            detectedCategory = "مستشفيات وطوارئ"
        }

        // Labs & Radiology
        else if (lower.contains("تحاليل") || lower.contains("معمل") || lower.contains("مختبر")) {
            detectedCategory = "أطباء وعيادات"
            detectedSpecialty = "معامل تحاليل"
        } else if (lower.contains("أشعة") || lower.contains("اشعة") || lower.contains("مركز أشعة")) {
            detectedCategory = "أطباء وعيادات"
            detectedSpecialty = "مراكز أشعة"
        }

        // Technicians & Maintenance
        else if (lower.contains("تكييف") || lower.contains("صنايعي") || lower.contains("فني") || lower.contains("سباك") || lower.contains("كهربائي") || lower.contains("نقاش") || lower.contains("نجار")) {
            detectedCategory = "خدمات وصيانة"
            if (lower.contains("تكييف")) detectedSpecialty = "صيانة تكييفات"
            else if (lower.contains("سباك")) detectedSpecialty = "سباكة"
            else if (lower.contains("كهربائي")) detectedSpecialty = "كهرباء"
        }

        // Auto Repair
        else if (lower.contains("ميكانيكي") || lower.contains("سيارات") || lower.contains("صيانة سيارات") || lower.contains("كاوتش") || lower.contains("غسيل سيارات")) {
            detectedCategory = "خدمات وصيانة"
            detectedSpecialty = "صيانة سيارات"
        }

        // Factories & Workshops
        else if (lower.contains("مصنع") || lower.contains("مصانع") || lower.contains("ورشة") || lower.contains("ألوميتال") || lower.contains("حديد")) {
            detectedCategory = "مصانع وورش"
        }

        // Salons & Beauty
        else if (lower.contains("كوافير") || lower.contains("حلاق") || lower.contains("تجميل") || lower.contains("صالون")) {
            detectedCategory = "تسوق وخدمات شخصية"
        }

        // 6. Location Area Extraction
        val areasInMetGhamr = listOf(
            "شارع الحرية", "شارع المحطة", "ميدان المحطة", "السنانية", "صهرجت الكبرى",
            "أتميدة", "كوم النور", "دنديط", "تفهنا الأشراف", "بشلا", "سنفا", "حي المعلمين", "وسط البلد"
        )
        val targetArea = areasInMetGhamr.firstOrNull { lower.contains(it.lowercase()) }

        return AiStructuredQuery(
            rawQuery = raw,
            intent = intentStr,
            categoryName = detectedCategory,
            specialty = detectedSpecialty,
            targetArea = targetArea,
            keyword = detectedSpecialty ?: detectedCategory ?: raw,
            openNowOnly = isOpenNow,
            minRating = if (isBest) 4.0 else 0.0,
            isNearestRequested = isNearest
        )
    }

    private fun extractComparisonNames(raw: String): List<String> {
        val cleaned = raw.replace("قارن بين", "").replace("قارن", "").replace("إيه الفرق بين", "")
        val parts = cleaned.split("و", "ضد", "مع", "أو")
        return parts.map { it.trim() }.filter { it.isNotBlank() }
    }
}
