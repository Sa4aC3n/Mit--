package com.example.data.engine

import com.example.data.model.GeographicDistrictItem
import com.example.data.model.SuggestedAreaItem

object MetGhamrGeoHierarchy {

    val mainStreetsAndDistricts = listOf(
        GeographicDistrictItem("dist_1", "شارع الحرية", "CENTER_MAIN_STREET", priority = 1),
        GeographicDistrictItem("dist_2", "شارع المحطة", "CENTER_MAIN_STREET", priority = 1),
        GeographicDistrictItem("dist_3", "شارع البحر (كورنيش النيل)", "CENTER_MAIN_STREET", priority = 1),
        GeographicDistrictItem("dist_4", "شارع بورسعيد", "CENTER_MAIN_STREET", priority = 1),
        GeographicDistrictItem("dist_5", "شارع الجيش", "CENTER_MAIN_STREET", priority = 1),
        GeographicDistrictItem("dist_6", "حي المعلمين", "CITY_DISTRICT", priority = 1),
        GeographicDistrictItem("dist_7", "حي النور والزهراء", "CITY_DISTRICT", priority = 2),
        GeographicDistrictItem("dist_8", "شارع أحمد عرابي", "CENTER_MAIN_STREET", priority = 2),
        GeographicDistrictItem("dist_9", "منطقة المستشفى العام", "CITY_DISTRICT", priority = 1),
        GeographicDistrictItem("dist_10", "ميدان صيدناوي ووسط البلد", "CITY_DISTRICT", priority = 1),
        GeographicDistrictItem("dist_11", "سوق الجملة والتجارة", "COMMERCIAL_AREA", priority = 1),
        GeographicDistrictItem("dist_12", "المنطقة الصناعية ومصانع الألومنيوم", "INDUSTRIAL_ZONE", priority = 1),
        GeographicDistrictItem("dist_13", "مجمع ورش ومصانع صهرجت", "INDUSTRIAL_ZONE", priority = 2),
        GeographicDistrictItem("dist_14", "شارع التجارة وسوق الصاغة", "COMMERCIAL_AREA", priority = 1),
        GeographicDistrictItem("dist_15", "طريق ميت غمر - الزقازيق", "CENTER_MAIN_STREET", priority = 2),
        GeographicDistrictItem("dist_16", "طريق ميت غمر - بنها", "CENTER_MAIN_STREET", priority = 2),
        GeographicDistrictItem("dist_17", "طريق ميت غمر - المنصورة السريع", "CENTER_MAIN_STREET", priority = 2)
    )

    val villagesAndUnits = listOf(
        GeographicDistrictItem("vil_1", "صهرجت الكبرى", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_2", "تفهنا الأشراف", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_3", "بشلا", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_4", "أتميدة", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_5", "دنديط", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_6", "كوم النور", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_7", "ميت ناجي", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_8", "ميت القرشي", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_9", "كفر المقدام", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_10", "ميت الفرماوي", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_11", "دماص", "VILLAGE", priority = 1),
        GeographicDistrictItem("vil_12", "سنفا", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_13", "أوليلة", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_14", "كفر الشناوي", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_15", "سرنجا", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_16", "ميت يعيش", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_17", "ميت أبو خالد", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_18", "المعصرة", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_19", "طنامل الغربية", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_20", "طنامل الشرقية", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_21", "كفر بهيدة", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_22", "كفر الحطبة", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_23", "أبو نبهان", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_24", "هلا", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_25", "كفر إبراهيم", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_26", "كفر الوزير", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_27", "الرحمانية", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_28", "ميت محسن", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_29", "كفر النعيم", "VILLAGE", priority = 2),
        GeographicDistrictItem("vil_30", "كفر شرف", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_31", "كفر سرنجا", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_32", "البوها", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_33", "سنتماي", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_34", "كفر الشيخ هلال", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_35", "ميت غراب", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_36", "الحاكمية", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_37", "كفر عطا الله سليمان", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_38", "كفر علي غالي", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_39", "ميت العز", "VILLAGE", priority = 3),
        GeographicDistrictItem("vil_40", "البيوم", "VILLAGE", priority = 3)
    )

    val allLocations: List<GeographicDistrictItem> = mainStreetsAndDistricts + villagesAndUnits

    val defaultSuggestedAreas = listOf(
        SuggestedAreaItem(
            id = "sug_area_1",
            nameAr = "عزبة علي باشا",
            type = "VILLAGE",
            typeLabelAr = "تجمع / عزبة تابعة",
            sampleBusinessName = "مخبز البركة الآلي",
            confidence = 90
        ),
        SuggestedAreaItem(
            id = "sug_area_2",
            nameAr = "مساكن التعاونيات الجديدة",
            type = "NEIGHBORHOOD",
            typeLabelAr = "حي وتجمع سكني",
            sampleBusinessName = "سوبر ماركت الإيمان",
            confidence = 88
        ),
        SuggestedAreaItem(
            id = "sug_area_3",
            nameAr = "منطقة ورش صهرجت الصناعية",
            type = "INDUSTRIAL_ZONE",
            typeLabelAr = "منطقة صناعية وورش",
            sampleBusinessName = "مصنع الأهرام لتشكيل المعادن",
            confidence = 92
        ),
        SuggestedAreaItem(
            id = "sug_area_4",
            nameAr = "شارع السوق التجاري القديم",
            type = "COMMERCIAL_STREET",
            typeLabelAr = "شارع تجاري وسوق",
            sampleBusinessName = "محل السلام للمفروشات",
            confidence = 85
        )
    )
}

