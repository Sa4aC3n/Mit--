package com.example.data.model.seed

import com.example.data.model.BusinessEntity
import com.example.data.model.CategoryItem
import com.example.data.model.NotificationEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.SubcategoryItem

object InitialDataSeed {

    val categories = listOf(
        CategoryItem(
            id = "cat_restaurants",
            nameAr = "مطاعم",
            iconName = "Restaurant",
            count = 12,
            colorHex = 0xFFE63946,
            description = "مشويات، كريب، وجبات سريعة، وأسماك طازجة",
            isFeatured = true,
            sortOrder = 1,
            subcategories = listOf(
                SubcategoryItem("sub_rest_1", "cat_restaurants", "مأكولات شرقية ومشويات", listOf("كباب", "كفتة", "لحوم")),
                SubcategoryItem("sub_rest_2", "cat_restaurants", "وجبات سريعة وكريب", listOf("برجر", "كريب", "سندوتشات")),
                SubcategoryItem("sub_rest_3", "cat_restaurants", "مأكولات بحرية وأسماك", listOf("سمك", "جمبري", "طواجن")),
                SubcategoryItem("sub_rest_4", "cat_restaurants", "بيتزا وفطائر", listOf("بيتزا", "فطير", "إيطالي")),
                SubcategoryItem("sub_rest_5", "cat_restaurants", "حلويات شرقية وغربية", listOf("بسبوسة", "كيك", "آيس كريم"))
            )
        ),
        CategoryItem(
            id = "cat_doctors",
            nameAr = "عيادات وأطباء",
            iconName = "MedicalServices",
            count = 15,
            colorHex = 0xFF457B9D,
            description = "أطباء واستشاريين لكافة التخصصات الطبية",
            isFeatured = true,
            sortOrder = 2,
            subcategories = listOf(
                SubcategoryItem("sub_doc_1", "cat_doctors", "عظام ومفاصل", listOf("كسور", "مفاصل", "عمود فقري", "غضروف", "خشونة")),
                SubcategoryItem("sub_doc_2", "cat_doctors", "باطنة وقلب", listOf("ضغط", "سكر", "قلب", "أوعية دموية")),
                SubcategoryItem("sub_doc_3", "cat_doctors", "أطفال وحديثي الولادة", listOf("أطفال", "حديثي الولادة", "تطعيمات", "نمو", "مبتسرين")),
                SubcategoryItem("sub_doc_4", "cat_doctors", "نساء وتوليد", listOf("نساء", "توليد", "حمل", "ولادة", "سونار", "عقم")),
                SubcategoryItem("sub_doc_5", "cat_doctors", "الفم والأسنان", listOf("أسنان", "فم", "تقويم", "حشو", "زراعة", "تبييض")),
                SubcategoryItem("sub_doc_6", "cat_doctors", "الأمراض الجلدية", listOf("جلدية", "أمراض جلدية", "بشرة", "ليزر", "شعر", "حساسية")),
                SubcategoryItem("sub_doc_7", "cat_doctors", "جراحة التجميل", listOf("تجميل", "جراحة تجميل", "نحت", "شفط", "شد", "ليزر")),
                SubcategoryItem("sub_doc_8", "cat_doctors", "أنف وأذن وحنجرة", listOf("أنف", "أذن", "حنجرة", "جيوب أنفية", "لحمية", "سمعيات")),
                SubcategoryItem("sub_doc_9", "cat_doctors", "الأمراض الباطنية العامة", listOf("باطنة عامة", "أمراض باطنية", "كبد", "جهاز هضمي", "مناعة")),
                SubcategoryItem("sub_doc_10", "cat_doctors", "عظام", listOf("عظام", "كسور", "جبس", "مفصل")),
                SubcategoryItem("sub_doc_11", "cat_doctors", "امراض صدر وحساسية", listOf("صدر", "حساسية", "ربو", "تنفس", "رئة", "كحة")),
                SubcategoryItem("sub_doc_12", "cat_doctors", "مسالك بولية", listOf("مسالك", "كلى", "حصوات", "بروستاتا", "مثانة")),
                SubcategoryItem("sub_doc_13", "cat_doctors", "قلب", listOf("قلب", "أوعية دموية", "قسطرة", "رسم قلب", "إيكو")),
                SubcategoryItem("sub_doc_14", "cat_doctors", "الطب النفسي والعصبي", listOf("نفسي", "عصبي", "مخ وأعصاب", "توتر", "اكتئاب", "علاج نفسي")),
                SubcategoryItem("sub_doc_15", "cat_doctors", "أطفال", listOf("أطفال", "تغذية أطفال", "كشف أطفال")),
                SubcategoryItem("sub_doc_16", "cat_doctors", "الجراحة العامة", listOf("جراحة عامة", "مناظير", "فتق", "مرارة", "استئصال")),
                SubcategoryItem("sub_doc_17", "cat_doctors", "طب العيون", listOf("عيون", "رمد", "ليزك", "نظارات", "مياه بيضاء", "شبكية")),
                SubcategoryItem("sub_doc_18", "cat_doctors", "جراحة العظام", listOf("جراحة عظام", "مناظير مفاصل", "تغيير مفاصل", "رباط صليبي")),
                SubcategoryItem("sub_doc_19", "cat_doctors", "جراحة المخ والأعصاب", listOf("جراحة مخ وأعصاب", "عمود فقري", "انزلاق غضروفي", "أعصاب")),
                SubcategoryItem("sub_doc_20", "cat_doctors", "جراحة القلب والصدر", listOf("جراحة قلب", "جراحة صدر", "قلب مفتوح", "شرايين")),
                SubcategoryItem("sub_doc_21", "cat_doctors", "جراحة المسالك البولية", listOf("جراحة مسالك", "مناظير كلى", "تفتيت حصوات", "ذكورة وعقم"))
            )
        ),
        CategoryItem(
            id = "cat_medical_centers",
            nameAr = "مراكز طبية",
            iconName = "MedicalServices",
            count = 8,
            colorHex = 0xFF0077B6,
            description = "مراكز طبية متخصصة، مجمعات عيادات، ومراكز جراحة اليوم الواحد",
            isFeatured = true,
            sortOrder = 3,
            subcategories = listOf(
                SubcategoryItem("sub_medc_1", "cat_medical_centers", "مجمعات عيادات تخصصية", listOf("مجمع عيادات", "استشاريين", "تخصصات")),
                SubcategoryItem("sub_medc_2", "cat_medical_centers", "مراكز عيون وجراحة ليزك", listOf("عيون", "نظارات", "ليزك", "مياه بيضاء")),
                SubcategoryItem("sub_medc_3", "cat_medical_centers", "مراكز علاج طبيعي وتأهيل", listOf("علاج طبيعي", "تأهيل", "فقرات", "إصابات"))
            )
        ),
        CategoryItem(
            id = "cat_pharmacies",
            nameAr = "صيدليات",
            iconName = "LocalPharmacy",
            count = 11,
            colorHex = 0xFF00B4D8,
            description = "صيدليات تعمل على مدار 24 ساعة وخدمة توصيل الأدوية",
            isFeatured = true,
            sortOrder = 4,
            subcategories = listOf(
                SubcategoryItem("sub_pharm_1", "cat_pharmacies", "صيدليات 24 ساعة وطوارئ", listOf("طوارئ", "ليلية", "دواء", "توصيل")),
                SubcategoryItem("sub_pharm_2", "cat_pharmacies", "مستلزمات أطفال وألبان", listOf("لبن أطفال", "حفاضات", "رعاية")),
                SubcategoryItem("sub_pharm_3", "cat_pharmacies", "مستحضرات تجميل وعناية", listOf("بشرة", "واقي شمس", "ميك أب"))
            )
        ),
        CategoryItem(
            id = "cat_government",
            nameAr = "مصالح حكومية",
            iconName = "AccountBalance",
            count = 9,
            colorHex = 0xFF3A0CA3,
            description = "مجلس المدينة، السجل المدني، الشهر العقاري، البريد، ومكاتب الصحة والتموين",
            isFeatured = true,
            sortOrder = 5,
            subcategories = listOf(
                SubcategoryItem("sub_gov_1", "cat_government", "مجلس المدينة والوحدات المحلية", listOf("مجلس المدينة", "وحدة محلية", "تنظيم", "تراخيص")),
                SubcategoryItem("sub_gov_2", "cat_government", "سجل مدني وشهر عقاري", listOf("سجل مدني", "بطاقات", "شهر عقاري", "توثيق")),
                SubcategoryItem("sub_gov_3", "cat_government", "مكاتب البريد والتأمينات", listOf("بريد", "معاشات", "تأمينات اجتماعية", "حوالات")),
                SubcategoryItem("sub_gov_4", "cat_government", "مكاتب التموين والمرور والصحة", listOf("تموين", "مرور", "رخص", "صحة", "مكتب صحة"))
            )
        ),
        CategoryItem(
            id = "cat_banks",
            nameAr = "بنوك",
            iconName = "AccountBalanceWallet",
            count = 7,
            colorHex = 0xFF023E8A,
            description = "البنوك الوطنية والاستثمارية، ماكينات ATM، وشركات الصرافة",
            isFeatured = true,
            sortOrder = 6,
            subcategories = listOf(
                SubcategoryItem("sub_bank_1", "cat_banks", "فروع البنوك الرئيسية", listOf("البنك الأهلي", "بنك مصر", "بنك القاهرة", "CIB", "البنك الزراعي")),
                SubcategoryItem("sub_bank_2", "cat_banks", "ماكينات صراف آلي ATM", listOf("ATM", "صراف آلي", "سحب وإيداع")),
                SubcategoryItem("sub_bank_3", "cat_banks", "شركات صرافة وتحويل أموال", listOf("صرافة", "تحويل أموال", "دولار", "ويسترن يونيون"))
            )
        ),
        CategoryItem(
            id = "cat_universities",
            nameAr = "جامعات",
            iconName = "School",
            count = 5,
            colorHex = 0xFF1D3557,
            description = "جامعة الأزهر فرع تفهنا الأشراف، المعاهد العليا والكليات التخصصية",
            isFeatured = true,
            sortOrder = 7,
            subcategories = listOf(
                SubcategoryItem("sub_univ_1", "cat_universities", "جامعة الأزهر تفهنا الأشراف", listOf("الأزهر", "شريعة وقانون", "تربية", "تجارة", "تفهنا")),
                SubcategoryItem("sub_univ_2", "cat_universities", "معاهد عليا وكليات", listOf("معهد عالي", "هندسة", "تكنولوجيا", "دراسات عليا"))
            )
        ),
        CategoryItem(
            id = "cat_schools",
            nameAr = "مدارس",
            iconName = "School",
            count = 12,
            colorHex = 0xFF457B9D,
            description = "مدارس رسمية لغات، رسمية، معاهد أزهرية، ومدارس خاصة",
            isFeatured = true,
            sortOrder = 8,
            subcategories = listOf(
                SubcategoryItem("sub_sch_1", "cat_schools", "مدارس رسمية لغات ورسمية", listOf("لغات", "تجريبي", "ابتدائي", "إعدادي", "ثانوي")),
                SubcategoryItem("sub_sch_2", "cat_schools", "معاهد أزهرية", listOf("أزهر", "معهد أزهري", "ابتدائي أزهري", "ثانوي أزهري")),
                SubcategoryItem("sub_sch_3", "cat_schools", "مدارس خاصة وفنية", listOf("خاصة", "صنايع", "تمريض", "تجارة"))
            )
        ),
        CategoryItem(
            id = "cat_libraries",
            nameAr = "مكتبات",
            iconName = "MenuBook",
            count = 10,
            colorHex = 0xFF2A9D8F,
            description = "مكتبات أدوات مدرسية، أدوات جامعية، طباعة ونسخ مستندات وكتب",
            isFeatured = true,
            sortOrder = 9,
            subcategories = listOf(
                SubcategoryItem("sub_lib_1", "cat_libraries", "أدوات مدرسية وجامعية", listOf("أدوات مكتبية", "كشكول", "أقلام", "شنط")),
                SubcategoryItem("sub_lib_2", "cat_libraries", "طباعة وتصوير وخدمات طابعات", listOf("تصوير", "طباعة", "ملزمات", "مجلات")),
                SubcategoryItem("sub_lib_3", "cat_libraries", "بيع كتب وروايات", listOf("كتب", "روايات", "قصص", "دينية"))
            )
        ),
        CategoryItem(
            id = "cat_shops",
            nameAr = "محلات",
            iconName = "Storefront",
            count = 18,
            colorHex = 0xFF7209B7,
            description = "محلات ملابس، أحذية، أدوات منزلية، أجهزة ومواد غذائية",
            isFeatured = true,
            sortOrder = 10,
            subcategories = listOf(
                SubcategoryItem("sub_shop_1", "cat_shops", "سوبرماركت ومواد غذائية", listOf("بقالة", "تموين", "ماركت", "ألبان")),
                SubcategoryItem("sub_shop_2", "cat_shops", "ملابس وأحذية ونوفوتيه", listOf("رجالي", "حريمي", "أطفال", "أحذية", "شنط")),
                SubcategoryItem("sub_shop_3", "cat_shops", "أدوات منزلية وأجهزة كهربائية", listOf("أجهزة", "مطبخ", "أدوات منزلية")),
                SubcategoryItem("sub_shop_4", "cat_shops", "موبايلات وإلكترونيات", listOf("موبايل", "صيانة", "اكسسوارات"))
            )
        ),
        CategoryItem(
            id = "cat_companies",
            nameAr = "شركات",
            iconName = "Business",
            count = 9,
            colorHex = 0xFF14213D,
            description = "شركات المقاولات، الشحن والتوصيل، البرمجيات والدعاية والإعلان",
            isFeatured = true,
            sortOrder = 11,
            subcategories = listOf(
                SubcategoryItem("sub_comp_1", "cat_companies", "شركات مقاولات وديكور وعقارات", listOf("مقاولات", "تشطيبات", "ديكور", "عقارات")),
                SubcategoryItem("sub_comp_2", "cat_companies", "شركات شحن ونقل بضائع", listOf("شحن", "توصيل", "بضائع", "نقل")),
                SubcategoryItem("sub_comp_3", "cat_companies", "شركات برمجيات ودعاية وتسويق", listOf("برمجيات", "تسويق", "دعاية", "إعلان", "تصميم"))
            )
        ),
        CategoryItem(
            id = "cat_syndicates",
            nameAr = "نقابات",
            iconName = "Groups",
            count = 6,
            colorHex = 0xFF6A040F,
            description = "نقابة المهندسين، المعلمين، التجاريين، والأطباء والصيادلة والفرعيات",
            isFeatured = true,
            sortOrder = 12,
            subcategories = listOf(
                SubcategoryItem("sub_syn_1", "cat_syndicates", "نقابة المهندسين والفرعيات", listOf("مهندسين", "كارنيه", "مشروع علاج")),
                SubcategoryItem("sub_syn_2", "cat_syndicates", "نقابة المعلمين والتجاريين", listOf("معلمين", "تجاريين", "معاشات")),
                SubcategoryItem("sub_syn_3", "cat_syndicates", "نقابة الأطباء والصيادلة والأسنان", listOf("أطباء", "صيادلة", "علاج"))
            )
        ),
        CategoryItem(
            id = "cat_charities",
            nameAr = "جمعيات خيرية ومؤسسات أهلية",
            iconName = "VolunteerActivism",
            count = 8,
            colorHex = 0xFF2B9348,
            description = "الجمعيات الخيرية، كفالة الأيتام، المساعدات الاجتماعية والمؤسسات الأهلية",
            isFeatured = true,
            sortOrder = 13,
            subcategories = listOf(
                SubcategoryItem("sub_char_1", "cat_charities", "جمعيات كفالة الأيتام والمساعدات", listOf("أيتام", "كفالة", "إطعام", "مساعدات", "صدقات")),
                SubcategoryItem("sub_char_2", "cat_charities", "مؤسسات أهلية وتنمية مجتمع", listOf("تنمية مجتمع", "خدمات أهلية", "تدريب")),
                SubcategoryItem("sub_char_3", "cat_charities", "مستوصفات خيرية وبنوك طعام", listOf("مستوصف خيري", "علاج خيري", "بنك طعام"))
            )
        ),
        CategoryItem(
            id = "cat_technicians",
            nameAr = "فنيين وصنايعية",
            iconName = "Handyman",
            count = 14,
            colorHex = 0xFFE76F51,
            description = "كهربائي، سباك، نجار، فني تكييف وأجهزة",
            isFeatured = false,
            sortOrder = 14,
            subcategories = listOf(
                SubcategoryItem("sub_tech_1", "cat_technicians", "فني كهرباء منازل", listOf("كهربائي", "تأسيس", "إضاءة")),
                SubcategoryItem("sub_tech_2", "cat_technicians", "سباكة وصيانة شبكات", listOf("سباك", "خلاطات", "تسريب")),
                SubcategoryItem("sub_tech_3", "cat_technicians", "صيانة تكييفات وتبريد", listOf("تكييف", "فريون", "شحن")),
                SubcategoryItem("sub_tech_4", "cat_technicians", "نجارة وأثاث", listOf("نجار", "غرف", "مطبخ")),
                SubcategoryItem("sub_tech_5", "cat_technicians", "نقاشة وطلاء", listOf("نقاش", "ديكور", "دهان")),
                SubcategoryItem("sub_tech_6", "cat_technicians", "صيانة أجهزة منزلية", listOf("غسالات", "ثلاجات", "بوتاجاز"))
            )
        ),
        CategoryItem(
            id = "cat_factories",
            nameAr = "مصانع ورش",
            iconName = "Factory",
            count = 10,
            colorHex = 0xFF264653,
            description = "مصانع ألومنيوم، ورش حدادة وخراطة ومسابك",
            isFeatured = false,
            sortOrder = 15,
            subcategories = listOf(
                SubcategoryItem("sub_fact_1", "cat_factories", "مصانع وتشكيل ألومنيوم", listOf("ألومنيوم", "قطاعات")),
                SubcategoryItem("sub_fact_2", "cat_factories", "ورش حدادة وكريتال", listOf("حديد", "بوابات")),
                SubcategoryItem("sub_fact_3", "cat_factories", "خراطة ومسابك ألومنيوم", listOf("مخرطة", "سباكة"))
            )
        ),
        CategoryItem(
            id = "cat_cafes",
            nameAr = "كافيهات",
            iconName = "Coffee",
            count = 9,
            colorHex = 0xFFD4A373,
            description = "كافيهات عائلية وجلسات شبابية ومشروبات طازجة",
            isFeatured = false,
            sortOrder = 16,
            subcategories = listOf(
                SubcategoryItem("sub_cafe_1", "cat_cafes", "كافيهات عائلية", listOf("عائلات", "هدوء")),
                SubcategoryItem("sub_cafe_2", "cat_cafes", "عصائر وفرش", listOf("قصب", "مانجو", "كوكتيل"))
            )
        ),
        CategoryItem(
            id = "cat_hospitals",
            nameAr = "مستشفيات",
            iconName = "LocalHospital",
            count = 6,
            colorHex = 0xFF1D3557,
            description = "مستشفيات عامة وخاصة ومراكز طوارئ",
            isFeatured = false,
            sortOrder = 17,
            subcategories = listOf(
                SubcategoryItem("sub_hosp_1", "cat_hospitals", "مستشفيات خاصة", listOf("عناية", "عمليات")),
                SubcategoryItem("sub_hosp_2", "cat_hospitals", "مراكز طوارئ وإسعاف", listOf("طوارئ", "حوادث"))
            )
        ),
        CategoryItem(
            id = "cat_radiology",
            nameAr = "مراكز أشعة وتحاليل",
            iconName = "Biotech",
            count = 8,
            colorHex = 0xFF2A9D8F,
            description = "معامل تحاليل ومراكز أشعة مقطعية وتلفزيونية",
            isFeatured = false,
            sortOrder = 18,
            subcategories = listOf(
                SubcategoryItem("sub_rad_1", "cat_radiology", "معامل تحاليل طبية", listOf("دم", "سوق")),
                SubcategoryItem("sub_rad_2", "cat_radiology", "مراكز أشعة ورنين", listOf("رنين", "مقطعية"))
            )
        ),
        CategoryItem(
            id = "cat_automotive",
            nameAr = "خدمات سيارات",
            iconName = "DirectionsCar",
            count = 7,
            colorHex = 0xFF3F37C9,
            description = "ميكانيكي، كهربائي سيارات، قطع غيار ومغاسل",
            isFeatured = false,
            sortOrder = 19,
            subcategories = listOf(
                SubcategoryItem("sub_auto_1", "cat_automotive", "ميكانيكا وسرفيس سيارات", listOf("عفشة", "محرك")),
                SubcategoryItem("sub_auto_2", "cat_automotive", "قطع غيار واكسسوارات", listOf("زيت", "فلاتر"))
            )
        ),
        CategoryItem(
            id = "cat_clubs",
            nameAr = "نوادي ومراكز رياضية",
            iconName = "FitnessCenter",
            count = 5,
            colorHex = 0xFFF4A261,
            description = "جيم، صالات لياقة بدنية، وملاعب خماسية",
            isFeatured = false,
            sortOrder = 20,
            subcategories = listOf(
                SubcategoryItem("sub_club_1", "cat_clubs", "صالات جيم كمال أجسام", listOf("حديد", "فتنس")),
                SubcategoryItem("sub_club_2", "cat_clubs", "ملاعب خماسية كورة", listOf("حجز", "نجيل"))
            )
        ),
        CategoryItem(
            id = "cat_home_events",
            nameAr = "خدمات منزلية ومناسبات",
            iconName = "Event",
            count = 6,
            colorHex = 0xFFF72585,
            description = "قاعات أفراح، فراشة، وتنسيق مناسبات",
            isFeatured = false,
            sortOrder = 21,
            subcategories = listOf(
                SubcategoryItem("sub_evt_1", "cat_home_events", "قاعات أفراح ومناسبات", listOf("زفاف", "خطوبة"))
            )
        )
    )

    val businesses: List<BusinessEntity> = ComprehensiveMetGhamrDirectory.businesses

    val reviews = listOf(
        ReviewEntity(
            id = "rev_1",
            businessId = "b_rest_1",
            userId = "u_1",
            userName = "محمد كشك",
            userAvatarUrl = "https://lh3.googleusercontent.com/a/default-user",
            userProvider = "GOOGLE",
            rating = 5.0f,
            comment = "ما شاء الله المشويات عندهم طازة وطعمها ممتازة جداً والتوصيل سريع لميت غمر صهرجت.",
            status = "APPROVED",
            timestamp = System.currentTimeMillis() - 86400000L
        ),
        ReviewEntity(
            id = "rev_2",
            businessId = "b_doc_1",
            userId = "u_2",
            userName = "مهندس إسلام السعيد",
            userAvatarUrl = "https://platform-lookaside.fbsbx.com/platform/profilepic/",
            userProvider = "FACEBOOK",
            rating = 5.0f,
            comment = "دكتور ممتاز وصبور جداً في الشرح والتشخيص دقيق، عافانا الله وإياكم.",
            status = "APPROVED",
            timestamp = System.currentTimeMillis() - 172800000L
        ),
        ReviewEntity(
            id = "rev_3",
            businessId = "b_tech_1",
            userId = "u_3",
            userName = "دكتورة سارة محمود",
            userAvatarUrl = null,
            userProvider = "MICROSOFT",
            rating = 5.0f,
            comment = "الأسطى إبراهيم جاء في الميعاد بالضبط وصلح الغسالة الفول أوتوماتيك بأمانة وقطع غيار أصلية.",
            status = "APPROVED",
            timestamp = System.currentTimeMillis() - 259200000L
        )
    )

    val notifications = listOf(
        NotificationEntity(
            id = "notif_1",
            title = "مرحباً بك في دليل ميت غمر! 🌟",
            body = "تم تحديث وتوسيع قاعدة بيانات ميت غمر بكافة الأنشطة والخدمات الطبية والمهنية والتجارية.",
            categoryId = null,
            businessId = null,
            timestamp = System.currentTimeMillis() - 3600000L,
            isRead = false
        ),
        NotificationEntity(
            id = "notif_2",
            title = "تحديث خدمات الطوارئ 🚑",
            body = "مستشفى ميت غمر العام ومستشفى الشفاء والصيدليات 24 ساعة متاحة الآن للاتصال المباشر.",
            categoryId = "cat_hospitals",
            businessId = "b_hosp_1",
            timestamp = System.currentTimeMillis() - 86400000L,
            isRead = true
        )
    )
}
