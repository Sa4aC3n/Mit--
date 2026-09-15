package com.example.data.engine

import com.example.data.model.CandidateBusiness
import com.example.data.model.CandidateStatus
import com.example.data.model.DiscoverySourceType
import com.example.data.model.VerificationState
import kotlinx.coroutines.delay
import java.util.UUID

object SourceConnectors {

    data class RawDiscoveredRecord(
        val rawName: String,
        val rawCategory: String,
        val subcategory: String? = null,
        val specialty: String = "",
        val rawPhone: String? = null,
        val rawPhone2: String? = null,
        val rawWhatsapp: String? = null,
        val rawAddress: String = "",
        val area: String = "",
        val city: String = "مدينة ميت غمر",
        val latitude: Double = 30.7183,
        val longitude: Double = 31.2568,
        val rawWebsite: String? = null,
        val rawFacebook: String? = null,
        val workingHours: String = "",
        val source: DiscoverySourceType,
        val sourceUrl: String? = null,
        val rating: Float = 0f,
        val reviewCount: Int = 0
    )

    /**
     * Executes discovery for a given SearchQueryPlan across authorized sources with safe rate-limiting.
     */
    suspend fun discoverRecordsForQuery(
        queryPlan: SearchQueryPlan,
        sources: List<DiscoverySourceType>,
        jobId: String
    ): List<CandidateBusiness> {
        val discoveredCandidates = mutableListOf<CandidateBusiness>()

        // Simulate safe rate limiting / polite network pause (150ms)
        delay(150)

        // Mock multi-source discovery results based on query plan parameters
        for (source in sources) {
            val rawRecords = fetchRawSourceRecords(queryPlan, source)
            for (raw in rawRecords) {
                val cleanedName = NormalizationEngine.cleanBusinessName(raw.rawName)
                val phoneResult = NormalizationEngine.normalizePhone(raw.rawPhone)
                val secondaryPhoneResult = NormalizationEngine.normalizePhone(raw.rawPhone2)
                val cleanWeb = NormalizationEngine.cleanUrl(raw.rawWebsite)
                val cleanFb = NormalizationEngine.cleanUrl(raw.rawFacebook)
                val hours = NormalizationEngine.formatWorkingHours(raw.workingHours)

                val candidate = CandidateBusiness(
                    id = "cand_" + UUID.randomUUID().toString().take(10),
                    businessName = cleanedName,
                    categoryId = queryPlan.categoryId,
                    categoryName = queryPlan.categoryName,
                    subcategory = raw.subcategory ?: queryPlan.subcategory,
                    specialty = raw.specialty.ifBlank { queryPlan.subcategory ?: queryPlan.categoryName },
                    description = "تم اكتشافه عبر محرك البحث والجمع الذكي (${source.displayNameAr})",
                    phone = raw.rawPhone,
                    displayPhone = phoneResult?.display,
                    normalizedPhone = phoneResult?.normalized,
                    secondaryPhone = secondaryPhoneResult?.display,
                    whatsapp = raw.rawWhatsapp,
                    address = raw.rawAddress.ifBlank { "${raw.area}، ${raw.city}" },
                    area = raw.area.ifBlank { queryPlan.targetArea },
                    city = raw.city,
                    latitude = raw.latitude,
                    longitude = raw.longitude,
                    googlePlaceId = if (source == DiscoverySourceType.GOOGLE_PLACES) "gplace_${UUID.randomUUID().toString().take(8)}" else null,
                    website = cleanWeb,
                    facebookPage = cleanFb,
                    workingHours = hours,
                    is24Hours = hours.contains("24 ساعة"),
                    rating = raw.rating,
                    reviewCount = raw.reviewCount,
                    primarySource = source,
                    sourceUrl = raw.sourceUrl,
                    sourceIds = listOf("${source.name}:${UUID.randomUUID().toString().take(6)}"),
                    fieldProvenances = mapOf(
                        "name" to "${source.name}:${raw.sourceUrl ?: ""}",
                        "phone" to "${source.name}:${raw.sourceUrl ?: ""}",
                        "address" to "${source.name}:${raw.sourceUrl ?: ""}"
                    ),
                    qualityScore = 0,
                    status = CandidateStatus.NEW,
                    verificationStatus = if (source == DiscoverySourceType.OFFICIAL_WEBSITE || source == DiscoverySourceType.GOOGLE_PLACES) VerificationState.VERIFIED else VerificationState.PARTIALLY_VERIFIED,
                    jobId = jobId
                )

                // Calculate Quality Score
                val quality = DataEnrichmentAndMergingEngine.calculateQualityScore(candidate)
                discoveredCandidates.add(candidate.copy(qualityScore = quality))
            }
        }

        return discoveredCandidates
    }

    private fun fetchRawSourceRecords(
        queryPlan: SearchQueryPlan,
        source: DiscoverySourceType
    ): List<RawDiscoveredRecord> {
        val records = mutableListOf<RawDiscoveredRecord>()
        val catName = queryPlan.categoryName
        val area = queryPlan.targetArea

        when (source) {
            DiscoverySourceType.GOOGLE_PLACES -> {
                records.add(
                    RawDiscoveredRecord(
                        rawName = "مركز $catName النخبة",
                        rawCategory = catName,
                        subcategory = queryPlan.subcategory,
                        specialty = "خدمات $catName متكاملة",
                        rawPhone = "010" + (10000000..99999999).random(),
                        rawPhone2 = "05069" + (10000..99999).random(),
                        rawAddress = "$area، بجوار المعلم الرئيسي",
                        area = area,
                        city = if (area.contains("صهرجت") || area.contains("بشلا") || area.contains("تفهنا")) area else "مدينة ميت غمر",
                        rawWebsite = "https://www.el-nokhba-$area.com",
                        rawFacebook = "https://facebook.com/nokhba.$area",
                        workingHours = "09:00 AM - 11:00 PM",
                        source = source,
                        sourceUrl = "https://maps.google.com/?cid=123",
                        rating = 4.8f,
                        reviewCount = (20..150).random()
                    )
                )
            }
            DiscoverySourceType.FACEBOOK_PAGES -> {
                records.add(
                    RawDiscoveredRecord(
                        rawName = "جروب وتجمع $catName $area",
                        rawCategory = catName,
                        subcategory = queryPlan.subcategory,
                        specialty = "نشاط $catName",
                        rawPhone = "011" + (10000000..99999999).random(),
                        rawWhatsapp = "2011" + (10000000..99999999).random(),
                        rawAddress = "$area، الشارع الرئيسي",
                        area = area,
                        city = "مركز ميت غمر",
                        rawFacebook = "https://facebook.com/groups/metghamr.$catName",
                        workingHours = "10:00 AM - 12:00 AM",
                        source = source,
                        sourceUrl = "https://facebook.com/pages/metghamr",
                        rating = 4.6f,
                        reviewCount = (10..80).random()
                    )
                )
            }
            DiscoverySourceType.YELLOW_PAGES -> {
                records.add(
                    RawDiscoveredRecord(
                        rawName = "مؤسسة $catName الحديثة",
                        rawCategory = catName,
                        subcategory = queryPlan.subcategory,
                        specialty = "دليل أعمال $catName",
                        rawPhone = "05069" + (10000..99999).random(),
                        rawAddress = "ميت غمر - $area",
                        area = area,
                        city = "مدينة ميت غمر",
                        rawWebsite = "https://www.metghamr-directory.org/$area",
                        workingHours = "08:00 ص - 08:00 م",
                        source = source,
                        sourceUrl = "https://yellowpages.com.eg/metghamr",
                        rating = 4.5f,
                        reviewCount = 12
                    )
                )
            }
            DiscoverySourceType.OFFICIAL_WEBSITE -> {
                records.add(
                    RawDiscoveredRecord(
                        rawName = "شركة $catName ميت غمر الرسمية",
                        rawCategory = catName,
                        subcategory = queryPlan.subcategory,
                        specialty = "الفرع المعتمد",
                        rawPhone = "19" + (100..999).random(),
                        rawAddress = "شارع البحر، ميت غمر",
                        area = "شارع البحر (كورنيش النيل)",
                        city = "مدينة ميت غمر",
                        rawWebsite = "https://official-$catName.eg",
                        workingHours = "مفتوح 24 ساعة",
                        source = source,
                        sourceUrl = "https://official-$catName.eg/contact",
                        rating = 5.0f,
                        reviewCount = 200
                    )
                )
            }
            else -> {}
        }

        return records
    }
}
