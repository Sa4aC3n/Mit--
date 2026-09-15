package com.example.data.engine

import com.example.data.model.CategoryItem
import com.example.data.model.QueryPriority
import com.example.data.model.seed.InitialDataSeed
import com.example.util.ArabicNormalizer

data class SearchQueryPlan(
    val queryText: String,
    val categoryId: String,
    val categoryName: String,
    val targetArea: String,
    val subcategory: String? = null,
    val specialtyOrService: String? = null,
    val language: String = "AR", // "AR", "EN"
    val priority: Int = 1 // 1: HIGH, 2: MEDIUM, 3: LOW
)

object SearchPlanner {

    private val executedQueriesCache = mutableSetOf<String>()

    // English town variations for search engines
    private val englishTownVariations = listOf(
        "Mit Ghamr",
        "Meet Ghamr",
        "Mit Ghamr City"
    )

    // Category Synonym Dictionaries for AI/Algorithmic Query Expansion
    private val categorySynonymMap: Map<String, List<String>> = mapOf(
        "cat_restaurants" to listOf("مطاعم", "مطعم", "مأكولات", "وجبات سريعة", "تيك أواي", "مطاعم وكافيهات", "أكل بيت", "Food", "Restaurant", "Restaurants"),
        "cat_cafes" to listOf("كافيهات", "كافيه", "مقاهي", "مقهى", "شيشة وكافيه", "Coffee", "Cafe", "Coffee Shop", "Tea Shop"),
        "cat_doctors" to listOf("عيادات", "دكتور", "أطباء", "استشاري", "عيادة تخصصية", "عيادات خارجية", "Doctors", "Clinic", "Medical Clinic"),
        "cat_medical_centers" to listOf("مراكز طبية", "مجمع عيادات", "مركز طبي تخصصي", "مركز جراحة", "Medical Center"),
        "cat_pharmacies" to listOf("صيدليات", "صيدلية", "صيدليات 24 ساعة", "دواء وطوارئ", "Pharmacy", "Pharmacies"),
        "cat_hospitals" to listOf("مستشفيات", "مستشفى", "طوارئ", "مركز طبي عام", "Hospital"),
        "cat_radiology" to listOf("معامل تحاليل", "معمل تحليل", "مراكز أشعة", "أشعة ورنين", "Medical Lab", "Radiology"),
        "cat_shops" to listOf("محلات", "متجر", "محل ملابس", "أحذية وشنط", "أدوات منزلية", "أجهزة كهربائية", "Shop", "Store"),
        "cat_technicians" to listOf("فنيين", "صنايعية", "كهربائي", "سباك", "نجار", "فني تكييف", "تصليح وصيانة", "Technician", "Maintenance"),
        "cat_automotive" to listOf("خدمات سيارات", "ميكانيكي", "كهرباء سيارات", "قطع غيار", "ورشة سيارات", "معارض سيارات", "Auto Repair", "Car Service"),
        "cat_factories" to listOf("مصانع", "ورش صناعية", "تشكيل ألومنيوم", "مصنع ملابس", "مصنع أثاث", "Factory", "Industrial"),
        "cat_companies" to listOf("شركات", "مكاتب", "مكتب محاماة", "مكتب محاسبة", "عقارات ومقاولات", "Company", "Office"),
        "cat_schools" to listOf("مدارس", "حضانات", "مراكز تعليمية", "سنتر دروس", "School", "Academy"),
        "cat_universities" to listOf("جامعات", "معاهد عليا", "جامعة الأزهر", "University", "College"),
        "cat_clubs" to listOf("نوادي", "جيم", "صالات رياضية", "ملاعب خماسية", "Gym", "Fitness Club"),
        "cat_home_events" to listOf("قاعات أفراح", "تنسيق مناسبات", "خدمات منزلية", "ديكور وتشطيبات", "Wedding Hall", "Events")
    )

    // Doctor Specialties for Targeted Specialty Search Queries
    private val doctorSpecialties = listOf(
        "دكتور قلب",
        "دكتور أطفال",
        "دكتور عظام",
        "دكتور جلدية وتجميل",
        "دكتور أسنان",
        "دكتور عيون ورمد",
        "دكتور أنف وأذن وحنجرة",
        "دكتور باطنة وجهاز هضمي",
        "دكتور نساء وتوليد",
        "دكتور مخ وأعصاب",
        "دكتور علاج طبيعي",
        "دكتور مسالك بولية",
        "دكتور جراحة عامة",
        "دكتور نفسي وعصبية"
    )

    // Technician Service Patterns
    private val technicianServicePatterns = listOf(
        "فني تكييف",
        "تصليح تكييف",
        "صيانة تكييف",
        "تركيب تكييف",
        "كهربائي منازل",
        "تأسيس كهرباء",
        "سباك صحي",
        "صيانة سباكة",
        "صيانة أجهزة كهربائية",
        "صيانة غسالات وثلاجات",
        "فني دش ورسيفر",
        "فني كاميرات مراقبة",
        "ميكانيكي سيارات",
        "كهربائي سيارات",
        "سمكري ودهان سيارات"
    )

    /**
     * Generates a Multi-Dimensional, Deduplicated, and Prioritized Search Query Plan.
     * Category × Subcategory × Area / Village / Street × Synonyms × Language (AR/EN) × Specialty / Service.
     */
    fun generateComprehensivePlan(
        targetCategoryId: String = "ALL",
        targetAreaName: String = "ALL",
        maxQueries: Int = 200,
        includeEnglish: Boolean = true
    ): List<SearchQueryPlan> {
        val categories = if (targetCategoryId == "ALL") {
            InitialDataSeed.categories
        } else {
            InitialDataSeed.categories.filter { it.id == targetCategoryId }
        }

        val locations = if (targetAreaName == "ALL") {
            MetGhamrGeoHierarchy.allLocations
        } else {
            MetGhamrGeoHierarchy.allLocations.filter { it.nameAr == targetAreaName }
        }

        val planList = mutableListOf<SearchQueryPlan>()
        val seenSignatures = mutableSetOf<String>()

        fun addPlan(
            query: String,
            catId: String,
            catName: String,
            area: String,
            sub: String? = null,
            specialty: String? = null,
            lang: String = "AR",
            priority: QueryPriority
        ) {
            val normalized = ArabicNormalizer.normalize(query.trim().lowercase())
            if (normalized.isNotBlank() && seenSignatures.add(normalized)) {
                planList.add(
                    SearchQueryPlan(
                        queryText = query.trim(),
                        categoryId = catId,
                        categoryName = catName,
                        targetArea = area,
                        subcategory = sub,
                        specialtyOrService = specialty,
                        language = lang,
                        priority = priority.weight
                    )
                )
            }
        }

        // ========================================================
        // 1. HIGH PRIORITY: Core City & Center Queries with Synonyms
        // ========================================================
        for (cat in categories) {
            val synonyms = categorySynonymMap[cat.id] ?: listOf(cat.nameAr)
            for (syn in synonyms.take(4)) {
                addPlan("$syn ميت غمر", cat.id, cat.nameAr, "مدينة ميت غمر", priority = QueryPriority.HIGH)
                addPlan("$syn في ميت غمر", cat.id, cat.nameAr, "مدينة ميت غمر", priority = QueryPriority.HIGH)
                addPlan("$syn مركز ميت غمر", cat.id, cat.nameAr, "مركز ميت غمر", priority = QueryPriority.HIGH)
            }

            // Subcategory Specific Core Queries
            for (sub in cat.subcategories) {
                addPlan("${sub.nameAr} ميت غمر", cat.id, cat.nameAr, "مدينة ميت غمر", sub = sub.nameAr, priority = QueryPriority.HIGH)
                for (kw in sub.keywords.take(2)) {
                    addPlan("$kw ميت غمر", cat.id, cat.nameAr, "مدينة ميت غمر", sub = sub.nameAr, priority = QueryPriority.HIGH)
                }
            }
        }

        // ========================================================
        // 2. HIGH / MEDIUM PRIORITY: Specialty & Service Specific
        // ========================================================
        val docCat = categories.find { it.id == "cat_doctors" }
        if (docCat != null) {
            for (spec in doctorSpecialties) {
                addPlan("$spec ميت غمر", docCat.id, docCat.nameAr, "مدينة ميت غمر", specialty = spec, priority = QueryPriority.HIGH)
                addPlan("$spec في ميت غمر", docCat.id, docCat.nameAr, "مدينة ميت غمر", specialty = spec, priority = QueryPriority.HIGH)
                addPlan("عيادة $spec ميت غمر", docCat.id, docCat.nameAr, "مدينة ميت غمر", specialty = spec, priority = QueryPriority.HIGH)
            }
        }

        val techCat = categories.find { it.id == "cat_technicians" }
        if (techCat != null) {
            for (srv in technicianServicePatterns) {
                addPlan("$srv ميت غمر", techCat.id, techCat.nameAr, "مدينة ميت غمر", specialty = srv, priority = QueryPriority.HIGH)
                addPlan("$srv في ميت غمر", techCat.id, techCat.nameAr, "مدينة ميت غمر", specialty = srv, priority = QueryPriority.HIGH)
            }
        }

        // ========================================================
        // 3. MEDIUM PRIORITY: English Cross-Lingual Queries
        // ========================================================
        if (includeEnglish) {
            for (cat in categories) {
                val englishSyns = (categorySynonymMap[cat.id] ?: emptyList()).filter { it.matches(Regex("^[a-zA-Z\\s]+$")) }
                for (engTerm in englishSyns) {
                    for (townVar in englishTownVariations) {
                        addPlan("$engTerm $townVar", cat.id, cat.nameAr, "Mit Ghamr", lang = "EN", priority = QueryPriority.MEDIUM)
                        addPlan("$engTerm in $townVar", cat.id, cat.nameAr, "Mit Ghamr", lang = "EN", priority = QueryPriority.MEDIUM)
                    }
                }
            }
        }

        // ========================================================
        // 4. MEDIUM / LOW PRIORITY: Geographic Expansion (Villages & Streets)
        // ========================================================
        val targetLocations = if (targetAreaName == "ALL") locations else locations.filter { it.nameAr == targetAreaName }
        for (loc in targetLocations) {
            for (cat in categories) {
                val p = if (loc.priority == 1) QueryPriority.MEDIUM else QueryPriority.LOW
                addPlan("${cat.nameAr} ${loc.nameAr}", cat.id, cat.nameAr, loc.nameAr, priority = p)
                addPlan("${cat.nameAr} في ${loc.nameAr}", cat.id, cat.nameAr, loc.nameAr, priority = p)

                // High priority subcategories for villages
                for (sub in cat.subcategories.take(2)) {
                    addPlan("${sub.nameAr} ${loc.nameAr}", cat.id, cat.nameAr, loc.nameAr, sub = sub.nameAr, priority = p)
                }
            }
        }

        // Filter out already executed queries in this session for cost & duplicate efficiency
        val unexecutedPlans = planList.filter { !executedQueriesCache.contains(it.queryText) }
        val finalPlans = if (unexecutedPlans.isNotEmpty()) unexecutedPlans else planList

        return finalPlans.sortedBy { it.priority }.take(maxQueries)
    }

    /**
     * Records executed query to avoid redundant future runs in the same session.
     */
    fun markQueryExecuted(queryText: String) {
        executedQueriesCache.add(queryText)
    }

    /**
     * Clears executed queries cache when fresh full-discovery is desired.
     */
    fun clearExecutedCache() {
        executedQueriesCache.clear()
    }
}

