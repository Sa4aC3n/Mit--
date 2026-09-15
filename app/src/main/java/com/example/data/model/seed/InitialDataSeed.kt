package com.example.data.model.seed

import com.example.data.model.BusinessEntity
import com.example.data.model.CategoryItem
import com.example.data.model.NotificationEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.SubcategoryItem

object InitialDataSeed {

    val categories = listOf(
        CategoryItem(
            id = "cat_photo_studios",
            nameAr = "استوديوهات تصوير",
            iconName = "CameraAlt",
            count = 6,
            colorHex = 0xFF7C3AED,
            description = "استوديوهات تصوير فوتوغرافي، سيشن أفراح، وتصوير مناسبات",
            isFeatured = false,
            sortOrder = 1,
            subcategories = listOf(
                SubcategoryItem("sub_photo_1", "cat_photo_studios", "استوديوهات تصوير", listOf("تصوير", "استوديو", "فوتوسيشن", "فوتوغرافي", "كاميرا", "أفراح"))
            )
        ),
        CategoryItem(
            id = "cat_veterinary",
            nameAr = "الحيوانات والطب البيطري",
            iconName = "Pets",
            count = 8,
            colorHex = 0xFF059669,
            description = "عيادات بيطرية، رعاية الحيوانات الأليفة، وأعلاف الدواجن والمواشي",
            isFeatured = false,
            sortOrder = 2,
            subcategories = listOf(
                SubcategoryItem("sub_vet_1", "cat_veterinary", "عيادات بيطرية", listOf("بيطري", "طبيب بيطري", "عيادة بيطرية", "علاج حيوانات", "تطعيمات")),
                SubcategoryItem("sub_vet_2", "cat_veterinary", "مستلزمات الحيوانات الأليفة", listOf("دراي فود", "مستلزمات قطط", "مستلزمات كلاب", "رعاية")),
                SubcategoryItem("sub_vet_3", "cat_veterinary", "محلات الحيوانات الأليفة", listOf("قطط", "كلاب", "حيوانات أليفة")),
                SubcategoryItem("sub_vet_4", "cat_veterinary", "طيور وأسماك الزينة", listOf("عصافير", "حمام", "أسماك زينة", "أحواض سمك")),
                SubcategoryItem("sub_vet_5", "cat_veterinary", "أعلاف ومستلزمات الدواجن", listOf("أعلاف دواجن", "علف فراخ", "كتاكيت", "تحصينات دواجن")),
                SubcategoryItem("sub_vet_6", "cat_veterinary", "أعلاف ومواشي", listOf("علف مواشي", "ردة", "ذرة", "أعلاف مواشي وتسمين"))
            )
        ),
        CategoryItem(
            id = "cat_agriculture",
            nameAr = "الزراعة والثروة الحيوانية",
            iconName = "Park",
            count = 6,
            colorHex = 0xFF16A34A,
            description = "مشاتل زهور ونباتات، أسمدة، مبيدات زراعية ومستلزمات إنتاج",
            isFeatured = false,
            sortOrder = 3,
            subcategories = listOf(
                SubcategoryItem("sub_agri_1", "cat_agriculture", "مشاتل وزهور", listOf("مشتل", "زهور", "نباتات زينة", "أشجار", "شتلات", "لاندسكيب")),
                SubcategoryItem("sub_agri_2", "cat_agriculture", "أسمدة ومبيدات", listOf("أسمدة", "مبيدات", "كيماويات زراعية", "مخصبات", "بذور"))
            )
        ),
        CategoryItem(
            id = "cat_banks",
            nameAr = "بنوك",
            iconName = "AccountBalanceWallet",
            count = 7,
            colorHex = 0xFF023E8A,
            description = "البنوك الوطنية والاستثمارية، وماكينات الصراف الآلي",
            isFeatured = true,
            sortOrder = 4,
            subcategories = listOf(
                SubcategoryItem("sub_bank_1", "cat_banks", "فروع البنوك الرئيسية", listOf("البنك الأهلي", "بنك مصر", "بنك القاهرة", "CIB", "البنك الزراعي", "صراف آلي", "حسابات", "قروض"))
            )
        ),
        CategoryItem(
            id = "cat_universities",
            nameAr = "جامعات",
            iconName = "School",
            count = 5,
            colorHex = 0xFF1D3557,
            description = "جامعة الأزهر فرع تفهنا الأشراف والمعاهد العليا والكليات",
            isFeatured = true,
            sortOrder = 5,
            subcategories = listOf(
                SubcategoryItem("sub_univ_1", "cat_universities", "جامعة الأزهر تفهنا الأشراف", listOf("جامعة الأزهر", "تفهنا الأشراف", "شريعة وقانون", "تربية أزهر", "تجارة أزهر")),
                SubcategoryItem("sub_univ_2", "cat_universities", "معاهد عليا وكليات", listOf("معهد عالي", "كلية", "دراسات عليا", "أكاديمية", "معاهد تكنولوجية"))
            )
        ),
        CategoryItem(
            id = "cat_charities",
            nameAr = "جمعيات خيرية ومؤسسات أهلية",
            iconName = "VolunteerActivism",
            count = 8,
            colorHex = 0xFF2B9348,
            description = "الجمعيات الخيرية، كفالة الأيتام، المستوصفات الخيرية وبنوك الطعام",
            isFeatured = true,
            sortOrder = 6,
            subcategories = listOf(
                SubcategoryItem("sub_char_1", "cat_charities", "جمعيات كفالة الأيتام والمساعدات", listOf("كفالة أيتام", "مساعدات", "صدقات", "إغاثة", "زكاة")),
                SubcategoryItem("sub_char_2", "cat_charities", "مؤسسات أهلية وتنمية مجتمع", listOf("مؤسسة أهلية", "تنمية مجتمع", "تأهيل وتدريب", "خدمات مجتمعية")),
                SubcategoryItem("sub_char_3", "cat_charities", "مستوصفات خيرية وبنوك طعام", listOf("مستوصف خيري", "بنك طعام", "إطعام", "علاج مجاني"))
            )
        ),
        CategoryItem(
            id = "cat_home_events",
            nameAr = "خدمات منزلية ومناسبات",
            iconName = "Event",
            count = 6,
            colorHex = 0xFFF72585,
            description = "قاعات أفراح ومناسبات وتنسيق الحفلات والمؤتمرات",
            isFeatured = false,
            sortOrder = 7,
            subcategories = listOf(
                SubcategoryItem("sub_evt_1", "cat_home_events", "قاعات أفراح ومناسبات", listOf("قاعة أفراح", "مناسبات", "خطوبة", "زفاف", "حفلات", "فراشة"))
            )
        ),
        CategoryItem(
            id = "cat_companies",
            nameAr = "شركات",
            iconName = "Business",
            count = 9,
            colorHex = 0xFF14213D,
            description = "شركات مقاولات، شحن وتوصيل، استيراد وتصدير وبرمجيات",
            isFeatured = true,
            sortOrder = 8,
            subcategories = emptyList()
        ),
        CategoryItem(
            id = "cat_pharmacies",
            nameAr = "صيدليات",
            iconName = "LocalPharmacy",
            count = 11,
            colorHex = 0xFF00B4D8,
            description = "صيدليات، مخازن أدوية، وصيدليات 24 ساعة وطوارئ",
            isFeatured = true,
            sortOrder = 9,
            subcategories = listOf(
                SubcategoryItem("sub_pharm_1", "cat_pharmacies", "صيدلية", listOf("صيدلية", "دواء", "أدوية", "علاج", "روشتة")),
                SubcategoryItem("sub_pharm_2", "cat_pharmacies", "مخازن ادوية", listOf("مخزن أدوية", "توزيع أدوية", "مستلزمات طبية بالجملة", "مخازن ادوية")),
                SubcategoryItem("sub_pharm_3", "cat_pharmacies", "صيدليات 24 ساعة وطوارئ", listOf("24 ساعة", "طوارئ", "توصيل منازل", "خدمة ليلية"))
            )
        ),
        CategoryItem(
            id = "cat_technicians",
            nameAr = "فنيين ومراكز صيانة",
            iconName = "Handyman",
            count = 14,
            colorHex = 0xFFE76F51,
            description = "كهرباء منازل، سباكة، صيانة تكييف، نجارة، نقاشة، أجهزة وسيارات",
            isFeatured = false,
            sortOrder = 10,
            subcategories = listOf(
                SubcategoryItem("sub_tech_1", "cat_technicians", "فني كهرباء منازل", listOf("كهربائي", "كهرباء منازل", "تأسيس كهرباء", "إضاءة")),
                SubcategoryItem("sub_tech_2", "cat_technicians", "سباكة", listOf("سباك", "سباكة", "صيانة حمامات", "خلاطات", "تسريب")),
                SubcategoryItem("sub_tech_3", "cat_technicians", "صيانة تكييفات وتبريد", listOf("تكييف", "تبريد", "شحن فريون", "تكييفات")),
                SubcategoryItem("sub_tech_4", "cat_technicians", "نجارة وأثاث", listOf("نجار", "نجارة", "أثاث", "تصليح أبواب", "غرف نوم")),
                SubcategoryItem("sub_tech_5", "cat_technicians", "نقاشة وطلاء", listOf("نقاش", "نقاشة", "دهانات", "طلاء", "ديكور")),
                SubcategoryItem("sub_tech_6", "cat_technicians", "صيانة أجهزة منزلية", listOf("صيانة غسالات", "ثلاجات", "بوتاجاز", "سخانات", "أجهزة منزلية")),
                SubcategoryItem("sub_tech_7", "cat_technicians", "سيارات", listOf("ميكانيكي سيارات", "صيانة سيارات", "عفشة", "كهرباء سيارات", "سيرفيس", "مركز صيانة سيارات"))
            )
        ),
        CategoryItem(
            id = "cat_cafes",
            nameAr = "كافيهات",
            iconName = "Coffee",
            count = 9,
            colorHex = 0xFFD4A373,
            description = "كافيهات عائلية وشباب وبنات وعصائر فريش",
            isFeatured = false,
            sortOrder = 11,
            subcategories = listOf(
                SubcategoryItem("sub_cafe_1", "cat_cafes", "كافيهات عائلية", listOf("عائلات", "كافيه عائلي", "جلسات هادئة")),
                SubcategoryItem("sub_cafe_2", "cat_cafes", "كافيهات شباب", listOf("شباب", "ماتشات", "بلايستيشن", "قهوة")),
                SubcategoryItem("sub_cafe_3", "cat_cafes", "كافيهات بنات", listOf("بنات", "كافيه حريمي", "جلسات بنات")),
                SubcategoryItem("sub_cafe_4", "cat_cafes", "عصائر فرش", listOf("عصير فريش", "قصب", "مانجو", "كوكتيل", "سموذي"))
            )
        ),
        CategoryItem(
            id = "cat_shops",
            nameAr = "محلات",
            iconName = "Storefront",
            count = 22,
            colorHex = 0xFF7209B7,
            description = "سوبرماركت، ملابس، أدوات منزلية، هواتف، قطع غيار ونظارات",
            isFeatured = true,
            sortOrder = 12,
            subcategories = listOf(
                SubcategoryItem("sub_shop_1", "cat_shops", "سوبرماركت ومواد غذائية", listOf("سوبرماركت", "مواد غذائية", "بقالة", "ماركت")),
                SubcategoryItem("sub_shop_2", "cat_shops", "ملابس وأحذية", listOf("ملابس", "أحذية", "رجالي", "حريمي", "أطفال")),
                SubcategoryItem("sub_shop_3", "cat_shops", "أدوات منزلية وأجهزة كهربائية", listOf("أدوات منزلية", "أجهزة كهربائية", "مطبخ", "شاشات")),
                SubcategoryItem("sub_shop_4", "cat_shops", "موبايلات وإلكترونيات", listOf("موبايل", "هواتف", "إلكترونيات", "اكسسوارات موبايل")),
                SubcategoryItem("sub_shop_5", "cat_shops", "أقمشة وستائر", listOf("أقمشة", "ستائر", "مفروشات", "تنجيد")),
                SubcategoryItem("sub_shop_6", "cat_shops", "محلات أتيليه للعرائس", listOf("أتيليه عرائس", "فستان زفاف", "تأجير فساتين")),
                SubcategoryItem("sub_shop_7", "cat_shops", "محلات أتيليه للعرسان", listOf("أتيليه عرسان", "بدل رجالي", "بدلة عريس")),
                SubcategoryItem("sub_shop_8", "cat_shops", "عطارة", listOf("عطارة", "توابل", "أعشاب", "بهارات")),
                SubcategoryItem("sub_shop_9", "cat_shops", "محلات إكسسوارات وهدايا", listOf("إكسسوارات", "هدايا", "ساعات", "برفانات")),
                SubcategoryItem("sub_shop_10", "cat_shops", "قطع غيار واكسسوارات سيارات", listOf("قطع غيار سيارات", "اكسسوارات سيارات", "زيوت وفلاتر", "كاوتش")),
                SubcategoryItem("sub_shop_11", "cat_shops", "محلات نظارات", listOf("نظارات", "بصريات", "عدسات", "فحص نظر"))
            )
        ),
        CategoryItem(
            id = "cat_schools",
            nameAr = "مدارس",
            iconName = "School",
            count = 12,
            colorHex = 0xFF457B9D,
            description = "مدارس حكومية رسمية، خاصة ولغات، معاهد أزهرية، وفنية",
            isFeatured = true,
            sortOrder = 13,
            subcategories = listOf(
                SubcategoryItem("sub_sch_1", "cat_schools", "مدارس حكومية رسمية", listOf("مدارس حكومية", "رسمية", "ابتدائي", "إعدادي", "ثانوي")),
                SubcategoryItem("sub_sch_2", "cat_schools", "مدارس خاصة ولغات", listOf("مدارس خاصة", "لغات", "تجريبي", "إنترناشونال")),
                SubcategoryItem("sub_sch_3", "cat_schools", "معاهد أزهرية", listOf("معهد أزهري", "أزهر", "ابتدائي أزهري", "ثانوي أزهري")),
                SubcategoryItem("sub_sch_4", "cat_schools", "مدارس فنية", listOf("مدارس فنية", "صنايع", "تجارة", "زراعة", "تمريض")),
                SubcategoryItem("sub_sch_5", "cat_schools", "روضات أطفال", listOf("روضة أطفال", "حضانة", "كي جي", "تأسيس أطفال"))
            )
        ),
        CategoryItem(
            id = "cat_radiology",
            nameAr = "مراكز أشعة وتحاليل",
            iconName = "Biotech",
            count = 8,
            colorHex = 0xFF2A9D8F,
            description = "معامل تحاليل طبية ومراكز أشعة مقطعية ورنين مغناطيسي",
            isFeatured = false,
            sortOrder = 14,
            subcategories = listOf(
                SubcategoryItem("sub_rad_1", "cat_radiology", "معامل تحاليل طبية", listOf("معمل تحاليل", "تحاليل طبية", "فحص دم", "وظائف كبد", "سكر")),
                SubcategoryItem("sub_rad_2", "cat_radiology", "مراكز أشعة ورنين", listOf("أشعة", "رنين مغناطيسي", "أشعة مقطعية", "سونار", "أشعة عادية"))
            )
        ),
        CategoryItem(
            id = "cat_medical_centers",
            nameAr = "مراكز طبية",
            iconName = "MedicalServices",
            count = 8,
            colorHex = 0xFF0077B6,
            description = "مجمعات عيادات تخصصية ومراكز طبية متكاملة",
            isFeatured = true,
            sortOrder = 15,
            subcategories = listOf(
                SubcategoryItem("sub_medc_1", "cat_medical_centers", "مجمعات عيادات تخصصية", listOf("مجمع عيادات", "عيادات تخصصية", "مركز طبي", "استشاريين"))
            )
        ),
        CategoryItem(
            id = "cat_hospitals",
            nameAr = "مستشفيات",
            iconName = "LocalHospital",
            count = 6,
            colorHex = 0xFF1D3557,
            description = "مستشفيات حكومية ومستشفيات خاصة ومراكز طوارئ وعمليات",
            isFeatured = false,
            sortOrder = 16,
            subcategories = listOf(
                SubcategoryItem("sub_hosp_1", "cat_hospitals", "مستشفيات حكومية", listOf("مستشفى حكومي", "تأمين صحي", "مستشفى عام", "مستشفى ميت غمر المركزي")),
                SubcategoryItem("sub_hosp_2", "cat_hospitals", "مستشفيات خاصة", listOf("مستشفى خاص", "عمليات", "عناية مركزة", "حضانات أطفال"))
            )
        ),
        CategoryItem(
            id = "cat_government",
            nameAr = "مصالح حكومية",
            iconName = "AccountBalance",
            count = 9,
            colorHex = 0xFF3A0CA3,
            description = "مجلس المدينة، السجل المدني، الشهر العقاري، البريد والتموين",
            isFeatured = true,
            sortOrder = 17,
            subcategories = listOf(
                SubcategoryItem("sub_gov_1", "cat_government", "مجلس المدينة والوحدات المحلية", listOf("مجلس المدينة", "وحدات محلية", "تنظيم وتراخيص", "رئاسة المركز")),
                SubcategoryItem("sub_gov_2", "cat_government", "سجل مدني", listOf("سجل مدني", "بطاقات رقم قومي", "شهادات ميلاد")),
                SubcategoryItem("sub_gov_3", "cat_government", "سجل شهر عقاري", listOf("شهر عقاري", "توثيق", "عقود", "توكيلات")),
                SubcategoryItem("sub_gov_4", "cat_government", "مكاتب البريد", listOf("مكتب بريد", "بريد سريع", "حوالات", "معاشات")),
                SubcategoryItem("sub_gov_5", "cat_government", "مكاتب التأمينات", listOf("تأمينات اجتماعية", "تأمينات ومعاشات")),
                SubcategoryItem("sub_gov_6", "cat_government", "مكاتب التموين", listOf("مكتب تموين", "بطاقات تموينية", "صرف سلع")),
                SubcategoryItem("sub_gov_7", "cat_government", "مكاتب المرور", listOf("مرور", "تراخيص سيارات", "رخص قيادة")),
                SubcategoryItem("sub_gov_8", "cat_government", "مكاتب الصحة", listOf("مكتب صحة", "تطعيمات", "تسجيل مواليد ووفيات"))
            )
        ),
        CategoryItem(
            id = "cat_factories",
            nameAr = "مصانع ورش",
            iconName = "Factory",
            count = 10,
            colorHex = 0xFF264653,
            description = "مصانع وتشكيل ألومنيوم، ورش حدادة وكريتال، وخراطة ومسابك",
            isFeatured = false,
            sortOrder = 18,
            subcategories = listOf(
                SubcategoryItem("sub_fact_1", "cat_factories", "مصانع وتشكيل ألومنيوم", listOf("ألومنيوم", "مصانع ألومنيوم", "تشكيل ألومنيوم", "أواني")),
                SubcategoryItem("sub_fact_2", "cat_factories", "ورش حدادة وكريتال", listOf("حدادة", "كريتال", "أبواب وشبابيك حديد", "هياكل معدنية")),
                SubcategoryItem("sub_fact_3", "cat_factories", "خراطة ومسابك ألومنيوم", listOf("خراطة", "مسابك ألومنيوم", "سباكة معادن", "ورش خراطة"))
            )
        ),
        CategoryItem(
            id = "cat_restaurants",
            nameAr = "مطاعم",
            iconName = "Restaurant",
            count = 12,
            colorHex = 0xFFE63946,
            description = "مشويات، كريب ووجبات سريعة، أسماك، بيتزا، حلويات ومخابز",
            isFeatured = true,
            sortOrder = 19,
            subcategories = listOf(
                SubcategoryItem("sub_rest_1", "cat_restaurants", "مأكولات شرقية ومشويات", listOf("مشويات", "كباب وكفتة", "أكل شرقي", "طواجن")),
                SubcategoryItem("sub_rest_2", "cat_restaurants", "وجبات سريعة وكريب", listOf("وجبات سريعة", "كريب", "برجر", "سندوتشات", "شاورما")),
                SubcategoryItem("sub_rest_3", "cat_restaurants", "مأكولات بحرية وأسماك", listOf("أسماك", "مأكولات بحرية", "جمبري", "شوربة سي فود")),
                SubcategoryItem("sub_rest_4", "cat_restaurants", "بيتزا وفطائر", listOf("بيتزا", "فطائر", "فطير مشلتت", "إيطالي")),
                SubcategoryItem("sub_rest_5", "cat_restaurants", "حلويات شرقية وغربية", listOf("حلويات", "بسبوسة", "كنافة", "جاتوه", "تورت")),
                SubcategoryItem("sub_rest_6", "cat_restaurants", "مخابز", listOf("مخبز", "عيش فينـو", "عيش بلدي", "مخبوزات"))
            )
        ),
        CategoryItem(
            id = "cat_trade_exhibitions",
            nameAr = "معارض تجارية",
            iconName = "Store",
            count = 8,
            colorHex = 0xFFF59E0B,
            description = "محلات أثاث، معارض سيارات، تجهيز العرائس، وأجهزة منزلية",
            isFeatured = false,
            sortOrder = 20,
            subcategories = listOf(
                SubcategoryItem("sub_trade_1", "cat_trade_exhibitions", "محلات أثاث", listOf("أثاث", "صالونات", "أنتريهات", "غرف سفرة", "موبيليات")),
                SubcategoryItem("sub_trade_2", "cat_trade_exhibitions", "معارض سيارات", listOf("معرض سيارات", "بيع وشراء سيارات", "سيارات جديدة ومستعملة")),
                SubcategoryItem("sub_trade_3", "cat_trade_exhibitions", "تجهيز العرائس", listOf("تجهيز عرائس", "جهاز العروسة", "مفروشات وأطقم")),
                SubcategoryItem("sub_trade_4", "cat_trade_exhibitions", "أجهزة منزلية", listOf("معارض أجهزة منزلية", "أجهزة كهربائية كبرى", "ثلاجات وغسالات"))
            )
        ),
        CategoryItem(
            id = "cat_libraries",
            nameAr = "مكتبات",
            iconName = "MenuBook",
            count = 10,
            colorHex = 0xFF2A9D8F,
            description = "بيع كتب وروايات، طباعة وتصوير، وأدوات مدرسية وجامعية",
            isFeatured = true,
            sortOrder = 21,
            subcategories = listOf(
                SubcategoryItem("sub_lib_1", "cat_libraries", "بيع كتب وروايات", listOf("كتب", "روايات", "كتب دينية", "مراجع علمية")),
                SubcategoryItem("sub_lib_2", "cat_libraries", "طباعة وتصوير وخدمات طابعات", listOf("طباعة", "تصوير مستندات", "أحبار", "خدمات طابعات")),
                SubcategoryItem("sub_lib_3", "cat_libraries", "أدوات مدرسية وجامعية", listOf("أدوات مدرسية", "أدوات جامعية", "كشاكيل", "أقلام", "شنط"))
            )
        ),
        CategoryItem(
            id = "cat_syndicates",
            nameAr = "نقابات",
            iconName = "Groups",
            count = 6,
            colorHex = 0xFF6A040F,
            description = "نقابات الأطباء، الصيادلة، المعلمين، التجاريين، والمهندسين",
            isFeatured = true,
            sortOrder = 22,
            subcategories = listOf(
                SubcategoryItem("sub_syn_1", "cat_syndicates", "نقابة الأطباء والصيادلة والأسنان", listOf("نقابة الأطباء", "نقابة الصيادلة", "نقابة أطباء الأسنان")),
                SubcategoryItem("sub_syn_2", "cat_syndicates", "نقابة المعلمين والتجاريين", listOf("نقابة المعلمين", "نقابة التجاريين")),
                SubcategoryItem("sub_syn_3", "cat_syndicates", "نقابة المهندسين والفرعيات", listOf("نقابة المهندسين", "الفرعيات المهنية"))
            )
        ),
        CategoryItem(
            id = "cat_clubs",
            nameAr = "نوادي ومراكز رياضية",
            iconName = "FitnessCenter",
            count = 5,
            colorHex = 0xFFF4A261,
            description = "نوادي اجتماعية، كيدز اريا، صالات جيم وملاعب كورة خماسية",
            isFeatured = false,
            sortOrder = 23,
            subcategories = listOf(
                SubcategoryItem("sub_club_1", "cat_clubs", "مدينة العاب - Kids area", listOf("كيدز اريا", "مدينة ألعاب", "ألعاب أطفال", "ملاهي")),
                SubcategoryItem("sub_club_2", "cat_clubs", "نادي إجتماعي", listOf("نادي اجتماعي", "نوادي عائلية", "جلسات النادي")),
                SubcategoryItem("sub_club_3", "cat_clubs", "صالات جيم كمال أجسام", listOf("جيم", "كمال أجسام", "لياقة بدنية", "فتنس")),
                SubcategoryItem("sub_club_4", "cat_clubs", "ملاعب خماسية كورة", listOf("ملاعب خماسية", "حجز ملعب", "كورة قدم", "نجيل صناعي"))
            )
        ),
        CategoryItem(
            id = "cat_doctors",
            nameAr = "عيادات وأطباء",
            iconName = "MedicalServices",
            count = 15,
            colorHex = 0xFF457B9D,
            description = "عيادات نخبة من الأطباء والاستشاريين في 15 تخصصاً طبياً",
            isFeatured = true,
            sortOrder = 24,
            subcategories = listOf(
                SubcategoryItem("sub_doc_1", "cat_doctors", "طب العيون", listOf("عيون", "رمد", "ليزك", "نظارات", "مياه بيضاء", "شبكية")),
                SubcategoryItem("sub_doc_2", "cat_doctors", "أنف وأذن وحنجرة", listOf("أنف", "أذن", "حنجرة", "جيوب أنفية", "لحمية", "سمعيات")),
                SubcategoryItem("sub_doc_3", "cat_doctors", "جراحة", listOf("جراحة", "جراحة عامة", "مناظير", "جراحة أورام", "جراحة أوعية")),
                SubcategoryItem("sub_doc_4", "cat_doctors", "أطفال", listOf("أطفال", "طبيب أطفال", "كشف أطفال", "تغذية أطفال")),
                SubcategoryItem("sub_doc_5", "cat_doctors", "الطب النفسي والعصبي", listOf("نفسي", "عصبي", "مخ وأعصاب", "علاج نفسي", "طب نفسي")),
                SubcategoryItem("sub_doc_6", "cat_doctors", "قلب", listOf("قلب", "أمراض القلب", "أوعية دموية", "قسطرة", "إيكو")),
                SubcategoryItem("sub_doc_7", "cat_doctors", "مسالك بولية", listOf("مسالك", "مسالك بولية", "كلى", "حصوات", "بروستاتا")),
                SubcategoryItem("sub_doc_8", "cat_doctors", "امراض صدر وحساسية", listOf("صدر", "حساسية", "أمراض صدر", "ربو", "جهاز تنفسي")),
                SubcategoryItem("sub_doc_9", "cat_doctors", "عظام", listOf("عظام", "جراحة عظام", "كسور", "مفاصل", "عمود فقري")),
                SubcategoryItem("sub_doc_10", "cat_doctors", "الأمراض الباطنية العامة", listOf("الأمراض الباطنية العامة", "باطنة عامة", "أمراض باطنية", "جهاز هضمي وكبد")),
                SubcategoryItem("sub_doc_11", "cat_doctors", "الأمراض الجلدية", listOf("جلدية", "الأمراض الجلدية", "بشرة", "ليزر", "شعر")),
                SubcategoryItem("sub_doc_12", "cat_doctors", "الفم والأسنان", listOf("أسنان", "الفم والأسنان", "حشو", "تقويم", "زراعة أسنان", "تبييض")),
                SubcategoryItem("sub_doc_13", "cat_doctors", "نساء وتوليد", listOf("نساء وتوليد", "حمل", "ولادة", "عقم وحقن مجهري", "سونار")),
                SubcategoryItem("sub_doc_14", "cat_doctors", "أطفال وحديثي الولادة", listOf("حديثي الولادة", "مبتسرين", "أطفال وحديثي الولادة", "تطعيمات")),
                SubcategoryItem("sub_doc_15", "cat_doctors", "باطنة", listOf("باطنة", "سكر", "ضغط", "كلى وغدد صماء", "باطني"))
            )
        ),
        CategoryItem(
            id = "cat_beauty_care",
            nameAr = "الجمال والعناية الشخصية",
            iconName = "Face",
            count = 9,
            colorHex = 0xFFEC4899,
            description = "صالونات حلاقة، كوافيرات حريمي، سبا وعناية، ومستحضرات تجميل",
            isFeatured = false,
            sortOrder = 25,
            subcategories = listOf(
                SubcategoryItem("sub_bty_1", "cat_beauty_care", "صالونات حلاقة", listOf("حلاقة", "حلاق", "كوافير رجالي", "صالون حلاقة", "قص شعر رجالي")),
                SubcategoryItem("sub_bty_2", "cat_beauty_care", "كوافيرات وصالونات حريمي", listOf("كوافير حريمي", "صالون حريمي", "بيوتي سنتر", "فرد شعر", "ميكب عرائس")),
                SubcategoryItem("sub_bty_3", "cat_beauty_care", "سبا وعناية بالجسم", listOf("سبا", "مساج", "عناية بالجسم", "حمام مغربي", "تنظيف بشرة")),
                SubcategoryItem("sub_bty_4", "cat_beauty_care", "ميكاب ومستحضرات تجميل", listOf("ميكاب", "مستحضرات تجميل", "برفانات", "عناية بالشعر والبشرة"))
            )
        ),
        CategoryItem(
            id = "cat_services",
            nameAr = "خدمات",
            iconName = "LocalLaundryService",
            count = 7,
            colorHex = 0xFF0284C7,
            description = "مغاسل ملابس ومغاسل سجاد وخدمات عامة",
            isFeatured = false,
            sortOrder = 26,
            subcategories = listOf(
                SubcategoryItem("sub_serv_1", "cat_services", "مغسلة ملابس", listOf("مغسلة ملابس", "دراي كلين", "غسيل وكي", "مكوجي")),
                SubcategoryItem("sub_serv_2", "cat_services", "مغسلة سجاد", listOf("مغسلة سجاد", "غسيل سجاد", "موكيت", "بطاطين", "تنظيف مفروشات"))
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
