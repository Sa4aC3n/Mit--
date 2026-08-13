package com.example.util

object ArabicNormalizer {

    /**
     * Normalizes Arabic text for search matching:
     * - Unifies Alif forms (أ, إ, آ -> ا)
     * - Unifies Yaa and Alif Maqsoora (ى -> ي)
     * - Unifies Taa Marbouta and Haa (ة -> ه)
     * - Removes Arabic diacritics (Tashkeel)
     * - Trims & converts English to lowercase
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""

        var result = text.trim().lowercase()

        // Remove Arabic Diacritics (Tashkeel)
        result = result.replace(Regex("[\\u064B-\\u0652]"), "")

        // Normalize Alif
        result = result.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')

        // Normalize Yaa / Alif Maqsoora
        result = result.replace('ى', 'ي')

        // Normalize Taa Marbouta / Haa
        result = result.replace('ة', 'ه')

        // Normalize spaces
        result = result.replace(Regex("\\s+"), " ")

        return result
    }

    /**
     * Normalizes phone numbers for searching by removing spaces, dashes, and country prefixes (+20, 0020)
     */
    fun normalizePhone(phone: String): String {
        var cleaned = phone.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("+20")) {
            cleaned = "0" + cleaned.substring(3)
        } else if (cleaned.startsWith("0020")) {
            cleaned = "0" + cleaned.substring(4)
        } else if (cleaned.startsWith("20") && cleaned.length > 10) {
            cleaned = "0" + cleaned.substring(2)
        }
        return cleaned
    }

    /**
     * Checks if target text contains normalized search query or query words
     */
    fun matches(target: String, query: String): Boolean {
        if (query.isBlank()) return true
        val normalizedTarget = normalize(target)
        val normalizedQuery = normalize(query)

        if (normalizedTarget.contains(normalizedQuery)) return true

        // Split query into words for multi-word search
        val queryWords = normalizedQuery.split(" ").filter { it.length > 1 }
        if (queryWords.isEmpty()) return false

        return queryWords.all { word -> normalizedTarget.contains(word) }
    }

    /**
     * Calculates relevance score for ranking search results
     * Higher score = better match
     */
    fun calculateRelevanceScore(
        businessName: String,
        specialty: String,
        categoryName: String,
        area: String,
        description: String,
        phone: String,
        query: String
    ): Int {
        if (query.isBlank()) return 0

        val normQuery = normalize(query)
        val normName = normalize(businessName)
        val normSpec = normalize(specialty)
        val normCat = normalize(categoryName)
        val normArea = normalize(area)
        val normDesc = normalize(description)
        val normPhone = normalizePhone(phone)
        val cleanedQueryPhone = normalizePhone(query)

        var score = 0

        // Exact Name Match
        if (normName == normQuery) score += 100
        else if (normName.startsWith(normQuery)) score += 80
        else if (normName.contains(normQuery)) score += 60

        // Specialty / Subcategory Match
        if (normSpec == normQuery) score += 70
        else if (normSpec.contains(normQuery)) score += 50

        // Category Match
        if (normCat == normQuery) score += 40
        else if (normCat.contains(normQuery)) score += 30

        // Phone Match
        if (cleanedQueryPhone.isNotEmpty() && normPhone.contains(cleanedQueryPhone)) score += 90

        // Area Match
        if (normArea.contains(normQuery)) score += 20

        // Description Match
        if (normDesc.contains(normQuery)) score += 10

        return score
    }
}
