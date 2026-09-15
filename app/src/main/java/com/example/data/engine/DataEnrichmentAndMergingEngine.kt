package com.example.data.engine

import com.example.data.model.*
import java.util.UUID

object DataEnrichmentAndMergingEngine {

    /**
     * Calculates Data Quality Score (0 to 100) based on field completeness, social presence, and verification.
     */
    fun calculateQualityScore(candidate: CandidateBusiness): Int {
        var score = 0

        // 1. Business Name (15 pts)
        if (candidate.businessName.isNotBlank()) score += 15

        // 2. Category & Specialty (15 pts)
        if (candidate.categoryId.isNotBlank()) score += 10
        if (candidate.specialty.isNotBlank()) score += 5

        // 3. Primary Phone / Mobile (20 pts)
        if (!candidate.normalizedPhone.isNullOrBlank()) score += 20
        if (!candidate.secondaryPhone.isNullOrBlank() || !candidate.whatsapp.isNullOrBlank()) score += 5

        // 4. Exact Address & Area (15 pts)
        if (candidate.address.isNotBlank()) score += 10
        if (candidate.area.isNotBlank()) score += 5

        // 5. Geographic Coordinates (10 pts)
        if (candidate.latitude > 0.0 && candidate.longitude > 0.0) score += 10

        // 6. Working Hours (10 pts)
        if (candidate.workingHours.isNotBlank()) score += 10

        // 7. Online Presence (Website / Social Links) (10 pts)
        if (!candidate.website.isNullOrBlank()) score += 5
        if (!candidate.facebookPage.isNullOrBlank()) score += 5

        return score.coerceIn(0, 100)
    }

    /**
     * Calculates Data Quality Score for BusinessEntity.
     */
    fun calculateQualityScore(entity: BusinessEntity): Int {
        var score = 0
        if (entity.name.isNotBlank()) score += 15
        if (entity.categoryId.isNotBlank()) score += 10
        if (entity.specialty.isNotBlank()) score += 5
        if (entity.phone.isNotBlank() && entity.phone != "غير متوفر") score += 20
        if (!entity.phoneSecondary.isNullOrBlank() || !entity.whatsapp.isNullOrBlank()) score += 5
        if (entity.address.isNotBlank()) score += 10
        if (entity.area.isNotBlank()) score += 5
        if (entity.latitude > 0.0 && entity.longitude > 0.0) score += 10
        if (entity.workingHours.isNotBlank()) score += 10
        if (!entity.websiteUrl.isNullOrBlank()) score += 5
        if (!entity.facebookUrl.isNullOrBlank()) score += 5
        return score.coerceIn(0, 100)
    }

    data class MergeOutcome(
        val mergedBusiness: BusinessEntity,
        val detectedConflicts: List<DataConflictItem>,
        val provenanceMap: Map<String, String>,
        val updatedFields: List<String>,
        val sourcesToAttach: List<BusinessSourceEntity>,
        val fieldAudits: List<FieldAuditHistoryEntity>
    )

    /**
     * Merges a Candidate Record into an existing Master Business Record.
     * Preserves existing values if candidate has nulls, enriches with new fields,
     * tracks field-level provenance, attaches external sources, and detects conflicts.
     */
    fun mergeCandidateIntoBusiness(
        existing: BusinessEntity,
        candidate: CandidateBusiness
    ): MergeOutcome {
        val conflicts = mutableListOf<DataConflictItem>()
        val provenance = mutableMapOf<String, String>()
        val updatedFields = mutableListOf<String>()
        val fieldAudits = mutableListOf<FieldAuditHistoryEntity>()

        val srcKey = candidate.primarySource.name
        val srcUrl = candidate.sourceUrl

        // 1. Phone Merge & Conflict Check
        var finalPhone = existing.phone
        var finalPhoneSrc = existing.phoneSource
        val candPhone = candidate.displayPhone ?: candidate.phone

        if ((finalPhone.isBlank() || finalPhone == "غير متوفر") && !candPhone.isNullOrBlank()) {
            val oldVal = finalPhone
            finalPhone = candPhone
            finalPhoneSrc = srcKey
            provenance["phone"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("رقم الهاتف الأساسي")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "phone",
                    fieldLabelAr = "رقم الهاتف الأساسي",
                    oldValue = oldVal,
                    newValue = finalPhone,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        } else if (finalPhone.isNotBlank() && finalPhone != "غير متوفر" && !candPhone.isNullOrBlank() && finalPhone != candPhone) {
            conflicts.add(
                DataConflictItem(
                    conflictId = "cnf_" + UUID.randomUUID().toString().take(8),
                    candidateId = candidate.id,
                    businessName = existing.name,
                    fieldName = "phone",
                    fieldLabelAr = "رقم الهاتف",
                    valueA = finalPhone,
                    sourceA = DiscoverySourceType.ADMIN_MANUAL,
                    valueB = candPhone,
                    sourceB = candidate.primarySource
                )
            )
        }

        // 2. Secondary Phone
        var finalSecondaryPhone = existing.phoneSecondary
        if (finalSecondaryPhone.isNullOrBlank() && !candidate.secondaryPhone.isNullOrBlank()) {
            finalSecondaryPhone = candidate.secondaryPhone
            provenance["phoneSecondary"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("الهاتف الإضافي")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "phoneSecondary",
                    fieldLabelAr = "الهاتف الإضافي",
                    oldValue = null,
                    newValue = finalSecondaryPhone,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        }

        // 3. WhatsApp
        var finalWhatsapp = existing.whatsapp
        if (finalWhatsapp.isNullOrBlank() && !candidate.whatsapp.isNullOrBlank()) {
            finalWhatsapp = candidate.whatsapp
            provenance["whatsapp"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("رقم الواتساب")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "whatsapp",
                    fieldLabelAr = "رقم الواتساب",
                    oldValue = null,
                    newValue = finalWhatsapp,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        }

        // 4. Website URL
        var finalWebsite = existing.websiteUrl
        var finalWebsiteSrc = existing.websiteSource
        if (finalWebsite.isNullOrBlank() && !candidate.website.isNullOrBlank()) {
            finalWebsite = candidate.website
            finalWebsiteSrc = srcKey
            provenance["websiteUrl"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("الموقع الإلكتروني الرسمي")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "websiteUrl",
                    fieldLabelAr = "الموقع الإلكتروني",
                    oldValue = null,
                    newValue = finalWebsite,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        }

        // 5. Facebook URL
        var finalFacebook = existing.facebookUrl
        var finalFacebookSrc = existing.facebookSource
        if (finalFacebook.isNullOrBlank() && !candidate.facebookPage.isNullOrBlank()) {
            finalFacebook = candidate.facebookPage
            finalFacebookSrc = srcKey
            provenance["facebookUrl"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("صفحة فيسبوك")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "facebookUrl",
                    fieldLabelAr = "صفحة فيسبوك",
                    oldValue = null,
                    newValue = finalFacebook,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        }

        // 6. Address & Area
        var finalAddress = existing.address
        var finalAddressSrc = existing.addressSource
        if (finalAddress.isBlank() && candidate.address.isNotBlank()) {
            finalAddress = candidate.address
            finalAddressSrc = srcKey
            provenance["address"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("العنوان")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "address",
                    fieldLabelAr = "العنوان",
                    oldValue = null,
                    newValue = finalAddress,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        }

        // 7. Working Hours
        var finalWorkingHours = existing.workingHours
        var finalWorkingHoursSrc = existing.workingHoursSource
        if (finalWorkingHours.isBlank() && candidate.workingHours.isNotBlank()) {
            finalWorkingHours = candidate.workingHours
            finalWorkingHoursSrc = srcKey
            provenance["workingHours"] = "$srcKey:${srcUrl ?: ""}"
            updatedFields.add("مواعيد العمل")
            fieldAudits.add(
                FieldAuditHistoryEntity(
                    id = "fa_" + UUID.randomUUID().toString().take(10),
                    businessId = existing.id,
                    fieldName = "workingHours",
                    fieldLabelAr = "مواعيد العمل",
                    oldValue = null,
                    newValue = finalWorkingHours,
                    source = srcKey,
                    sourceUrl = srcUrl
                )
            )
        }

        // 8. Specialty & Description
        var finalSpecialty = existing.specialty
        if (finalSpecialty.isBlank() && candidate.specialty.isNotBlank()) {
            finalSpecialty = candidate.specialty
            updatedFields.add("التخصص والنشاط")
        }

        // Construct Sources to attach to the business
        val sourcesToAttach = mutableListOf<BusinessSourceEntity>()
        sourcesToAttach.add(
            BusinessSourceEntity(
                id = "src_" + UUID.randomUUID().toString().take(10),
                businessId = existing.id,
                sourceType = candidate.primarySource.name,
                sourceId = candidate.googlePlaceId ?: candidate.id,
                sourceUrl = candidate.sourceUrl,
                sourceName = candidate.primarySource.displayNameAr,
                sourceConfidence = candidate.primarySource.defaultReliability,
                isOfficial = candidate.primarySource == DiscoverySourceType.OFFICIAL_WEBSITE || candidate.primarySource == DiscoverySourceType.ADMIN_MANUAL,
                discoveredAt = candidate.createdAt,
                lastCheckedAt = System.currentTimeMillis()
            )
        )

        val merged = existing.copy(
            phone = finalPhone,
            phoneSecondary = finalSecondaryPhone,
            whatsapp = finalWhatsapp,
            websiteUrl = finalWebsite,
            facebookUrl = finalFacebook,
            address = finalAddress,
            workingHours = finalWorkingHours,
            specialty = finalSpecialty,
            phoneSource = finalPhoneSrc,
            websiteSource = finalWebsiteSrc,
            facebookSource = finalFacebookSrc,
            addressSource = finalAddressSrc,
            workingHoursSource = finalWorkingHoursSrc,
            dataQualityScore = calculateQualityScore(
                existing.copy(
                    phone = finalPhone,
                    phoneSecondary = finalSecondaryPhone,
                    whatsapp = finalWhatsapp,
                    websiteUrl = finalWebsite,
                    facebookUrl = finalFacebook,
                    address = finalAddress,
                    workingHours = finalWorkingHours,
                    specialty = finalSpecialty
                )
            ),
            isVerified = true,
            verificationStatus = "VERIFIED",
            lastVerifiedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return MergeOutcome(
            mergedBusiness = merged,
            detectedConflicts = conflicts,
            provenanceMap = provenance,
            updatedFields = updatedFields,
            sourcesToAttach = sourcesToAttach,
            fieldAudits = fieldAudits
        )
    }

    /**
     * Builds a full Side-By-Side Merge Preview.
     */
    fun buildSideBySidePreview(
        master: BusinessEntity,
        candidate: CandidateBusiness
    ): SideBySideMergePreview {
        val outcome = mergeCandidateIntoBusiness(master, candidate)
        return SideBySideMergePreview(
            masterBusiness = master,
            candidate = candidate,
            mergedResult = outcome.mergedBusiness,
            sourcesToAttach = outcome.sourcesToAttach,
            detectedConflicts = outcome.detectedConflicts,
            updatedFields = outcome.updatedFields
        )
    }

    /**
     * Converts an approved CandidateBusiness to a ready-to-insert BusinessEntity.
     */
    fun candidateToBusinessEntity(candidate: CandidateBusiness): Pair<BusinessEntity, List<BusinessSourceEntity>> {
        val busId = candidate.googlePlaceId ?: ("b_eng_" + UUID.randomUUID().toString().take(10))
        val quality = calculateQualityScore(candidate)

        val entity = BusinessEntity(
            id = busId,
            name = candidate.businessName,
            categoryId = candidate.categoryId,
            categoryName = candidate.categoryName,
            specialty = candidate.specialty.ifBlank { candidate.subcategory ?: candidate.categoryName },
            description = candidate.description.ifBlank { "نشاط تم جمعه وتوثيقه بواسطة محرك البيانات الذكي لدليل ميت غمر." },
            phone = candidate.displayPhone ?: candidate.phone ?: "غير متوفر",
            phoneSecondary = candidate.secondaryPhone,
            whatsapp = candidate.whatsapp,
            city = candidate.city.ifBlank { "مدينة ميت غمر" },
            address = candidate.address.ifBlank { "ميت غمر - الدقهلية" },
            area = candidate.area.ifBlank { "وسط البلد" },
            facebookUrl = candidate.facebookPage,
            websiteUrl = candidate.website,
            latitude = if (candidate.latitude > 0.0) candidate.latitude else 30.7183,
            longitude = if (candidate.longitude > 0.0) candidate.longitude else 31.2568,
            workingHours = candidate.workingHours.ifBlank { "09:00 ص - 10:00 م" },
            isOpenNow = true,
            isVerified = candidate.verificationStatus == VerificationState.VERIFIED || quality >= 80,
            isActive = true,
            ratingAverage = if (candidate.rating > 0f) candidate.rating else 4.8f,
            ratingCount = if (candidate.reviewCount > 0) candidate.reviewCount else 15,
            viewCount = 120,
            verificationStatus = if (candidate.verificationStatus == VerificationState.VERIFIED || quality >= 80) "VERIFIED" else "UNVERIFIED",
            dataQualityScore = quality,
            phoneSource = candidate.primarySource.name,
            websiteSource = if (!candidate.website.isNullOrBlank()) candidate.primarySource.name else null,
            facebookSource = if (!candidate.facebookPage.isNullOrBlank()) candidate.primarySource.name else null,
            addressSource = candidate.primarySource.name,
            workingHoursSource = candidate.primarySource.name,
            createdAt = candidate.createdAt,
            lastVerifiedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val sources = mutableListOf<BusinessSourceEntity>()
        // Primary Source
        sources.add(
            BusinessSourceEntity(
                id = "src_" + UUID.randomUUID().toString().take(10),
                businessId = busId,
                sourceType = candidate.primarySource.name,
                sourceId = candidate.googlePlaceId ?: candidate.id,
                sourceUrl = candidate.sourceUrl,
                sourceName = candidate.primarySource.displayNameAr,
                sourceConfidence = candidate.primarySource.defaultReliability,
                isOfficial = candidate.primarySource == DiscoverySourceType.OFFICIAL_WEBSITE || candidate.primarySource == DiscoverySourceType.ADMIN_MANUAL,
                discoveredAt = candidate.createdAt,
                lastCheckedAt = System.currentTimeMillis()
            )
        )

        // Additional source IDs in candidate
        for (extraSrc in candidate.sourceIds.filter { it != candidate.primarySource.name }) {
            sources.add(
                BusinessSourceEntity(
                    id = "src_" + UUID.randomUUID().toString().take(10),
                    businessId = busId,
                    sourceType = extraSrc,
                    sourceId = null,
                    sourceUrl = candidate.fieldProvenances[extraSrc],
                    sourceName = extraSrc,
                    sourceConfidence = 80,
                    isOfficial = extraSrc == "OFFICIAL_WEBSITE",
                    discoveredAt = candidate.createdAt,
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
        }

        return Pair(entity, sources)
    }
}

