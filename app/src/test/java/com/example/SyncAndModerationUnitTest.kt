package com.example

import com.example.data.engine.EntityResolutionAndDeduplicationEngine
import com.example.data.model.BusinessEntity
import com.example.data.model.CandidateBusiness
import com.example.data.model.ContributionStatus
import com.example.data.model.ContributionType
import com.example.util.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncAndModerationUnitTest {

    @Test
    fun testArabicNormalization() {
        val raw = "  صيدلية الإيمان الحديثة ة ى   "
        val normalized = ArabicNormalizer.normalize(raw)
        // Alif normalized to ا, Taa Marbouta to ه, Yaa/Alif Maqsoora to ي
        assertEquals("صيدليه الايمان الحديثه ه ي", normalized)
    }

    @Test
    fun testPhoneNormalization() {
        assertEquals("01012345678", ArabicNormalizer.normalizePhone("+201012345678"))
        assertEquals("01012345678", ArabicNormalizer.normalizePhone("00201012345678"))
        assertEquals("01012345678", ArabicNormalizer.normalizePhone("010-1234-5678"))
        assertEquals("01012345678", ArabicNormalizer.normalizePhone("201012345678"))
    }

    @Test
    fun testCandidateDeduplicationMatchByPhoneAndName() {
        val existingBusiness = BusinessEntity(
            id = "biz_001",
            name = "صيدلية الإيمان",
            categoryId = "pharmacies",
            categoryName = "صيدليات",
            specialty = "صيدلية وخدمات دوائية",
            description = "صيدلية متكاملة بميت غمر",
            phone = "01012345678",
            address = "شارع بورسعيد، ميت غمر",
            area = "وسط البلد",
            workingHours = "24 ساعة",
            ratingAverage = 4.8f,
            ratingCount = 10
        )

        val candidate = CandidateBusiness(
            id = "cand_001",
            businessName = "صيدليه الايمان الحديثة",
            categoryId = "pharmacies",
            categoryName = "صيدليات",
            phone = "+201012345678",
            normalizedPhone = ArabicNormalizer.normalizePhone("+201012345678"),
            address = "ميت غمر - شارع بورسعيد"
        )

        val result = EntityResolutionAndDeduplicationEngine.evaluateCandidate(
            candidate = candidate,
            existingBusinesses = listOf(existingBusiness)
        )

        assertNotNull(result.matchedBusinessId)
        assertEquals("biz_001", result.matchedBusinessId)
        assertTrue(result.duplicateScore >= 50)
    }

    @Test
    fun testContributionLifecycleStatuses() {
        assertEquals("PENDING", ContributionStatus.PENDING.name)
        assertEquals("APPROVED", ContributionStatus.APPROVED.name)
        assertEquals("REJECTED", ContributionStatus.REJECTED.name)
        assertEquals("ADD_BUSINESS", ContributionType.ADD_BUSINESS.name)
        assertEquals("SUGGEST_EDIT", ContributionType.SUGGEST_EDIT.name)
    }
}
