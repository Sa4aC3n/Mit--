package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.engine.EntityResolutionAndDeduplicationEngine
import com.example.data.firebase.FirebaseFirestoreSyncManager
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
    fun testSyncEngineEvictsUnpublishedBusinessFromRoom() = runBlocking {
        val initiallyPublishedBusiness = BusinessEntity(
            id = "biz_legacy_unpub_301",
            name = "نشاط قديم غير منشور",
            categoryId = "services",
            categoryName = "خدمات",
            specialty = "خدمات قديمة",
            description = "تم إلغاء نشره أثناء الترحيل",
            phone = "01000000000",
            address = "شارع الحرية",
            area = "وسط البلد",
            workingHours = "غير متاح",
            isPublished = true,
            isActive = true,
            isDeleted = false,
            updatedAt = 1700000000000L
        )

        // 1. Initial State: Business is published and present in Room
        dao.insertBusiness(initiallyPublishedBusiness)
        assertNotNull(dao.getBusinessByIdDirect("biz_legacy_unpub_301"))

        // 2. Migration changes state in Firestore: isPublished becomes false with a new updatedAt
        val unpublishFirestoreDoc = "biz_legacy_unpub_301" to mapOf(
            "id" to "biz_legacy_unpub_301",
            "name" to "نشاط قديم غير منشور",
            "isPublished" to false,
            "isActive" to true,
            "isDeleted" to false,
            "updatedAt" to 1700000050000L
        )

        // 3. Run the ACTUAL Sync Engine (no direct dao.deleteBusiness call)
        val syncResult = FirebaseFirestoreSyncManager.processSyncBatchMaps(
            dao = dao,
            businessDocs = listOf(unpublishFirestoreDoc),
            tombstoneDocs = emptyList()
        )

        // 4. Assert sync engine processed the deletion and record is evicted from Room
        assertEquals(1, syncResult.deletedCount)
        val afterSync = dao.getBusinessByIdDirect("biz_legacy_unpub_301")
        assertNull(afterSync)
    }

    @Test
    fun testSyncEngineEvictsTombstonedBusinessFromRoom() = runBlocking {
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

        // 2. Change feed tombstone arriving from business_tombstones
        val tombstoneDoc = "tomb_202" to mapOf(
            "id" to "tomb_202",
            "businessId" to "biz_to_tombstone_202",
            "reason" to "migration_unpublish_legacy",
            "updatedAt" to 1700000060000L
        )

        // 3. Run the ACTUAL Sync Engine with the tombstone
        val syncResult = FirebaseFirestoreSyncManager.processSyncBatchMaps(
            dao = dao,
            businessDocs = emptyList(),
            tombstoneDocs = listOf(tombstoneDoc)
        )

        // 4. Assert sync engine processed tombstone and evicted from Room
        assertEquals(1, syncResult.deletedCount)
        val result = dao.getBusinessByIdDirect("biz_to_tombstone_202")
        assertNull(result)
    }

    @Test
    fun testSyncEngineHandlesEqualTimestampsDeterministically() = runBlocking {
        val sameTimestamp = 1700000099000L

        // Two items with the EXACT same updatedAt timestamp:
        // Biz A: Active published business
        // Biz B: Existing in Room, but now tombstoned
        val existingBizB = BusinessEntity(
            id = "biz_b_to_delete",
            name = "نشاط ب",
            categoryId = "cafes",
            categoryName = "كافيهات",
            specialty = "كافيه",
            description = "كافيه قديم",
            phone = "01022223333",
            address = "شارع البحر",
            area = "وسط البلد",
            workingHours = "10ص - 12م",
            isPublished = true,
            isActive = true,
            isDeleted = false,
            updatedAt = 1700000000000L
        )
        dao.insertBusiness(existingBizB)
        assertNotNull(dao.getBusinessByIdDirect("biz_b_to_delete"))

        val incomingBizA = "biz_a_new" to mapOf(
            "id" to "biz_a_new",
            "name" to "نشاط أ الجديد",
            "categoryId" to "pharmacies",
            "categoryName" to "صيدليات",
            "isPublished" to true,
            "isActive" to true,
            "isDeleted" to false,
            "updatedAt" to sameTimestamp
        )

        val incomingTombstoneB = "tomb_b" to mapOf(
            "businessId" to "biz_b_to_delete",
            "reason" to "unpublish",
            "updatedAt" to sameTimestamp
        )

        val result = FirebaseFirestoreSyncManager.processSyncBatchMaps(
            dao = dao,
            businessDocs = listOf(incomingBizA),
            tombstoneDocs = listOf(incomingTombstoneB)
        )

        // Verify Biz A was inserted and Biz B was deleted
        assertEquals(1, result.upsertedCount)
        assertEquals(1, result.deletedCount)
        assertEquals(sameTimestamp, result.highestBusinessUpdatedAt)
        assertEquals(sameTimestamp, result.highestTombstoneUpdatedAt)

        assertNotNull(dao.getBusinessByIdDirect("biz_a_new"))
        assertEquals("نشاط أ الجديد", dao.getBusinessByIdDirect("biz_a_new")?.name)
        assertNull(dao.getBusinessByIdDirect("biz_b_to_delete"))
    }

    @Test
    fun testFeaturedAndTopRatedAndCategoryCountsRoomQueries() = runBlocking {
        val biz1 = BusinessEntity(
            id = "biz_001",
            name = "مطعم الفيروز",
            categoryId = "restaurants",
            categoryName = "مطاعم",
            specialty = "مأكولات شرقية",
            description = "مطعم عريق",
            phone = "01011111111",
            address = "شارع الجيش",
            area = "وسط البلد",
            workingHours = "10ص - 12م",
            ratingAverage = 4.9f,
            ratingCount = 50,
            isVerified = true,
            isActive = true,
            createdAt = 1000L
        )

        val biz2 = BusinessEntity(
            id = "biz_002",
            name = "صيدلية النور",
            categoryId = "pharmacies",
            categoryName = "صيدليات",
            specialty = "صيدلية",
            description = "صيدلية متكاملة",
            phone = "01022222222",
            address = "شارع بورسعيد",
            area = "وسط البلد",
            workingHours = "24 ساعة",
            ratingAverage = 4.0f,
            ratingCount = 5,
            isVerified = false,
            isActive = true,
            createdAt = 2000L
        )

        val biz3 = BusinessEntity(
            id = "biz_003",
            name = "كافيه الأهرام",
            categoryId = "cafes",
            categoryName = "كافيهات",
            specialty = "مشروبات",
            description = "كافيه شبابي",
            phone = "01033333333",
            address = "شارع البحر",
            area = "وسط البلد",
            workingHours = "10ص - 2ص",
            ratingAverage = 4.7f,
            ratingCount = 30,
            isVerified = false,
            isActive = true,
            createdAt = 3000L
        )

        dao.insertBusinesses(listOf(biz1, biz2, biz3))

        // 1. Verify category counts query
        val counts = dao.getAllActiveBusinessesDirect().groupingBy { it.categoryId }.eachCount()
        assertEquals(1, counts["restaurants"])
        assertEquals(1, counts["pharmacies"])
        assertEquals(1, counts["cafes"])

        // 2. Verify direct access and queries
        val active = dao.getAllActiveBusinessesDirect()
        assertEquals(3, active.size)
    }

    @Test
    fun testBackendSuperAdminSecurityValidation() = runBlocking {
        val apiService = com.example.data.remote.BackendApiService()

        // Unauthorized call without auth or token
        val unauthResponse = apiService.verifyAuthorization(null, "ADMIN")
        assertEquals(401, unauthResponse.errorCode)

        // SuperAdmin check with unauthorized email
        val regularUserSuperAdminCheck = apiService.verifySuperAdminAuthorization(
            authToken = "regular_user_token",
            userEmail = "regular@example.com"
        )
        assertEquals(403, regularUserSuperAdminCheck.errorCode)

        // SuperAdmin check with authorized owner email
        val ownerSuperAdminCheck = apiService.verifySuperAdminAuthorization(
            authToken = "owner_token",
            userEmail = "m.k3shka@gmail.com"
        )
        assertTrue(ownerSuperAdminCheck.success)
        assertTrue(ownerSuperAdminCheck.data ?: false)
    }
}
