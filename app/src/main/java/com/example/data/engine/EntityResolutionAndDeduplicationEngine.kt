package com.example.data.engine

import com.example.data.model.*
import com.example.util.ArabicNormalizer
import java.util.UUID
import kotlin.math.*

object EntityResolutionAndDeduplicationEngine {

    data class DeduplicationResult(
        val duplicateScore: Int,
        val status: CandidateStatus,
        val matchedBusinessId: String?,
        val matchedBusinessName: String?,
        val matchReasons: List<String>,
        val isBranch: Boolean = false,
        val branchName: String? = null
    )

    // Cross-lingual English <-> Arabic common keywords and transliterations
    private val TRANSLITERATION_MAP = mapOf(
        "restaurant" to "مطعم",
        "rest" to "مطعم",
        "food" to "ماكولات",
        "foods" to "ماكولات",
        "cafe" to "كافيه",
        "coffee" to "مقهى",
        "pharmacy" to "صيدلية",
        "clinic" to "عيادة",
        "hospital" to "مستشفى",
        "bakery" to "مخبز",
        "sweets" to "حلواني",
        "pastry" to "فطاطري",
        "market" to "ماركت",
        "supermarket" to "سوبرماركت",
        "hypermarket" to "هايبرماركت",
        "store" to "محل",
        "shop" to "محل",
        "center" to "مركز",
        "dental" to "اسنان",
        "optics" to "بصريات",
        "baraka" to "البركة",
        "albaraka" to "البركة",
        "elbaraka" to "البركة",
        "nokhba" to "النخبة",
        "shafie" to "الشافعي",
        "makarem" to "المكارم",
        "amira" to "الاميرة",
        "andalus" to "الاندلس",
        "farouk" to "فاروق"
    )

    /**
     * Compares a candidate business against existing Master businesses in the database.
     */
    fun evaluateCandidate(
        candidate: CandidateBusiness,
        existingBusinesses: List<BusinessEntity>
    ): DeduplicationResult {
        var highestScore = 0
        var bestMatch: BusinessEntity? = null
        var bestReasons = mutableListOf<String>()
        var isBranchDetected = false
        var branchNameDetected: String? = null

        val candNormName = ArabicNormalizer.normalize(candidate.businessName)
        val candNormPhone = candidate.normalizedPhone
        val candDomain = NormalizationEngine.extractDomain(candidate.website)
        val candNormAddress = ArabicNormalizer.normalize(candidate.address)

        for (existing in existingBusinesses) {
            var currentScore = 0
            val reasons = mutableListOf<String>()

            val existNormName = ArabicNormalizer.normalize(existing.name)
            val existNormPhone = ArabicNormalizer.normalizePhone(existing.phone)
            val existDomain = NormalizationEngine.extractDomain(existing.websiteUrl)
            val existNormAddress = ArabicNormalizer.normalize(existing.address)

            // 1. Google Place ID Match (Direct Match = +100)
            if (!candidate.googlePlaceId.isNullOrBlank() && candidate.googlePlaceId == existing.id) {
                currentScore += 100
                reasons.add("تطابق كامل في معرف خرائط جوجل (Google Place ID Match +100)")
            }

            // 2. Exact Phone Match (+50)
            val isPhoneMatched = !candNormPhone.isNullOrBlank() && !existNormPhone.isNullOrBlank() && (candNormPhone == existNormPhone)
            if (isPhoneMatched) {
                currentScore += 50
                reasons.add("تطابق كامل في رقم الهاتف الموحد ($candNormPhone) (+50)")
            }

            // 3. Facebook URL Match (+80)
            if (!candidate.facebookPage.isNullOrBlank() && !existing.facebookUrl.isNullOrBlank()) {
                val cleanCandFb = NormalizationEngine.cleanUrl(candidate.facebookPage)
                val cleanExistFb = NormalizationEngine.cleanUrl(existing.facebookUrl)
                if (cleanCandFb != null && cleanCandFb.equals(cleanExistFb, ignoreCase = true)) {
                    currentScore += 80
                    reasons.add("تطابق تام في رابط صفحة فيسبوك الرسمية (+80)")
                }
            }

            // 4. Website Domain Match (+70)
            if (!candDomain.isNullOrBlank() && !existDomain.isNullOrBlank() && candDomain == existDomain) {
                currentScore += 70
                reasons.add("تطابق في نطاق الموقع الإلكتروني الرسمي ($candDomain) (+70)")
            }

            // 5. Name Similarity (+30) - supports Arabic & English Transliterations
            val nameSim = calculateNameSimilarity(candidate.businessName, existing.name)
            if (nameSim >= 0.80f) {
                currentScore += 30
                reasons.add("تطابق أو تشابه كبير في الاسم التجاري (${(nameSim * 100).toInt()}%) (+30)")
            } else if (nameSim >= 0.50f) {
                currentScore += 20
                reasons.add("تشابه جزئي أو تطابق عبر الترجمة/التعريب (${(nameSim * 100).toInt()}%) (+20)")
            }

            // 6. Address / Area Match (+25)
            if (candNormAddress.isNotBlank() && existNormAddress.isNotBlank()) {
                if (candNormAddress == existNormAddress || candidate.area.isNotBlank() && candidate.area == existing.area) {
                    currentScore += 25
                    reasons.add("تطابق في المنطقة والعنوان الجغرافي (+25)")
                }
            }

            // 7. Geographic Distance Proximity (+20 if < 150 meters)
            val distanceMeters = calculateDistanceMeters(
                candidate.latitude, candidate.longitude,
                existing.latitude, existing.longitude
            )
            if (candidate.latitude > 0.0 && existing.latitude > 0.0 && distanceMeters < 150.0) {
                currentScore += 20
                reasons.add("تقارب جغرافي شديد (أقل من 150 متر) (+20)")
            }

            // --- BRANCH PROTECTION & VERIFICATION ---
            // If the name is strongly matching (>= 0.65) BUT the phone numbers are distinctly different AND addresses/locations differ:
            // DO NOT AUTO MERGE - Mark as a potential branch!
            val hasDifferentPhones = !candNormPhone.isNullOrBlank() && existNormPhone.isNotBlank() && candNormPhone != existNormPhone
            val hasDifferentLocations = (candNormAddress.isNotBlank() && existNormAddress.isNotBlank() && candNormAddress != existNormAddress) || distanceMeters > 300.0

            if (nameSim >= 0.65f && hasDifferentPhones && hasDifferentLocations) {
                isBranchDetected = true
                branchNameDetected = "فرع ${candidate.area.ifBlank { candidate.address.ifBlank { candidate.city } }}"
                reasons.add("🛡️ حماية الفروع: تم رصد فرع مستقل لنفس المنشأة (اسم متطابق مع هاتف وعنوان مختلف) - لن يتم الدمج التلقائي")
                // Reduce score to prevent accidental auto-merge
                currentScore = min(35, currentScore)
            }

            if (currentScore > highestScore) {
                highestScore = currentScore
                bestMatch = existing
                bestReasons = reasons
            }
        }

        // Cap score at 100
        val finalScore = min(100, highestScore)

        // Classify candidate status based on score
        val status = when {
            isBranchDetected -> CandidateStatus.POSSIBLE_DUPLICATE
            finalScore >= 90 -> CandidateStatus.DEFINITE_DUPLICATE
            finalScore in 70..89 -> CandidateStatus.LIKELY_DUPLICATE
            finalScore in 40..69 -> CandidateStatus.POSSIBLE_DUPLICATE
            else -> CandidateStatus.NEW
        }

        return DeduplicationResult(
            duplicateScore = finalScore,
            status = status,
            matchedBusinessId = bestMatch?.id,
            matchedBusinessName = bestMatch?.name,
            matchReasons = bestReasons,
            isBranch = isBranchDetected,
            branchName = branchNameDetected
        )
    }

    /**
     * Resolves and consolidates a raw multi-source batch of candidates into a unified single candidate.
     * E.g. Record 1 (Google) + Record 2 (Facebook) + Record 3 (Website) + Record 4 (Yellow Pages)
     * -> 1 Single Candidate with 4 BusinessSource records attached!
     */
    fun consolidateMultiSourceBatch(
        rawBatch: List<CandidateBusiness>
    ): CandidateBusiness {
        if (rawBatch.isEmpty()) throw IllegalArgumentException("Batch cannot be empty")
        if (rawBatch.size == 1) return rawBatch.first()

        // Use highest quality or official source as master
        val sorted = rawBatch.sortedByDescending { it.primarySource.defaultReliability }
        val master = sorted.first()

        var finalPhone = master.phone
        var finalNormalizedPhone = master.normalizedPhone
        var finalDisplayPhone = master.displayPhone
        var finalSecondaryPhone = master.secondaryPhone
        var finalWhatsapp = master.whatsapp
        var finalWebsite = master.website
        var finalFacebook = master.facebookPage
        var finalAddress = master.address
        var finalArea = master.area
        var finalWorkingHours = master.workingHours
        var finalGooglePlaceId = master.googlePlaceId

        val sourcesList = mutableListOf<String>()
        val fieldProvenances = master.fieldProvenances.toMutableMap()

        for (item in rawBatch) {
            val srcKey = item.primarySource.name
            sourcesList.add(srcKey)

            if (finalPhone.isNullOrBlank() && !item.phone.isNullOrBlank()) {
                finalPhone = item.phone
                finalNormalizedPhone = item.normalizedPhone
                finalDisplayPhone = item.displayPhone
                fieldProvenances["phone"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalSecondaryPhone.isNullOrBlank() && !item.secondaryPhone.isNullOrBlank()) {
                finalSecondaryPhone = item.secondaryPhone
                fieldProvenances["phoneSecondary"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalWhatsapp.isNullOrBlank() && !item.whatsapp.isNullOrBlank()) {
                finalWhatsapp = item.whatsapp
                fieldProvenances["whatsapp"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalWebsite.isNullOrBlank() && !item.website.isNullOrBlank()) {
                finalWebsite = item.website
                fieldProvenances["website"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalFacebook.isNullOrBlank() && !item.facebookPage.isNullOrBlank()) {
                finalFacebook = item.facebookPage
                fieldProvenances["facebookPage"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalAddress.isBlank() && item.address.isNotBlank()) {
                finalAddress = item.address
                fieldProvenances["address"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalArea.isBlank() && item.area.isNotBlank()) {
                finalArea = item.area
                fieldProvenances["area"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalWorkingHours.isBlank() && item.workingHours.isNotBlank()) {
                finalWorkingHours = item.workingHours
                fieldProvenances["workingHours"] = "$srcKey:${item.sourceUrl ?: ""}"
            }
            if (finalGooglePlaceId.isNullOrBlank() && !item.googlePlaceId.isNullOrBlank()) {
                finalGooglePlaceId = item.googlePlaceId
            }
        }

        val enriched = master.copy(
            phone = finalPhone,
            normalizedPhone = finalNormalizedPhone,
            displayPhone = finalDisplayPhone,
            secondaryPhone = finalSecondaryPhone,
            whatsapp = finalWhatsapp,
            website = finalWebsite,
            facebookPage = finalFacebook,
            address = finalAddress,
            area = finalArea,
            workingHours = finalWorkingHours,
            googlePlaceId = finalGooglePlaceId,
            sourceIds = sourcesList.distinct(),
            fieldProvenances = fieldProvenances,
            verificationStatus = VerificationState.VERIFIED
        )

        val quality = DataEnrichmentAndMergingEngine.calculateQualityScore(enriched)
        return enriched.copy(qualityScore = quality)
    }

    /**
     * Calculates Jaccard + Levenshtein + Cross-Lingual Transliteration similarity between two strings.
     */
    fun calculateNameSimilarity(s1: String, s2: String): Float {
        if (s1.trim().equals(s2.trim(), ignoreCase = true)) return 1.0f
        if (s1.isBlank() || s2.isBlank()) return 0.0f

        val norm1 = ArabicNormalizer.normalize(s1)
        val norm2 = ArabicNormalizer.normalize(s2)

        if (norm1 == norm2) return 1.0f

        val words1 = expandAndTranslateTokens(norm1)
        val words2 = expandAndTranslateTokens(norm2)

        if (words1.isEmpty() || words2.isEmpty()) return 0.0f

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        val jaccard = intersection.toFloat() / union.toFloat()

        // Levenshtein ratio on normalized roots
        val levDist = levenshteinDistance(norm1, norm2)
        val maxLen = max(norm1.length, norm2.length)
        val levRatio = if (maxLen > 0) 1.0f - (levDist.toFloat() / maxLen.toFloat()) else 0.0f

        return max(jaccard, levRatio)
    }

    private fun expandAndTranslateTokens(text: String): Set<String> {
        val tokens = text.split(" ", "-", "_", "/", "\\", ".", "&", "|")
            .map { it.trim().lowercase() }
            .filter { it.length > 1 }

        val expanded = mutableSetOf<String>()
        for (token in tokens) {
            val normalizedToken = ArabicNormalizer.normalize(token)
            expanded.add(normalizedToken)

            // Check transliteration/translation map
            val translated = TRANSLITERATION_MAP[token] ?: TRANSLITERATION_MAP[normalizedToken]
            if (translated != null) {
                expanded.add(ArabicNormalizer.normalize(translated))
            }
        }
        return expanded
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Haversine formula for geographic distance calculation.
     */
    fun calculateDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
