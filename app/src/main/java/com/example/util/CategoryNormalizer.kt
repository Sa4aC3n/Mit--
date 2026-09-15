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
        // Strict keyword matching with priority
        return when {
            // Photography Studios
            clean.contains("استوديو") || clean.contains("فوتوسيشن") || clean.contains("فوتوغرافي") || (clean.contains("تصوير") && !clean.contains("اشع") && !clean.contains("مستندات")) -> {
                categories.find { it.id == "cat_photo_studios" }
            }
            // Veterinary and Pets
            clean.contains("بيطر") || clean.contains("حيوان") || clean.contains("دراي فود") || clean.contains("طيور زين") || clean.contains("اسماك زين") || clean.contains("علف دواجن") || clean.contains("اعلاف دواجن") || clean.contains("علف مواش") || clean.contains("اعلاف مواش") || clean.contains("كتاكيت") -> {
                categories.find { it.id == "cat_veterinary" }
            }
            // Agriculture & Plant Nurseries
            clean.contains("مشتل") || clean.contains("مشاتل") || clean.contains("اسمده") || clean.contains("مبيدات") || clean.contains("كيماويات زراعي") || clean.contains("نباتات زين") || clean.contains("شتلات") -> {
                categories.find { it.id == "cat_agriculture" }
            }
            // Trade Exhibitions (Furniture, Car Showrooms, Bridal, Major Appliances)
            clean.contains("معارض") || clean.contains("معرض سيار") || clean.contains("معرض اثاث") || clean.contains("موبيليات") || clean.contains("تجهيز العرائس") || clean.contains("جهاز العروس") || clean.contains("صالونات وانتريهات") -> {
                categories.find { it.id == "cat_trade_exhibitions" }
            }
            // Beauty & Personal Care (Salons, Barber, Spa, Makeup)
            // Note: SPA distinction: "سبا" or "مساج" or "عناية بالجسم" goes to beauty care, whereas "سباكة" goes to technicians.
            clean.contains("حلاق") || clean.contains("حلاقه") || clean.contains("كوافير") || clean.contains("بيوتي سنتر") || clean.contains("ميكاب") || clean.contains("ميك اب") || clean.contains("تجميل حريمي") || clean.contains("مساج") || clean.contains("عنايه بالجسم") || clean.contains("عناية بالجسم") || (clean.contains("سبا") && !clean.contains("سباك")) -> {
                categories.find { it.id == "cat_beauty_care" }
            }
            // Laundry & Cleaning Services
            clean.contains("مغسل") || clean.contains("دراي كلين") || clean.contains("غسيل سجاد") || clean.contains("مكوجي") -> {
                categories.find { it.id == "cat_services" }
            }
            // Doctors & Clinics
            clean.contains("طبيب") || clean.contains("اطباء") || clean.contains("دكتور") || clean.contains("دكتوره") || clean.contains("عياد") || clean.contains("كشف") || clean.contains("استشاري") || clean.contains("اخصائي") -> {
                categories.find { it.id == "cat_doctors" }
            }
            // Hospitals
            clean.contains("مستشف") || clean.contains("طوارئ") || clean.contains("اسعاف") || clean.contains("عنايه مركزه") -> {
                categories.find { it.id == "cat_hospitals" }
            }
            // Medical Centers
            clean.contains("مركز طبي") || clean.contains("مجمع عيادات") || clean.contains("مجمع طبي") -> {
                categories.find { it.id == "cat_medical_centers" }
            }
            // Pharmacies
            clean.contains("صيدل") || clean.contains("دواء") || clean.contains("مخزن ادوي") || clean.contains("مخازن ادوي") || clean.contains("روشتا") || clean.contains("اجزاخان") -> {
                categories.find { it.id == "cat_pharmacies" }
            }
            // Radiology & Labs
            clean.contains("اشع") || clean.contains("تحاليل") || clean.contains("معمل") || clean.contains("معامل") || clean.contains("رنين") || clean.contains("سونار") || clean.contains("مختبر") -> {
                categories.find { it.id == "cat_radiology" }
            }
            // Restaurants
            clean.contains("مطعم") || clean.contains("مطاعم") || clean.contains("مشوي") || clean.contains("اكل") || clean.contains("وجب") || clean.contains("كريب") || clean.contains("بيتزا") || clean.contains("فطائر") || clean.contains("برجر") || clean.contains("شاورما") || clean.contains("كباب") || clean.contains("ماكولات") || clean.contains("سندوتش") || clean.contains("حلويات") || clean.contains("مخبز") || clean.contains("مخابز") -> {
                categories.find { it.id == "cat_restaurants" }
            }
            // Cafes
            clean.contains("كافي") || clean.contains("مقه") || clean.contains("قهو") || clean.contains("عصير") || clean.contains("عصائر") || clean.contains("شاي") || clean.contains("كوفي") -> {
                categories.find { it.id == "cat_cafes" }
            }
            // Schools
            clean.contains("مدرس") || clean.contains("مدارس") || clean.contains("حضان") || clean.contains("روض") || clean.contains("سنتر تعليمي") || clean.contains("دروس") || clean.contains("كي جي") -> {
                categories.find { it.id == "cat_schools" }
            }
            // Universities & Higher Institutes
            clean.contains("جامع") || clean.contains("معهد عالي") || clean.contains("معاهد عليا") || clean.contains("كلي") || clean.contains("تفهنا") -> {
                categories.find { it.id == "cat_universities" }
            }
            // Clubs & Kids Areas
            clean.contains("ناد") || clean.contains("نواد") || clean.contains("كيدز اريا") || clean.contains("kids area") || clean.contains("العاب اطفال") || clean.contains("جيم") || clean.contains("لياق") || clean.contains("رياض") || clean.contains("ملاعب") || clean.contains("فتنس") -> {
                categories.find { it.id == "cat_clubs" }
            }
            // Banks
            clean.contains("بنك") || clean.contains("بنوك") || clean.contains("صراف الي") || clean.contains("atm") || clean.contains("اهلي") || clean.contains("cib") -> {
                categories.find { it.id == "cat_banks" }
            }
            // Government Entities
            clean.contains("حكوم") || clean.contains("سجل مدني") || clean.contains("شهر عقار") || clean.contains("مجلس المدين") || clean.contains("بريد") || clean.contains("تأمين") || clean.contains("مرور") || clean.contains("تموين") || clean.contains("مكتب صح") -> {
                categories.find { it.id == "cat_government" }
            }
            // Companies (no subcategories)
            clean.contains("شرك") || clean.contains("شركات") || clean.contains("مقاولات") || clean.contains("شحن وتوصيل") || clean.contains("استيراد وتصدير") -> {
                categories.find { it.id == "cat_companies" }
            }
            // Libraries & Printing
            clean.contains("مكتب") || clean.contains("تصوير مستندات") || clean.contains("طباع") || clean.contains("كتب") || clean.contains("ادوات مدرسي") -> {
                categories.find { it.id == "cat_libraries" }
            }
            // Charities
            clean.contains("خير") || clean.contains("ايتام") || clean.contains("جمعي") || clean.contains("اهلي") || clean.contains("مساعدات") || clean.contains("زكاه") || clean.contains("صدقات") || clean.contains("رسال") || clean.contains("اورمان") -> {
                categories.find { it.id == "cat_charities" }
            }
            // Technicians & Maintenance (Plumbing, Electrical, AC, Carpentry, Painting, Appliances, Cars)
            clean.contains("سباك") || clean.contains("كهربا") || clean.contains("نجار") || clean.contains("نقاش") || clean.contains("تكييف") || clean.contains("تبريد") || clean.contains("صيان") || clean.contains("ميكانيك") || clean.contains("عفش") || clean.contains("صنايع") -> {
                categories.find { it.id == "cat_technicians" }
            }
            // Factories & Workshops
            clean.contains("مصنع") || clean.contains("ورش") || clean.contains("الومنيوم") || clean.contains("حداد") || clean.contains("كريتال") || clean.contains("مخرط") || clean.contains("مسابك") || clean.contains("سباك معادن") -> {
                categories.find { it.id == "cat_factories" }
            }
            // Syndicates
            clean.contains("نقاب") -> {
                categories.find { it.id == "cat_syndicates" }
            }
            // Event Halls & Home Services
            clean.contains("مناسب") || clean.contains("قاع") || clean.contains("فرح") || clean.contains("افراح") || clean.contains("فراش") -> {
                categories.find { it.id == "cat_home_events" }
            }
            // Shops
            clean.contains("محل") || clean.contains("ماركت") || clean.contains("ملابس") || clean.contains("احذي") || clean.contains("اجهز") || clean.contains("موبايل") || clean.contains("اتيليه") || clean.contains("عطار") || clean.contains("نظارات") || clean.contains("بصريات") || clean.contains("ستائر") || clean.contains("اقمش") || clean.contains("اكسسوارات") || clean.contains("هدايا") -> {
                categories.find { it.id == "cat_shops" }
            }
            else -> null
        }
    }

    /**
     * Resolves a raw specialty/subcategory string for a given category into a standard string.
     * Respects user constraints:
     * - "شركات" has no subcategories -> returns empty string "", never creates "عام".
     * - Distinguishes "باطنة" vs "الأمراض الباطنية العامة".
     * - Distinguishes "سباكة" vs "سبا وعناية بالجسم".
     * - Never invents an unapproved subcategory or "${category.nameAr} عام".
     */
    fun resolveSpecialty(category: CategoryItem, rawSpecialty: String, nameHint: String = ""): String {
        // If category has no subcategories (e.g. شركات), it must strictly remain empty!
        if (category.id == "cat_companies" || category.subcategories.isEmpty()) {
            return ""
        }

        val clean = normalizeArabic(rawSpecialty)
        if (clean.isBlank()) {
            if (nameHint.isNotBlank()) {
                val cleanName = normalizeArabic(nameHint)
                category.subcategories.find {
                    val subClean = normalizeArabic(it.nameAr)
                    cleanName.contains(subClean) || it.keywords.any { kw -> cleanName.contains(normalizeArabic(kw)) }
                }?.let { return it.nameAr }
            }
            // Return first approved subcategory if present, never invent "عام"
            return category.subcategories.firstOrNull()?.nameAr ?: ""
        }

        // Specific disambiguation rules:
        // 1. Doctors & Clinics: "باطنة" vs "الأمراض الباطنية العامة"
        if (category.id == "cat_doctors") {
            if (clean.contains("عام") || clean.contains("الامراض الباطنيه العامه") || clean.contains("باطنيه عامه")) {
                return "الأمراض الباطنية العامة"
            }
            if (clean == "باطنه" || clean == "باطنة" || clean == "باطني" || clean.contains("باطن")) {
                return "باطنة"
            }
            if (clean == "عظام" || clean.contains("عظام")) return "عظام"
            if (clean == "قلب" || clean.contains("قلب")) return "قلب"
            if (clean == "اطفال" || clean == "أطفال") return "أطفال"
            if (clean.contains("حديثي الولاد") || clean.contains("مبتسرين") || clean.contains("رضع")) return "أطفال وحديثي الولادة"
            if (clean.contains("جراح")) return "جراحة"
            if (clean.contains("جلد")) return "الأمراض الجلدية"
            if (clean.contains("اسنان") || clean.contains("فم")) return "الفم والأسنان"
            if (clean.contains("نسا") || clean.contains("توليد") || clean.contains("حمل")) return "نساء وتوليد"
            if (clean.contains("انف") || clean.contains("اذن") || clean.contains("حنجر")) return "أنف وأذن وحنجرة"
            if (clean.contains("مسالك") || clean.contains("كلى") || clean.contains("بروستاتا")) return "مسالك بولية"
            if (clean.contains("صدر") || clean.contains("حساسي") || clean.contains("تنفس") || clean.contains("ربو")) return "امراض صدر وحساسية"
            if (clean.contains("نفسي") || clean.contains("عصبي") || clean.contains("مخ")) return "الطب النفسي والعصبي"
            if (clean.contains("عيون") || clean.contains("رمد") || clean.contains("ليزك")) return "طب العيون"
        }

        // 2. Technicians: "سباكة"
        if (category.id == "cat_technicians") {
            if (clean.contains("سباك") || clean.contains("سباكه") || clean.contains("سباكة")) {
                return "سباكة"
            }
        }

        // 3. Beauty & Personal Care: "سبا وعناية بالجسم"
        if (category.id == "cat_beauty_care") {
            if (clean.contains("سبا") || clean.contains("spa") || clean.contains("مساج") || clean.contains("عنايه بالجسم") || clean.contains("عناية بالجسم") || clean.contains("حمام مغربي")) {
                return "سبا وعناية بالجسم"
            }
        }

        // Direct exact or contains match on subcategory names
        category.subcategories.find {
            val subClean = normalizeArabic(it.nameAr)
            subClean == clean
        }?.let { return it.nameAr }

        category.subcategories.find {
            val subClean = normalizeArabic(it.nameAr)
            subClean.contains(clean) || clean.contains(subClean)
        }?.let { return it.nameAr }

        // Keyword match inside subcategories
        category.subcategories.find { sub ->
            sub.keywords.any { kw ->
                val kwClean = normalizeArabic(kw)
                kwClean == clean || kwClean.contains(clean) || clean.contains(kwClean)
            }
        }?.let { return it.nameAr }

        // If no match found among approved subcategories, fallback to the first approved subcategory or empty string
        return category.subcategories.firstOrNull()?.nameAr ?: ""
    }
}
