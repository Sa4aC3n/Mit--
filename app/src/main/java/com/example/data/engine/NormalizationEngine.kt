package com.example.data.engine

import com.example.util.ArabicNormalizer
import java.net.URI

object NormalizationEngine {

    /**
     * Cleans and normalizes business name (Arabic and English).
     * Removes excessive punctuation, extraneous quotes, and standardizes spacing.
     */
    fun cleanBusinessName(rawName: String): String {
        if (rawName.isBlank()) return ""
        var cleaned = rawName.trim()
            .replace(Regex("[\\\"'«»“”„]"), "")
            .replace(Regex("\\s+"), " ")

        // Remove redundant trailing suffixes like " - فرع ميت غمر" if redundant, but preserve branch indications
        cleaned = cleaned.replace(Regex("(?i)facebook|fb page|صفحة رسمية|أونلاين"), "").trim()
        return cleaned
    }

    /**
     * Standardizes Egyptian phone numbers into:
     * - normalizedPhone: E.164 without prefix or standardized format (e.g. 01012345678, 0506912345)
     * - displayPhone: formatted for user view (e.g. 010 1234 5678 or 050 691 2345)
     */
    data class PhoneNormalizationResult(
        val normalized: String,
        val display: String,
        val isValid: Boolean,
        val type: String // "MOBILE", "LANDLINE", "HOTLINE"
    )

    fun normalizePhone(rawPhone: String?): PhoneNormalizationResult? {
        if (rawPhone.isNullOrBlank()) return null
        val digitsOnly = rawPhone.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.length < 3) return null

        var localDigits = digitsOnly
        if (localDigits.startsWith("+20")) {
            localDigits = "0" + localDigits.substring(3)
        } else if (localDigits.startsWith("0020")) {
            localDigits = "0" + localDigits.substring(4)
        } else if (localDigits.startsWith("20") && localDigits.length >= 11) {
            localDigits = "0" + localDigits.substring(2)
        }

        // Hotlines (5 digits like 19xxx, 16xxx)
        if (localDigits.length == 5 && (localDigits.startsWith("19") || localDigits.startsWith("16") || localDigits.startsWith("15"))) {
            return PhoneNormalizationResult(
                normalized = localDigits,
                display = localDigits,
                isValid = true,
                type = "HOTLINE"
            )
        }

        // Egyptian Mobile (010, 011, 012, 015 - 11 digits)
        if (localDigits.length == 11 && (localDigits.startsWith("010") || localDigits.startsWith("011") || localDigits.startsWith("012") || localDigits.startsWith("015"))) {
            val formatted = "${localDigits.substring(0, 4)} ${localDigits.substring(4, 7)} ${localDigits.substring(7)}"
            return PhoneNormalizationResult(
                normalized = localDigits,
                display = formatted,
                isValid = true,
                type = "MOBILE"
            )
        }

        // Dakahlia Landline (050xxxxxxx - 9-10 digits)
        if (localDigits.startsWith("050") && localDigits.length in 9..10) {
            val formatted = "${localDigits.substring(0, 3)} ${localDigits.substring(3, 6)} ${localDigits.substring(6)}"
            return PhoneNormalizationResult(
                normalized = localDigits,
                display = formatted,
                isValid = true,
                type = "LANDLINE"
            )
        }

        // Default fallback if plausible number
        if (localDigits.length in 7..14) {
            return PhoneNormalizationResult(
                normalized = localDigits,
                display = localDigits,
                isValid = true,
                type = if (localDigits.startsWith("01")) "MOBILE" else "LANDLINE"
            )
        }

        return null
    }

    /**
     * Normalizes and cleans URLs:
     * - Strips tracking parameters (utm_*, fbclid, gclid, ref)
     * - Removes trailing slashes
     * - Ensures valid protocol (https://)
     */
    fun cleanUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        var url = rawUrl.trim()
        if (url.equals("غير متوفر", ignoreCase = true) || url.equals("null", ignoreCase = true) || url.equals("n/a", ignoreCase = true)) {
            return null
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        try {
            val uri = URI(url)
            val host = uri.host ?: return null
            val path = uri.path ?: ""
            // Remove tracking params
            val cleanQuery = uri.query?.split("&")?.filterNot { param ->
                param.startsWith("utm_") || param.startsWith("fbclid") || param.startsWith("gclid") || param.startsWith("ref")
            }?.joinToString("&")

            val finalUrl = StringBuilder("https://").append(host)
            if (path.isNotEmpty() && path != "/") {
                finalUrl.append(path.removeSuffix("/"))
            }
            if (!cleanQuery.isNullOrBlank()) {
                finalUrl.append("?").append(cleanQuery)
            }
            return finalUrl.toString()
        } catch (e: Exception) {
            return if (url.length > 8 && url.contains(".")) url else null
        }
    }

    /**
     * Standardizes working hours into clean readable Arabic.
     */
    fun formatWorkingHours(hoursStr: String?): String {
        if (hoursStr.isNullOrBlank()) return "مواعيد العمل المعتادة"
        var text = hoursStr.trim()
        if (text.contains("24") && (text.contains("ساعة") || text.contains("hours") || text.contains("طوال"))) {
            return "مفتوح 24 ساعة (طوال اليوم)"
        }
        text = text.replace("AM", "ص").replace("PM", "م")
            .replace("am", "ص").replace("pm", "م")
        return text
    }

    /**
     * Extracts Domain Name for duplicate detection.
     */
    fun extractDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = URI(cleanUrl(url) ?: return null)
            val host = uri.host ?: return null
            host.removePrefix("www.").lowercase()
        } catch (e: Exception) {
            null
        }
    }
}
