package com.example.data.ai

import com.example.data.model.BusinessEntity
import com.example.data.model.RecommendationExplanation
import com.example.data.model.AiStructuredQuery
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

object AiRecommendationEngine {

    // Center coordinates of Met Ghamr, Dakahlia
    const val MET_GHAMR_CENTER_LAT = 30.7198
    const val MET_GHAMR_CENTER_LNG = 31.2582

    private data class ScoredCandidate(
        val business: BusinessEntity,
        val totalScore: Double,
        val explanation: RecommendationExplanation
    )

    /**
     * Ranks business candidates based on multi-factor scoring algorithm:
     * - Rating (0-5)
     * - Review Count (Weighted log/cap)
     * - Open Now status boost
     * - Proximity distance score (if user coordinates provided)
     * - Verification status boost
     * - Specialty / Name keyword match
     */
    fun rankAndExplain(
        candidates: List<BusinessEntity>,
        query: AiStructuredQuery,
        userLat: Double?,
        userLng: Double?,
        calendar: Calendar = Calendar.getInstance()
    ): Pair<List<BusinessEntity>, List<RecommendationExplanation>> {
        val scoredList = candidates.map { business ->
            val ratingScore = (business.ratingAverage * 15.0).toDouble() // Max 75 pts
            val reviewsWeight = (min(business.ratingCount, 300) * 0.05).toDouble() // Max 15 pts

            val isCurrentlyOpen = isBusinessOpenNow(business.workingHours, calendar)
            val openNowBonus = if (query.openNowOnly && isCurrentlyOpen) 25.0 else if (isCurrentlyOpen) 5.0 else 0.0

            val verificationBonus = if (business.isVerified) 10.0 else 0.0

            var distanceKm: Double? = null
            var distanceBonus = 0.0
            if (userLat != null && userLng != null && business.latitude != 0.0 && business.longitude != 0.0) {
                distanceKm = calculateDistanceKm(userLat, userLng, business.latitude, business.longitude)
                if (query.isNearestRequested || query.intent == "NEAREST") {
                    // Maximum 30 pts bonus for very close proximity (decreases by 5 pts per km)
                    distanceBonus = max(0.0, 30.0 - (distanceKm * 5.0))
                }
            }

            // Keyword match bonus
            var keywordBonus = 0.0
            if (!query.specialty.isNullOrBlank() &&
                (business.specialty.contains(query.specialty, ignoreCase = true) ||
                 business.name.contains(query.specialty, ignoreCase = true))
            ) {
                keywordBonus += 15.0
            }

            val totalScore = ratingScore + reviewsWeight + openNowBonus + verificationBonus + distanceBonus + keywordBonus

            val explanationSummary = buildString {
                append("تقييم ${business.ratingAverage}⭐ (من ${business.ratingCount} تقييم)")
                if (isCurrentlyOpen) append(" • مفتوح الآن 🕐")
                if (distanceKm != null) append(" • المسافة: ${"%.1f".format(Locale.ENGLISH, distanceKm)} كم")
                if (business.isVerified) append(" • موثّق رسمياً ✔️")
            }

            val explanation = RecommendationExplanation(
                businessId = business.id,
                businessName = business.name,
                totalScore = totalScore,
                ratingScore = ratingScore,
                reviewsWeight = reviewsWeight,
                distanceKm = distanceKm,
                distanceBonus = distanceBonus,
                openNowBonus = openNowBonus,
                verificationBonus = verificationBonus,
                summaryAr = explanationSummary
            )

            ScoredCandidate(business, totalScore, explanation)
        }

        // Sort by total score descending
        val sortedCandidates = scoredList.sortedByDescending { it.totalScore }
        val sortedBusinesses = sortedCandidates.map { it.business }
        val sortedExplanations = sortedCandidates.map { it.explanation }

        return Pair(sortedBusinesses, sortedExplanations)
    }

    /**
     * Determines whether a business is currently open based on working hours string & current time
     */
    fun isBusinessOpenNow(workingHours: String, now: Calendar = Calendar.getInstance()): Boolean {
        return com.example.util.WorkingHoursUtils.isBusinessOpenNow(workingHours, now)
    }

    /**
     * Calculates distance in kilometers between two GPS coordinates using Haversine formula
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
