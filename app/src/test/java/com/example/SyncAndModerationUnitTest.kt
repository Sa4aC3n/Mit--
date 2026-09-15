package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.EntityResolutionAndDeduplicationEngine
import com.example.data.local.AppDatabase
import com.example.data.local.DirectoryDao
import com.example.data.model.BusinessEntity
import com.example.data.model.CandidateBusiness
import com.example.data.model.ContributionStatus
import com.example.data.model.ContributionType
import com.example.util.ArabicNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncAndModerationUnitTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DirectoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.directoryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

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

    @Test
    fun testRoomLocalPersistenceAfterSync() = runBlocking {
        val publishedBusiness = BusinessEntity(
            id = "biz_synced_101",
            name = "مطعم ومشاوي المنصورة بميت غمر",
            categoryId = "restaurants",
            categoryName = "مطاعم",
            specialty = "مشويات وطواجن",
            description = "أشهى المأكولات والمشويات",
            phone = "01099887766",
            phoneSecondary = "0506912345",
            whatsapp = "01099887766",
            city = "مدينة ميت غمر",
            address = "شارع الحرية، ميت غمر",
            area = "وسط البلد",
            workingHours = "10:00 ص - 01:00 ص",
            isOpenNow = true,
            isVerified = true,
            isActive = true,
            isPublished = true,
            isDeleted = false,
            ratingAverage = 4.9f,
            ratingCount = 15,
            viewCount = 42,
            verificationStatus = "VERIFIED",
            dataQualityScore = 95,
            createdAt = 1700000000000L,
            updatedAt = 1700000050000L
        )

        // 1. Simulate saving incoming delta sync item into local Room cache
        dao.insertBusiness(publishedBusiness)

        val cached = dao.getBusinessByIdDirect("biz_synced_101")
        assertNotNull(cached)
        assertEquals("biz_synced_101", cached?.id)
        assertEquals("مطعم ومشاوي المنصورة بميت غمر", cached?.name)
        assertEquals(true, cached?.isPublished)
        assertEquals(false, cached?.isDeleted)
        assertEquals(1700000050000L, cached?.updatedAt)
    }

    @Test
    fun testRoomTombstoneDeletionRemovesLocalRecord() = runBlocking {
        val businessToTombstone = BusinessEntity(
            id = "biz_to_tombstone_202",
            name = "محل مغلق للبيع",
            categoryId = "retail",
            categoryName = "محلات تجارية",
            specialty = "محلات تجارية وتصفية",
            description = "محل تجاري مغلق",
            phone = "01011223344",
            address = "شارع الجيش، ميت غمر",
            area = "وسط البلد",
            workingHours = "مغلق",
            isPublished = true,
            isActive = true,
            isDeleted = false,
            updatedAt = 1700000010000L
        )

        // 1. Seed initially in Room
        dao.insertBusiness(businessToTombstone)
        assertNotNull(dao.getBusinessByIdDirect("biz_to_tombstone_202"))

        // 2. Incoming delta sync receives tombstone (isDeleted=true / ARCHIVED / DELETED)
        // In FirebaseFirestoreSyncManager:
        // if (isDeleted || verificationStatus == "ARCHIVED" || verificationStatus == "DELETED" || !isActive) {
        //     dao.deleteBusiness(delId)
        // }
        dao.deleteBusiness("biz_to_tombstone_202")

        // 3. Verify local copy is completely removed from Room cache
        val result = dao.getBusinessByIdDirect("biz_to_tombstone_202")
        assertNull(result)
    }
}
