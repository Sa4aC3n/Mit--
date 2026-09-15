package com.example.util

import com.example.data.model.CategoryItem
import com.example.data.model.SubcategoryItem
import com.example.data.model.seed.InitialDataSeed

/**
 * Normalizes Arabic text (removes Hamza variations, Ya/Alef Maqsura, Ta Marbuta differences)
 * and resolves any user-entered category or specialty string to its canonical category & subcategory.
 */
object CategoryNormalizer {

    private val categories = InitialDataSeed.categories

    /**
     * Cleans and unifies Arabic characters for consistent matching.
     */
    fun normalizeArabic(text: String): String {
        return text.trim().lowercase()
            .replace("[أإآا]".toRegex(), "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("[\\u064B-\\u065F]".toRegex(), "") // Remove Harakat / Tashkeel
            .replace("[-_/]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Finds the canonical CategoryItem matching raw input.
     * Maps inputs like "طبيب", "اطباء", "دكتور", "عياده", "نوادي", "نوادى", "مدارس", "جامعات", "مصالح حكوميه"
     * or deduces category from contextual hints (name, specialty, description).
     */
    fun resolveCategory(rawCategory: String, contextHint: String = ""): CategoryItem {
        val clean = normalizeArabic(rawCategory)
        
        // 1. Direct match on normalized nameAr
        if (clean.isNotBlank()) {
            categories.find { normalizeArabic(it.nameAr) == clean }?.let { return it }

            // 2. Contains match on category name
            categories.find { normalizeArabic(it.nameAr).contains(clean) || clean.contains(normalizeArabic(it.nameAr)) }?.let { return it }

            // 3. Synonym / Keyword Mapping
            val matchFromCat = matchCategoryKeywords(clean)
            if (matchFromCat != null) return matchFromCat
        }

        // 4. If category was blank, generic ("عام", "خدمات عامة"), or unknown, deduce from contextHint (e.g. business name or description)
        if (contextHint.isNotBlank()) {
            val cleanHint = normalizeArabic(contextHint)
            val matchFromHint = matchCategoryKeywords(cleanHint)
            if (matchFromHint != null) return matchFromHint
            
            // Subcategory / specialty keywords inside all categories
            for (cat in categories) {
                for (sub in cat.subcategories) {
                    val subClean = normalizeArabic(sub.nameAr)
                    if (cleanHint.contains(subClean) || sub.keywords.any { cleanHint.contains(normalizeArabic(it)) }) {
                        return cat
                    }
                }
            }
        }

        // Fallback: If input exists return first category, otherwise default to shops/general
        return categories.find { it.id == "cat_shops" } ?: categories.first()
    }

    private fun matchCategoryKeywords(clean: String): CategoryItem? {
        return when {
            clean.contains("طبيب") || clean.contains("اطباء") || clean.contains("دكتور") || clean.contains("دكتوره") || clean.contains("عياد") || clean.contains("كشف") || clean.contains("استشاري") || clean.contains("اخصائي") -> {
                categories.find { it.id == "cat_doctors" }
            }
            clean.contains("مستشف") || clean.contains("طوارئ") || clean.contains("اسعاف") || clean.contains("عنايه مركزه") -> {
                categories.find { it.id == "cat_hospitals" }
            }
            clean.contains("مركز طبي") || clean.contains("مجمع عيادات") || clean.contains("مجمع طبي") || clean.contains("مركز اسنان") || clean.contains("مركز عيون") || clean.contains("مركز جراح") || clean.contains("مركز علاج") -> {
                categories.find { it.id == "cat_medical_centers" }
            }
            clean.contains("صيدل") || clean.contains("دواء") || clean.contains("علاج") || clean.contains("روشتا") || clean.contains("اجزاخان") -> {
                categories.find { it.id == "cat_pharmacies" }
            }
            clean.contains("اشع") || clean.contains("تحاليل") || clean.contains("معمل") || clean.contains("معامل") || clean.contains("رنين") || clean.contains("سونار") || clean.contains("مختبر") -> {
                categories.find { it.id == "cat_radiology" }
            }
            clean.contains("مطعم") || clean.contains("مطاعم") || clean.contains("مشوي") || clean.contains("اكل") || clean.contains("وجب") || clean.contains("كريب") || clean.contains("بيتزا") || clean.contains("سمك") || clean.contains("فول") || clean.contains("طعمي") || clean.contains("فطائر") || clean.contains("برجر") || clean.contains("شاورما") || clean.contains("حواوشي") || clean.contains("كباب") || clean.contains("ماكولات") || clean.contains("سندوتش") -> {
                categories.find { it.id == "cat_restaurants" }
            }
            clean.contains("كافي") || clean.contains("مقه") || clean.contains("قهو") || clean.contains("عصير") || clean.contains("عصائر") || clean.contains("ايس كريم") || clean.contains("شاي") || clean.contains("بلايستيشن") || clean.contains("كوفي") -> {
                categories.find { it.id == "cat_cafes" }
            }
            clean.contains("مدرس") || clean.contains("مدارس") || clean.contains("تعليم") || clean.contains("حضان") || clean.contains("سنتر تعليمي") || clean.contains("دروس") || clean.contains("اكاديمي") -> {
                categories.find { it.id == "cat_schools" }
            }
            clean.contains("جامع") || clean.contains("معهد") || clean.contains("كلي") || clean.contains("ازهر") || clean.contains("ثانوي عام") -> {
                categories.find { it.id == "cat_universities" }
            }
            clean.contains("ناد") || clean.contains("نواد") || clean.contains("جيم") || clean.contains("لياق") || clean.contains("رياض") || clean.contains("مسبح") || clean.contains("ملاعب") || clean.contains("بادل") || clean.contains("فتنس") -> {
                categories.find { it.id == "cat_clubs" }
            }
            clean.contains("بنك") || clean.contains("صراف") || clean.contains("atm") || clean.contains("فلوس") || clean.contains("اهلي") || clean.contains("cib") || clean.contains("قاهر") || clean.contains("اسكندري") -> {
                categories.find { it.id == "cat_banks" }
            }
            clean.contains("حكوم") || clean.contains("سجل") || clean.contains("مجلس") || clean.contains("بريد") || clean.contains("تأمين") || clean.contains("شهر عقار") || clean.contains("مرور") || clean.contains("تموين") || clean.contains("كهربا") || clean.contains("مياه") || clean.contains("ضرائب") || clean.contains("محكم") || clean.contains("شرط") -> {
                categories.find { it.id == "cat_government" }
            }
            clean.contains("شرك") || clean.contains("مقاول") || clean.contains("شحن") || clean.contains("عقار") || clean.contains("استيراد") || clean.contains("تصدير") || clean.contains("توريدات") || clean.contains("استثمار") -> {
                categories.find { it.id == "cat_companies" }
            }
            clean.contains("مكتب") || clean.contains("تصوير") || clean.contains("طباع") || clean.contains("كتب") || clean.contains("ادوات مدرسي") || clean.contains("د عاي") || clean.contains("اعلان") -> {
                categories.find { it.id == "cat_libraries" }
            }
            clean.contains("خير") || clean.contains("ايتام") || clean.contains("جمعي") || clean.contains("اهلي") || clean.contains("مساعدات") || clean.contains("زكاه") || clean.contains("صدقات") || clean.contains("رسال") || clean.contains("اورمان") -> {
                categories.find { it.id == "cat_charities" }
            }
            clean.contains("فني") || clean.contains("سباك") || clean.contains("كهربا") || clean.contains("نجار") || clean.contains("نقاش") || clean.contains("صيان") || clean.contains("تكييف") || clean.contains("تبريد") || clean.contains("الوميتال") || clean.contains("سيراميك") || clean.contains("دش") || clean.contains("كاميرات") -> {
                categories.find { it.id == "cat_technicians" }
            }
            clean.contains("مصنع") || clean.contains("ورش") || clean.contains("الومنيوم") || clean.contains("حداد") || clean.contains("مخرط") || clean.contains("تصنيع") || clean.contains("انتاج") || clean.contains("صناعي") -> {
                categories.find { it.id == "cat_factories" }
            }
            clean.contains("سيار") || clean.contains("ميكانيك") || clean.contains("عفش") || clean.contains("كاوتش") || clean.contains("بطاريات") || clean.contains("غسيل سيارات") || clean.contains("سمكر") || clean.contains("دوكو") || clean.contains("قطع غيار") || clean.contains("معرض سيارات") || clean.contains("ونش") -> {
                categories.find { it.id == "cat_automotive" }
            }
            clean.contains("نقاب") || clean.contains("مهندس") || clean.contains("معلم") || clean.contains("محام") || clean.contains("زراعي") || clean.contains("تجاري") -> {
                categories.find { it.id == "cat_syndicates" }
            }
            clean.contains("مناسب") || clean.contains("قاع") || clean.contains("فرح") || clean.contains("افراح") || clean.contains("فراش") || clean.contains("كوافير") || clean.contains("بيوتي سنتر") || clean.contains("فوتوسيشن") || clean.contains("دي جي") -> {
                categories.find { it.id == "cat_home_events" }
            }
            clean.contains("محل") || clean.contains("ماركت") || clean.contains("ملابس") || clean.contains("احذي") || clean.contains("اجهز") || clean.contains("موبايل") || clean.contains("هواتف") || clean.contains("عطور") || clean.contains("ميك اب") || clean.contains("ادوات منزلي") || clean.contains("مفروشات") || clean.contains("ذهب") || clean.contains("فضه") || clean.contains("مجوهرات") || clean.contains("بصريات") || clean.contains("نظارات") || clean.contains("ساعات") || clean.contains("العاب") || clean.contains("هدايا") || clean.contains("بقاله") || clean.contains("عطاره") || clean.contains("حلواني") || clean.contains("حلويات") || clean.contains("مخبز") || clean.contains("جزاره") || clean.contains("لحوم") || clean.contains("دواجن") || clean.contains("طيور") || clean.contains("سنتر") -> {
                categories.find { it.id == "cat_shops" }
            }
            else -> null
        }
    }

    /**
     * Resolves a raw specialty/subcategory string for a given category into a standard string.
     */
    fun resolveSpecialty(category: CategoryItem, rawSpecialty: String, nameHint: String = ""): String {
        val clean = normalizeArabic(rawSpecialty)
        if (clean.isBlank()) {
            if (nameHint.isNotBlank()) {
                val cleanName = normalizeArabic(nameHint)
                category.subcategories.find {
                    val subClean = normalizeArabic(it.nameAr)
                    cleanName.contains(subClean) || it.keywords.any { kw -> cleanName.contains(normalizeArabic(kw)) }
                }?.let { return it.nameAr }
            }
            return category.subcategories.firstOrNull()?.nameAr ?: "${category.nameAr} عام"
        }

        // Direct or contains match on subcategory names
        category.subcategories.find {
            val subClean = normalizeArabic(it.nameAr)
            subClean == clean || subClean.contains(clean) || clean.contains(subClean)
        }?.let { return it.nameAr }

        // Keyword match inside subcategories
        category.subcategories.find { sub ->
            sub.keywords.any { kw ->
                val kwClean = normalizeArabic(kw)
                kwClean == clean || kwClean.contains(clean) || clean.contains(kwClean)
            }
        }?.let { return it.nameAr }

        // Doctor specialty specific synonyms
        if (category.id == "cat_doctors" || category.id == "cat_medical_centers") {
            when {
                clean == "عظام" -> return "عظام"
                clean == "قلب" -> return "قلب"
                clean == "اطفال" -> return "أطفال"
                clean.contains("مخ واعصاب") || clean.contains("مخ و اعصاب") || clean.contains("جراحه المخ") -> return "جراحة المخ والأعصاب"
                clean.contains("قلب وصدر") || clean.contains("قلب و صدر") || clean.contains("جراحه القلب") -> return "جراحة القلب والصدر"
                clean.contains("جراحه المسالك") || (clean.contains("جراحه") && clean.contains("مسالك")) -> return "جراحة المسالك البولية"
                clean.contains("جراحه العظام") || (clean.contains("جراحه") && clean.contains("عظام")) -> return "جراحة العظام"
                clean.contains("جراحه تجميل") || clean.contains("جراحه التجميل") -> return "جراحة التجميل"
                clean.contains("جراحه عامه") || clean.contains("الجراحه العامه") -> return "الجراحة العامة"
                clean.contains("صدر") || clean.contains("حساسي") || clean.contains("تنفس") || clean.contains("ربو") -> return "امراض صدر وحساسية"
                clean.contains("مسالك") || clean.contains("كلى") || clean.contains("بروستاتا") || clean.contains("حصوات") -> return "مسالك بولية"
                clean.contains("نفسي") || clean.contains("طب نفسي") || clean.contains("استشارات نفسي") -> return "الطب النفسي والعصبي"
                clean.contains("عيون") || clean.contains("رمد") || clean.contains("ليزك") || clean.contains("مياه بيضا") -> return "طب العيون"
                clean.contains("اسنان") || clean.contains("فم") || clean.contains("تقويم") || clean.contains("حشو") || clean.contains("زراع") -> return "الفم والأسنان"
                clean.contains("جلد") || clean.contains("بشر") || clean.contains("ليزر") || clean.contains("شعر") -> return "الأمراض الجلدية"
                clean.contains("باطني") || clean.contains("باطنيه عامه") || clean.contains("كبد") -> return "الأمراض الباطنية العامة"
                clean.contains("باطن") || clean.contains("سكر") || clean.contains("ضغط") -> return "باطنة وقلب"
                clean.contains("مفصل") || clean.contains("عمود فقري") || clean.contains("كسور") -> return "عظام ومفاصل"
                clean.contains("رضع") || clean.contains("حديثي الولاده") || clean.contains("مبتسرين") -> return "أطفال وحديثي الولادة"
                clean.contains("نسا") || clean.contains("توليد") || clean.contains("حمل") || clean.contains("سونار") -> return "نساء وتوليد"
                clean.contains("انف") || clean.contains("اذن") || clean.contains("حنجر") || clean.contains("لحمي") -> return "أنف وأذن وحنجرة"
                clean.contains("علاج طبيعي") || clean.contains("تأهيل") || clean.contains("فقرات") -> return "علاج طبيعي وتأهيل"
            }
        }

        // Return original cleaned if no exact match found
        return rawSpecialty.trim()
    }
}
