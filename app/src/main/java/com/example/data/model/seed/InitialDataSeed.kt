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
                SubcategoryItem("sub_doc_1", "cat_doctors", "عظام ومفاصل", listOf("كسور", "عمود فقري", "غضروف")),
                SubcategoryItem("sub_doc_2", "cat_doctors", "باطنة وقلب", listOf("ضغط", "سكر", "قلب")),
                SubcategoryItem("sub_doc_3", "cat_doctors", "أطفال وحديثي الولادة", listOf("أطفال", "تطعيمات", "نمو")),
                SubcategoryItem("sub_doc_4", "cat_doctors", "نساء وتوليد", listOf("حمل", "ولادة", "سونار")),
                SubcategoryItem("sub_doc_5", "cat_doctors", "أسنان وتجميل", listOf("تقويم", "حشو", "زراعة")),
                SubcategoryItem("sub_doc_6", "cat_doctors", "جلدية وتجميل", listOf("ليزر", "بشرة", "شعر"))
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

    val businesses = listOf(
        // Restaurants
        BusinessEntity(
            id = "b_rest_1",
            name = "مطعم ومشاوي الشرقاوي",
            categoryId = "cat_restaurants",
            categoryName = "مطاعم",
            specialty = "كباب وكفتة ومأكولات شرقية",
            description = "أجود أنواع اللحوم البلدي والمشويات الطازجة يومياً مع كبسة وأرز بسمتي سلطات وتحلية شرقية.",
            phone = "01012345678",
            phoneSecondary = "0506912345",
            whatsapp = "201012345678",
            address = "شارع الحرية، بجوار البنك الأهلي المصري",
            area = "شارع الحرية - ميت غمر",
            latitude = 30.7183,
            longitude = 31.2568,
            workingHours = "10:00 ص - 02:00 ص",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 42,
            viewCount = 380
        ),
        BusinessEntity(
            id = "b_rest_2",
            name = "مطعم وكريب بازوكا ميت غمر",
            categoryId = "cat_restaurants",
            categoryName = "مطاعم",
            specialty = "برجر، كريب، ووجبات سريعة",
            description = "أفضل سندوتشات البرجر والكريب الإيطالي في ميت غمر مع خدمة توصيل طلبات للمنازل والقرى المجاورة.",
            phone = "01287654321",
            whatsapp = "201287654321",
            address = "شارع بورسعيد، أمام سينما ميت غمر",
            area = "شارع بورسعيد - ميت غمر",
            workingHours = "11:00 ص - 03:00 ص",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.6f,
            ratingCount = 29,
            viewCount = 290
        ),
        BusinessEntity(
            id = "b_rest_3",
            name = "مطعم البرنس للأسماك والجمبري",
            categoryId = "cat_restaurants",
            categoryName = "مطاعم",
            specialty = "مأكولات بحرية وطواجن أسماك",
            description = "أسماك طازجة يومياً من السويس وبورسعيد، شوربة سي فود، وسنجاري وطواجن بالموتزاريلا.",
            phone = "01099887766",
            address = "طريق صهرجت الكبرى الرئيسي",
            area = "صهرجت الكبرى",
            workingHours = "12:00 م - 12:00 ص",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 35,
            viewCount = 410
        ),

        // Doctors & Clinics
        BusinessEntity(
            id = "b_doc_1",
            name = "عيادة الدكتور أحمد متولي",
            categoryId = "cat_doctors",
            categoryName = "عيادات وأطباء",
            specialty = "استشاري جراحة العظام والمفاصل والمناظير",
            description = "علاج آلام الظهر والفقرات، تغيير المفاصل، علاج إصابات الملعب والكسور باستخدام أحدث التقنيات.",
            phone = "01122334455",
            phoneSecondary = "0506923456",
            whatsapp = "201122334455",
            address = "شارع المحطة، برج الأطباء - الدور الثالث",
            area = "شارع المحطة - ميت غمر",
            workingHours = "03:00 م - 09:00 م (السبت - الأربعاء)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 58,
            viewCount = 620
        ),
        BusinessEntity(
            id = "b_doc_2",
            name = "مركـز د. فاطمة الزهراء للطب النفسي والأطفال",
            categoryId = "cat_doctors",
            categoryName = "عيادات وأطباء",
            specialty = "أخصائي طب الأطفال وحديثي الولادة",
            description = "متابعة نمو حديثي الولادة، الفحوصات الشاملة، تطعيمات الأطفال واستشارات التغذية الصحية.",
            phone = "01055443322",
            address = "قرية أتميدة، بجوار المجمع الطبي",
            area = "أتميدة",
            workingHours = "02:00 م - 08:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 24,
            viewCount = 310
        ),
        BusinessEntity(
            id = "b_doc_3",
            name = "عيادة د. محمود الشربيني لأسنان الجمال",
            categoryId = "cat_doctors",
            categoryName = "عيادات وأطباء",
            specialty = "تركيبات وزراعة الأسنان والتقويم",
            description = "حشو ليزر بدون ألم، تبييض هوليوود سمايل، زراعة وتقويم أسنان الأطفال والكبار بأحدث أجهزة الديجيتال.",
            phone = "01233445566",
            whatsapp = "201233445566",
            address = "حي المعلمين، العمارة الفندقية",
            area = "حي المعلمين - ميت غمر",
            workingHours = "04:00 م - 10:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 39,
            viewCount = 480
        ),

        // Hospitals
        BusinessEntity(
            id = "b_hosp_1",
            name = "مستشفى ميت غمر العام",
            categoryId = "cat_hospitals",
            categoryName = "مستشفيات",
            specialty = "مستشفى حكومي متكامل وطوارئ 24 ساعة",
            description = "أقسام الطوارئ، الرعاية المركزة، الحاضنات، الاستقبال والعناية المركزة وجراحات اليوم الواحد.",
            phone = "0506900100",
            phoneSecondary = "0506900200",
            address = "شارع الجلاء، ميت غمر",
            area = "شارع الجلاء - ميت غمر",
            workingHours = "24 ساعة / 7 أيام",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.2f,
            ratingCount = 85,
            viewCount = 1200
        ),
        BusinessEntity(
            id = "b_hosp_2",
            name = "مستشفى الشفاء التخصصي",
            categoryId = "cat_hospitals",
            categoryName = "مستشفيات",
            specialty = "مستشفى خاص - جراحات ودعم عناية مركزة",
            description = "غرف عمليات مجهزة بأعلى المعايير، أجنحة خاصة، وحدة قسطرة القلب ورعاية الحالات الحرجة.",
            phone = "01077665544",
            address = "طريق صهرجت ميت غمر السريع",
            area = "صهرجت الكبرى",
            workingHours = "24 ساعة / 7 أيام",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 41,
            viewCount = 530
        ),

        // Radiology & Labs
        BusinessEntity(
            id = "b_rad_1",
            name = "معامل البرج للتحاليل الطبية - فرع ميت غمر",
            categoryId = "cat_radiology",
            categoryName = "مراكز أشعة وتحاليل",
            specialty = "تحاليل طبية شاملة ودقيقة بأجهزة ديجيتال",
            description = "تحاليل سكر، كبد، كلى، هرمونات، دلالات أورام مع إمكانية إرسال نتائج التحاليل عبر الواتساب وسحب العينات منزلية.",
            phone = "19201",
            phoneSecondary = "01000192010",
            whatsapp = "201000192010",
            address = "شارع الحرية، فوق صيدلية مصر",
            area = "شارع الحرية - ميت غمر",
            workingHours = "08:00 ص - 11:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 63,
            viewCount = 750
        ),
        BusinessEntity(
            id = "b_rad_2",
            name = "مركز النيل للأشعة المقطعية والرنين",
            categoryId = "cat_radiology",
            categoryName = "مراكز أشعة وتحاليل",
            specialty = "أشعة رنين مغناطيسي، مقطعية، وسونار 4D",
            description = "أحدث أجهزة الأشعة التشخيصية والسونار والماموجرام وتحليل تراكم العظام بإشراف نخبة من أساتذة الجامعة.",
            phone = "01088990011",
            address = "شارع بورسعيد، أمام النادي الرياضي",
            area = "شارع بورسعيد - ميت غمر",
            workingHours = "09:00 ص - 10:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 37,
            viewCount = 410
        ),

        // Technicians
        BusinessEntity(
            id = "b_tech_1",
            name = "الأسطى إبراهيم صيانة تكييفات وأجهزة منزلية",
            categoryId = "cat_technicians",
            categoryName = "فنيين وصنايعية",
            specialty = "فني تكييف، غسالات وثلاجات ديجيتال",
            description = "صيانة وتأسيس شبكات التكييف والشحن، إصلاح غسالات وشاشات وثلاجات لجميع الماركات بشهادة ضمان.",
            phone = "01066554433",
            whatsapp = "201066554433",
            address = "ميت غمر والقرى التابعة (خدمة منزلية فورية)",
            area = "كوم النور",
            workingHours = "08:00 ص - 10:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 48,
            viewCount = 510
        ),
        BusinessEntity(
            id = "b_tech_2",
            name = "ورشة الحاج فتحي للكهرباء والتأسيس",
            categoryId = "cat_technicians",
            categoryName = "فنيين وصنايعية",
            specialty = "كهربائي سيليكون وتأسيس شقق ومحلات",
            description = "تأسيس شبكات كهرباء حديثة، تركيب شاشات ولوحات سمارت، ليد بروفايل وإصلاح الأعطال الطارئة.",
            phone = "01211223344",
            address = "قرية دنديط، الشارع الجديد",
            area = "دنديط",
            workingHours = "08:00 ص - 09:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 31,
            viewCount = 340
        ),

        // Factories & Aluminum
        BusinessEntity(
            id = "b_fact_1",
            name = "مصنع السلام للألومنيوم والقطع المعمارية",
            categoryId = "cat_factories",
            categoryName = "مصانع ورش",
            specialty = "تصنيع أبواب، شبابيك ألوميتال ومطابخ مودرن",
            description = "أكبر مصنع ألومنيوم في ميت غمر لقطاعات الـ BS والجامبو وشبابيك كاتمة للحديث وسكاي لايت بجودة تصدير.",
            phone = "01033221100",
            phoneSecondary = "0506934567",
            whatsapp = "201033221100",
            address = "المنطقة الصناعية - ميت غمر",
            area = "المنطقة الصناعية - ميت غمر",
            workingHours = "08:00 ص - 06:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 52,
            viewCount = 680
        ),
        BusinessEntity(
            id = "b_fact_2",
            name = "ورشة الفرسان للمعدات والتشكيل الهيدروليكي",
            categoryId = "cat_factories",
            categoryName = "مصانع ورش",
            specialty = "خراطة وتشكيل حديد ومكابس صناعية",
            description = "خراطة سي إن سي CNC، تصنيع قطع غيار خطوط الإنتاج والورش الصناعية والهياكل المعدنية.",
            phone = "01144556677",
            address = "شارع المصانع القديم، تفهنا الأشراف",
            area = "تفهنا الأشراف",
            workingHours = "08:30 ص - 07:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 19,
            viewCount = 220
        ),

        // Cafes
        BusinessEntity(
            id = "b_cafe_1",
            name = "كافيه ولاونج نيل سيتي",
            categoryId = "cat_cafes",
            categoryName = "كافيهات",
            specialty = "مشروبات ساخنة وباردة ومكان عائلي",
            description = "إطلالة رائعة، مشروبات قهوة مختصة، وافل وكريب، شاشات عرض مباريات وشبكة واي فاي سريعة.",
            phone = "01011998877",
            whatsapp = "201011998877",
            address = "طريق الكورنيش، ميت غمر",
            area = "شارع الحرية - ميت غمر",
            workingHours = "09:00 ص - 02:00 ص",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 45,
            viewCount = 510
        ),

        // Clubs & Gyms
        BusinessEntity(
            id = "b_club_1",
            name = "نادي ميت غمر الرياضي الاجتماعي",
            categoryId = "cat_clubs",
            categoryName = "نوادي ومراكز رياضية",
            specialty = "نادي رياضي، ملاعب كورة وحمام سباحة",
            description = "أكاديميات كرة قدم، ملاعب تنس، حمام سباحة أُولمبي وصالة جيم وتدريب فنون قتالية للأطفال.",
            phone = "0506901122",
            address = "شارع بورسعيد، ميت غمر",
            area = "شارع بورسعيد - ميت غمر",
            workingHours = "08:00 ص - 11:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.6f,
            ratingCount = 70,
            viewCount = 890
        ),
        BusinessEntity(
            id = "b_gym_2",
            name = "جيم باور هوس اللياقة وكمال الأجسام",
            categoryId = "cat_clubs",
            categoryName = "نوادي ومراكز رياضية",
            specialty = "أجهزة كارديو وحديد ومتابعة مدربين",
            description = "أحدث أجهزة كمال الأجسام، برامج تخسيس وزيادة وزن، سونا ومفتوح مواعيد خاصة للسيدات صباحاً.",
            phone = "01099112233",
            address = "طريق بشلا الرئيسي",
            area = "بشلا",
            workingHours = "07:00 ص - 12:00 منتصف الليل",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 28,
            viewCount = 310
        ),

        // Pharmacies
        BusinessEntity(
            id = "b_pharm_1",
            name = "صيدلية العزبي - فرع ميت غمر",
            categoryId = "cat_pharmacies",
            categoryName = "صيدليات",
            specialty = "أدوية مستوردة ومستحضرات تجميل ودليفري",
            description = "توفير كافة الأدوية والمستلزمات الطبية وأجهزة قياس الضغط والسكر مع خدمة توصيل مجاني.",
            phone = "19777",
            phoneSecondary = "01022334411",
            whatsapp = "201022334411",
            address = "ميدان المحطة، ميت غمر",
            area = "شارع المحطة - ميت غمر",
            workingHours = "24 ساعة / 7 أيام",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 92,
            viewCount = 1100
        ),

        // Shops
        BusinessEntity(
            id = "b_shop_1",
            name = "هايبر ماركت الخير والبركة",
            categoryId = "cat_shops",
            categoryName = "محلات وسوبرماركت",
            specialty = "مواد غذائية، مجمدات وبقالة جودة عالية",
            description = "جميع المستلزمات المنزلية، لحوم طازجة، أجبان، خضار وفاكهة وعروض خصومات أسبوعية ممتازة.",
            phone = "01055667788",
            address = "قرية سنفا، الشارع العام",
            area = "سنفا",
            workingHours = "08:00 ص - 01:00 ص",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 33,
            viewCount = 390
        ),

        // Educational
        BusinessEntity(
            id = "b_edu_1",
            name = "سنتر التفوق التعليمي - ميت غمر",
            categoryId = "cat_education",
            categoryName = "خدمات تعليمية",
            specialty = "دروس خصوصية وكورسات لغات وكومبيوتر",
            description = "محاضرات ونخبة من أفضل معلمي الثانوية العامة والإعدادية، قاعات مكيفة وشاشات تفاعلية.",
            phone = "01044332211",
            whatsapp = "201044332211",
            address = "خلف مجلس المدينة، ميت غمر",
            area = "شارع الحرية - ميت غمر",
            workingHours = "09:00 ص - 09:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 38,
            viewCount = 440
        ),

        // Government Offices
        BusinessEntity(
            id = "b_gov_1",
            name = "رئاسة مجلس مدينة ومجلس مركز ميت غمر",
            categoryId = "cat_government",
            categoryName = "مصالح حكومية",
            specialty = "خدمات المواطنين والتراخيص والادارة المحلية",
            description = "المقر الرئيسي لمجلس مدينة ميت غمر لإصدار التراخيص والإشعارات وتقديم الخدمات الجماهيرية.",
            phone = "0506900112",
            city = "مدينة ميت غمر",
            address = "شارع الحرية، أمام نيل ميت غمر",
            area = "وسط البلد",
            workingHours = "08:00 ص - 03:00 م (الأحد - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 85,
            viewCount = 1250
        ),
        BusinessEntity(
            id = "b_gov_2",
            name = "مكتب السجل المدني الرئيسي بميت غمر",
            categoryId = "cat_government",
            categoryName = "مصالح حكومية",
            specialty = "استخراج بطاقات الرقم القومي وشهادات الميلاد والوفاة",
            description = "استخراج وتجديد بطاقات الرقم القومي وشهادات الميلاد المميكنة وقيد العائلة وصحيفة الأحوال الجنائية.",
            phone = "0506900223",
            city = "مدينة ميت غمر",
            address = "شارع المحطة، بجوار قسم الشرطة",
            area = "شارع المحطة",
            workingHours = "08:00 ص - 02:00 م (الأحد - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.5f,
            ratingCount = 110,
            viewCount = 1420
        ),

        // Banks
        BusinessEntity(
            id = "b_bank_1",
            name = "البنك الأهلي المصري - فرع ميت غمر",
            categoryId = "cat_banks",
            categoryName = "بنوك",
            specialty = "خدمات مصرفية، فتح حسابات، شهادات واستبدال عملات",
            description = "الفرع الرئيسي للبنك الأهلي المصري بميت غمر مع صالة استقبال كبار العملاء وماكينات ATM 24 ساعة.",
            phone = "19623",
            phoneSecondary = "0506911200",
            city = "مدينة ميت غمر",
            address = "شارع الحرية، ميت غمر",
            area = "شارع الحرية",
            facebookUrl = "https://facebook.com/NBE1988",
            websiteUrl = "https://www.nbe.com.eg",
            workingHours = "08:30 ص - 03:00 م (الأحد - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 160,
            viewCount = 2100
        ),
        BusinessEntity(
            id = "b_bank_2",
            name = "بنك مصر - فرع ميت غمر الرئيسي",
            categoryId = "cat_banks",
            categoryName = "بنوك",
            specialty = "تمويل مشروعات، قروض، وإيداع وماكينات صراف آلي",
            description = "فرع بنك مصر الرئيسي يقدم جميع المعاملات المالية وتحويلات الأموال وخدمات بطاقات الائتمان.",
            phone = "19888",
            phoneSecondary = "0506912300",
            city = "مدينة ميت غمر",
            address = "شارع المحطة، ميت غمر",
            area = "شارع المحطة",
            websiteUrl = "https://www.banquemisr.com",
            workingHours = "08:30 ص - 03:00 م (الأحد - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 145,
            viewCount = 1890
        ),

        // Universities
        BusinessEntity(
            id = "b_univ_1",
            name = "فرع جامعة الأزهر الشريف - تفهنا الأشراف",
            categoryId = "cat_universities",
            categoryName = "جامعات",
            specialty = "كليات الشريعة والقانون، التربية، والتجارة بنين وبنات",
            description = "مجمع كليات جامعة الأزهر الشريف بقرية تفهنا الأشراف مركز ميت غمر لتخريج الكوادر العلمية والدينية.",
            phone = "0506940111",
            city = "تفهنا الأشراف",
            address = "الشارع الرئيسي، قرية تفهنا الأشراف",
            area = "تفهنا الأشراف",
            workingHours = "08:00 ص - 04:00 م (السبت - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 230,
            viewCount = 3100
        ),

        // Schools
        BusinessEntity(
            id = "b_sch_1",
            name = "مدرسة ميت غمر الرسمية المتميزة لغات",
            categoryId = "cat_schools",
            categoryName = "مدارس",
            specialty = "تعليم لغات تجريبي مميز (رياض أطفال - ابتدائي - إعدادي - ثانوي)",
            description = "صرح تعليمي لغات متميز بميت غمر يضم ملاعب رياضية وأنشطة مدرسية ومعامل علوم وحاسب آلي حديثة.",
            phone = "0506904455",
            city = "مدينة ميت غمر",
            address = "بجوار نادي ميت غمر الرياضي",
            area = "وسط البلد",
            workingHours = "07:30 ص - 02:30 م (الأحد - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.8f,
            ratingCount = 95,
            viewCount = 1350
        ),

        // Libraries
        BusinessEntity(
            id = "b_lib_1",
            name = "مكتبة الفاروق للأدوات المدرسية والطباعة",
            categoryId = "cat_libraries",
            categoryName = "مكتبات",
            specialty = "أدوات مكتبية وجامعية، طباعة ملزمات وتصوير مستندات",
            description = "توفير كافة الأدوات المدرسية والجامعية والهندسية مع أحدث طابعات ألوان وسحب أوراق ودعاية.",
            phone = "01098765432",
            whatsapp = "201098765432",
            city = "مدينة ميت غمر",
            address = "شارع الحرية، أمام مدرسة المعلمين",
            area = "شارع الحرية",
            workingHours = "08:00 ص - 11:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 78,
            viewCount = 920
        ),

        // Companies
        BusinessEntity(
            id = "b_comp_1",
            name = "شركة ميت غمر للقطاعات الهندسية والألومنيوم",
            categoryId = "cat_companies",
            categoryName = "شركات",
            specialty = "تصنيع وتوريد قطاعات الألومنيوم والمستلزمات المعمارية",
            description = "إحدى كبرى الشركات المتخصصة في سحب وتصنيع قطاعات الألومنيوم والديكورات الحديثة للمباني والمشاريع.",
            phone = "0506915500",
            phoneSecondary = "01001122334",
            city = "مدينة ميت غمر",
            address = "طريق الزقازيق الرئيسي، ميت غمر",
            area = "شارع بورسعيد",
            websiteUrl = "https://www.metghamralum.com",
            workingHours = "08:00 ص - 05:00 م (السبت - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 112,
            viewCount = 1680
        ),

        // Syndicates
        BusinessEntity(
            id = "b_syn_1",
            name = "نقابة المهندسين - فرع ميت غمر",
            categoryId = "cat_syndicates",
            categoryName = "نقابات",
            specialty = "خدمات المهندسين، مشروع العلاج وتجديد الكارنيهات والأنشطة",
            description = "مقر فرعي لنقابة المهندسين يقدم كافة الخدمات النقابية، مشروع العلاج، ورحلات وتدريبات صيفية.",
            phone = "0506903322",
            city = "مدينة ميت غمر",
            address = "شارع الحرية، أعلى بنك القاهرة",
            area = "شارع الحرية",
            workingHours = "09:00 ص - 03:00 م (الأحد - الخميس)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.7f,
            ratingCount = 64,
            viewCount = 870
        ),

        // Charities
        BusinessEntity(
            id = "b_char_1",
            name = "جمعية كفالة اليتيم والتنمية الاجتماعية بميت غمر",
            categoryId = "cat_charities",
            categoryName = "جمعيات خيرية ومؤسسات أهلية",
            specialty = "رعاية الأيتام، المساعدات الشهرية للأسر المتعففة وتوزيع المساعدات",
            description = "جمعية خيرية مشهرة تعنى بكفالة الأيتام وتوفير الرعاية الصحية والتعليمية والغذائية للمحتاجين بميت غمر وقراها.",
            phone = "01011223388",
            whatsapp = "201011223388",
            city = "مدينة ميت غمر",
            address = "شارع بورسعيد، خلف جامع الغمري",
            area = "وسط البلد",
            workingHours = "09:00 ص - 05:00 م",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 5.0f,
            ratingCount = 132,
            viewCount = 1750
        ),

        // Medical Centers
        BusinessEntity(
            id = "b_medc_1",
            name = "مركز الشفاء الطبي التخصصي ومجمع العيادات",
            categoryId = "cat_medical_centers",
            categoryName = "مراكز طبية",
            specialty = "مجمع عيادات استشاريين، مركز جراحة يوم واحد ورعاية أولية",
            description = "مركز طبي شامل يضم أكثر من 15 تخصصاً طبياً مع أحدث أجهزة الأشعة ورسم القلب والعيادات الخارجية.",
            phone = "0506928899",
            phoneSecondary = "01233445566",
            city = "مدينة ميت غمر",
            address = "شارع المحطة، برج الشفاء الطبي",
            area = "شارع المحطة",
            workingHours = "09:00 ص - 11:00 م (طوال الأسبوع)",
            isOpenNow = true,
            isVerified = true,
            ratingAverage = 4.9f,
            ratingCount = 89,
            viewCount = 1410
        )
    )

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
            body = "تم إضافة أكثر من 150 عيادة ومطعم وورشة وصيدلية في مركز ومدينة ميت غمر والقرى التابعة.",
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
