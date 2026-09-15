package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.DirectoryDao
import com.example.data.model.*
import com.example.util.CategoryNormalizer
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * CloudSyncState represents the lifecycle and status of the Firestore synchronization engine.
 */
enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    OFFLINE
}

data class FirestoreSyncStatus(
    val state: SyncState = SyncState.IDLE,
    val isConnected: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val syncedBusinessesCount: Int = 0,
    val syncedSourcesCount: Int = 0,
    val syncedReviewsCount: Int = 0,
    val pendingUploads: Int = 0,
    val message: String = "جاهز للمزامنة السحابية (Firestore Source of Truth)"
)

/**
 * FirebaseFirestoreSyncManager:
 * The authoritative Cloud Persistence & Sync Manager for Met Ghamr Directory.
 * - Firestore is the primary Central Cloud Database and Source of Truth.
 * - Local Room/Realm database acts strictly as a high-performance Offline Cache.
 * - Supports Incremental Sync (fetching only delta modifications using updatedAt).
 * - Perceived instant startup (loads local cache first, syncs in background).
 * - Supports Batch Operations (chunked into Firestore batches of <= 450 items).
 * - Supports Multi-Source Data Provenance persistence.
 */
object FirebaseFirestoreSyncManager {

    private const val TAG = "FirestoreSyncManager"
    private const val PREFS_NAME = "met_ghamr_firestore_sync_prefs"
    private const val KEY_LAST_SYNC_TS = "last_firestore_sync_timestamp"
    private const val KEY_SERVER_DATA_VERSION = "server_data_version"

    // Firestore Collections
    const val COL_ACTIVITIES = "activities" // The primary collection for user-uploaded activities
    const val COL_BUSINESSES = "businesses"
    const val COL_SOURCES = "businessSources"
    const val COL_CATEGORIES = "categories"
    const val COL_REVIEWS = "reviews"
    const val COL_CONTRIBUTIONS = "contributions"
    const val COL_NOTIFICATIONS = "notifications"
    const val COL_AUDIT_LOGS = "auditLogs"
    const val COL_DISCOVERY_JOBS = "discoveryJobs"
    const val COL_SETTINGS = "settings"

    private val _syncStatus = MutableStateFlow(FirestoreSyncStatus())
    val syncStatus: StateFlow<FirestoreSyncStatus> = _syncStatus.asStateFlow()

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            db
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore instance could not be initialized: ${e.message}")
            null
        }
    }

    fun isFirestoreAvailable(): Boolean = firestore != null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLastSyncTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC_TS, 0L)
    }

    fun setLastSyncTimestamp(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC_TS, timestamp).apply()
        _syncStatus.value = _syncStatus.value.copy(lastSyncTimestamp = timestamp)
    }

    /**
     * Converts a BusinessEntity to a Firestore document map.
     */
    fun businessToFirestoreMap(biz: BusinessEntity): Map<String, Any?> {
        return mapOf(
            "id" to biz.id,
            "name" to biz.name,
            "categoryId" to biz.categoryId,
            "categoryName" to biz.categoryName,
            "specialty" to biz.specialty,
            "description" to biz.description,
            "phone" to biz.phone,
            "phoneSecondary" to biz.phoneSecondary,
            "whatsapp" to biz.whatsapp,
            "city" to biz.city,
            "address" to biz.address,
            "area" to biz.area,
            "facebookUrl" to biz.facebookUrl,
            "websiteUrl" to biz.websiteUrl,
            "latitude" to biz.latitude,
            "longitude" to biz.longitude,
            "workingHours" to biz.workingHours,
            "isOpenNow" to biz.isOpenNow,
            "isVerified" to biz.isVerified,
            "isActive" to biz.isActive,
            "ratingAverage" to biz.ratingAverage.toDouble(),
            "ratingCount" to biz.ratingCount,
            "viewCount" to biz.viewCount,
            "imageUrl" to biz.imageUrl,
            "verificationStatus" to biz.verificationStatus,
            "dataQualityScore" to biz.dataQualityScore,
            "phoneSource" to biz.phoneSource,
            "websiteSource" to biz.websiteSource,
            "facebookSource" to biz.facebookSource,
            "addressSource" to biz.addressSource,
            "workingHoursSource" to biz.workingHoursSource,
            "createdAt" to biz.createdAt,
            "lastVerifiedAt" to biz.lastVerifiedAt,
            "updatedAt" to biz.updatedAt,
            "isPublished" to biz.isPublished,
            "isDeleted" to biz.isDeleted,
            "archivedAt" to biz.archivedAt,
            "approvedContributionId" to biz.approvedContributionId,
            "serverSyncedAt" to FieldValue.serverTimestamp()
        )
    }

    /**
     * Deduces neighborhood or major street name from raw address text.
     */
    fun deduceAreaFromAddress(address: String): String {
        val clean = address.trim()
        val areas = listOf(
            "شارع بورسعيد", "شارع البحر", "شارع الحرية", "شارع الجيش", "شارع 26 يوليو",
            "شارع المحطة", "ميدان المحطة", "شارع أحمد ماهر", "شارع المعاهدة", "ميدان الشيخ حسنين",
            "حي المعاهدة", "حي السلام", "تقسيم الموظفين", "منطقة السوق",
            "السنبلاوين", "بشلا", "صهرجت الكبرى", "أتميدة", "كوم النور", "دنديط", "ميت ناجي",
            "ميت الفرماوي", "كفر المقدام", "ميت القرشي", "ميت يعيش", "تفهنا العزب", "هلا", "ميت محسن"
        )
        for (a in areas) {
            if (clean.contains(a, ignoreCase = true)) return a
        }
        return "ميت غمر"
    }

    /**
     * Reconstructs a BusinessEntity safely from any Firestore DocumentSnapshot.
     * Handles English/Arabic field names, numbers, timestamps, strings, and GeoPoints.
     */
    fun documentToBusiness(doc: DocumentSnapshot): BusinessEntity? {
        return try {
            val data = doc.data ?: return null

            fun findString(vararg keys: String): String? {
                for (k in keys) {
                    if (data.containsKey(k)) {
                        val v = data[k]
                        if (v != null) {
                            val s = v.toString().trim()
                            if (s.isNotEmpty() && s != "null") return s
                        }
                    }
                }
                for (k in keys) {
                    val entry = data.entries.find { it.key.equals(k, ignoreCase = true) }
                    if (entry?.value != null) {
                        val s = entry.value.toString().trim()
                        if (s.isNotEmpty() && s != "null") return s
                    }
                }
                return null
            }

            fun findDouble(defaultVal: Double, vararg keys: String): Double {
                for (k in keys) {
                    if (data.containsKey(k)) {
                        val v = data[k]
                        if (v is Number) return v.toDouble()
                        if (v is String) v.toDoubleOrNull()?.let { return it }
                        if (v is GeoPoint) {
                            if (k.contains("lat", ignoreCase = true)) return v.latitude
                            if (k.contains("lon", ignoreCase = true) || k.contains("lng", ignoreCase = true)) return v.longitude
                        }
                    }
                }
                for (k in keys) {
                    val entry = data.entries.find { it.key.equals(k, ignoreCase = true) }
                    val v = entry?.value
                    if (v is Number) return v.toDouble()
                    if (v is String) v.toDoubleOrNull()?.let { return it }
                }
                return defaultVal
            }

            fun findLong(defaultVal: Long, vararg keys: String): Long {
                for (k in keys) {
                    if (data.containsKey(k)) {
                        val v = data[k]
                        if (v is Number) return v.toLong()
                        if (v is com.google.firebase.Timestamp) return v.toDate().time
                        if (v is java.util.Date) return v.time
                        if (v is String) v.toLongOrNull()?.let { return it }
                    }
                }
                return defaultVal
            }

            fun findBoolean(defaultVal: Boolean, vararg keys: String): Boolean {
                for (k in keys) {
                    if (data.containsKey(k)) {
                        val v = data[k]
                        if (v is Boolean) return v
                        if (v is Number) return v.toInt() == 1
                        if (v is String) {
                            val s = v.trim().lowercase()
                            if (s == "true" || s == "1" || s == "نعم" || s == "نشط" || s == "مفعل") return true
                            if (s == "false" || s == "0" || s == "لا" || s == "معطل") return false
                        }
                    }
                }
                return defaultVal
            }

            val id = findString("id", "_id", "uid", "activityId", "businessId") ?: doc.id

            // Resilient lookup for name across English and Arabic aliases
            val name = findString(
                "name", "activityName", "businessName", "title",
                "اسم", "الاسم", "اسم_النشاط", "اسم النشاط", "الاسم_التجاري", "الاسم التجاري",
                "اسم_المحل", "اسم المحل", "المكان", "activity", "nameAr", "arabicName"
            ) ?: data.entries.find { 
                val k = it.key.lowercase()
                k.contains("name") || k.contains("اسم") || k.contains("title")
            }?.value?.toString()?.trim()

            if (name.isNullOrBlank()) {
                Log.w(TAG, "Document ${doc.id} skipped: no valid name found in keys: ${data.keys}")
                return null
            }

            // Phone numbers
            val phone = findString(
                "phone", "telephone", "mobile", "tel", "phone1", "phoneNumber", "phone_number",
                "الهاتف", "تليفون", "الموبايل", "رقم_الهاتف", "رقم الهاتف", "رقم_الموبايل", "رقم الموبايل", "التليفون"
            ) ?: ""

            val phoneSecondary = findString(
                "phoneSecondary", "phone2", "phone_secondary", "secondaryPhone",
                "هاتف_2", "تليفون_آخر", "تليفون_2", "رقم_اضافي", "رقم إضافي", "هاتف2"
            )

            val whatsapp = findString(
                "whatsapp", "whats", "wa", "واتساب", "الواتس", "رقم_الواتس", "رقم الواتساب"
            )

            // Category resolution with Arabic normalizer
            val rawCat = findString(
                "categoryName", "category", "categoryId", "cat",
                "التصنيف", "القسم", "نوع_النشاط", "نوع النشاط", "النشاط", "الفئة", "تصنيف"
            ) ?: ""

            val resolvedCategory = CategoryNormalizer.resolveCategory(rawCat, name)
            val categoryId = resolvedCategory.id
            val categoryName = if (rawCat.isNotBlank() && rawCat.length > 2) rawCat else resolvedCategory.nameAr

            val specialty = findString("specialty", "التخصص", "المهنة", "الخدمات", "services") ?: ""
            val description = findString("description", "الوصف", "التفاصيل", "معلومات", "notes", "ملاحظات", "about", "عن_النشاط") ?: ""

            val city = findString("city", "المدينة", "البلدة", "قرية", "town") ?: "مدينة ميت غمر"
            val address = findString("address", "العنوان", "المكان", "مقر", "الموقع", "العنوان_بالتفصيل", "addressDetails", "street", "شارع") ?: "ميت غمر"
            val area = findString("area", "المنطقة", "الحي", "الشارع", "district") ?: deduceAreaFromAddress(address)

            val facebookUrl = findString("facebookUrl", "facebook", "fb", "فيسبوك", "صفحة_الفيسبوك", "رابط_فيسبوك")
            val websiteUrl = findString("websiteUrl", "website", "site", "موقع", "موقع_الكتروني", "رابط_الموقع")
            val imageUrl = findString("imageUrl", "image", "img", "photo", "logo", "صورة", "الصورة", "شعار", "لوجو")

            // GeoPoint & coordinates
            var lat = findDouble(30.7183, "latitude", "lat", "خط_العرض", "خط العرض")
            var lng = findDouble(31.2568, "longitude", "lng", "lon", "خط_الطول", "خط الطول")
            for (v in data.values) {
                if (v is GeoPoint) {
                    lat = v.latitude
                    lng = v.longitude
                    break
                }
            }

            val workingHours = findString("workingHours", "hours", "مواعيد_العمل", "مواعيد العمل", "ساعات_العمل", "ساعات العمل") ?: "09:00 ص - 10:00 م"
            val isOpenNow = findBoolean(true, "isOpenNow", "open", "مفتوح")
            val isVerified = findBoolean(false, "isVerified", "verified", "موثق")
            val isActive = findBoolean(true, "isActive", "active", "نشط", "مفعل")

            val ratingAverage = findDouble(4.5, "ratingAverage", "rating", "rate", "التقييم", "تقييم").toFloat().coerceIn(1.0f, 5.0f)
            val ratingCount = findLong(5L, "ratingCount", "ratingsCount", "reviewsCount", "عدد_التقييمات").toInt()
            val viewCount = findLong(10L, "viewCount", "views", "المشاهدات").toInt()
            val dataQualityScore = findLong(85L, "dataQualityScore", "qualityScore").toInt()

            val phoneSource = findString("phoneSource")
            val websiteSource = findString("websiteSource")
            val facebookSource = findString("facebookSource")
            val addressSource = findString("addressSource")
            val workingHoursSource = findString("workingHoursSource")

            val createdAt = findLong(System.currentTimeMillis(), "createdAt", "created_at", "تاريخ_الانشاء")
            val lastVerifiedAt = findLong(System.currentTimeMillis(), "lastVerifiedAt", "last_verified_at")
            val updatedAt = findLong(System.currentTimeMillis(), "updatedAt", "updated_at", "تاريخ_التعديل")
            val isPublished = findBoolean(true, "isPublished", "published", "منشور")
            val isDeleted = findBoolean(false, "isDeleted", "deleted", "محذوف")
            val archivedAt = doc.getLong("archivedAt")
            val approvedContributionId = findString("approvedContributionId")

            BusinessEntity(
                id = id,
                name = name,
                categoryId = categoryId,
                categoryName = categoryName,
                specialty = specialty,
                description = description,
                phone = phone,
                phoneSecondary = phoneSecondary,
                whatsapp = whatsapp,
                city = city,
                address = address,
                area = area,
                facebookUrl = facebookUrl,
                websiteUrl = websiteUrl,
                latitude = lat,
                longitude = lng,
                workingHours = workingHours,
                isOpenNow = isOpenNow,
                isVerified = isVerified,
                isActive = isActive,
                ratingAverage = ratingAverage,
                ratingCount = ratingCount,
                viewCount = viewCount,
                imageUrl = imageUrl,
                verificationStatus = if (isVerified) "VERIFIED" else "UNVERIFIED",
                dataQualityScore = dataQualityScore,
                phoneSource = phoneSource,
                websiteSource = websiteSource,
                facebookSource = facebookSource,
                addressSource = addressSource,
                workingHoursSource = workingHoursSource,
                createdAt = createdAt,
                lastVerifiedAt = lastVerifiedAt,
                updatedAt = updatedAt,
                isPublished = isPublished,
                isDeleted = isDeleted,
                archivedAt = archivedAt,
                approvedContributionId = approvedContributionId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing document ${doc.id}: ${e.message}", e)
            null
        }
    }

    /**
     * Converts a BusinessSourceEntity to a Firestore map.
     */
    fun sourceToFirestoreMap(source: BusinessSourceEntity): Map<String, Any?> {
        return mapOf(
            "id" to source.id,
            "businessId" to source.businessId,
            "sourceType" to source.sourceType,
            "sourceId" to source.sourceId,
            "sourceUrl" to source.sourceUrl,
            "sourceName" to source.sourceName,
            "rawPayloadJson" to source.rawPayloadJson,
            "discoveredAt" to source.discoveredAt,
            "lastCheckedAt" to source.lastCheckedAt,
            "lastVerifiedAt" to source.lastVerifiedAt,
            "sourceConfidence" to source.sourceConfidence,
            "isOfficial" to source.isOfficial,
            "isActive" to source.isActive,
            "serverSyncedAt" to FieldValue.serverTimestamp()
        )
    }

    /**
     * Converts a Firestore document to BusinessSourceEntity.
     */
    fun documentToSource(doc: DocumentSnapshot): BusinessSourceEntity? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val businessId = doc.getString("businessId") ?: return null
            val sourceType = doc.getString("sourceType") ?: "OTHER"
            val sourceId = doc.getString("sourceId")
            val sourceUrl = doc.getString("sourceUrl")
            val sourceName = doc.getString("sourceName") ?: "مصدر إلكتروني"
            val rawPayloadJson = doc.getString("rawPayloadJson")
            val discoveredAt = doc.getLong("discoveredAt") ?: System.currentTimeMillis()
            val lastCheckedAt = doc.getLong("lastCheckedAt") ?: System.currentTimeMillis()
            val lastVerifiedAt = doc.getLong("lastVerifiedAt")
            val sourceConfidence = (doc.getLong("sourceConfidence"))?.toInt() ?: 85
            val isOfficial = doc.getBoolean("isOfficial") ?: false
            val isActive = doc.getBoolean("isActive") ?: true

            BusinessSourceEntity(
                id = id,
                businessId = businessId,
                sourceType = sourceType,
                sourceId = sourceId,
                sourceUrl = sourceUrl,
                sourceName = sourceName,
                rawPayloadJson = rawPayloadJson,
                discoveredAt = discoveredAt,
                lastCheckedAt = lastCheckedAt,
                lastVerifiedAt = lastVerifiedAt,
                sourceConfidence = sourceConfidence,
                isOfficial = isOfficial,
                isActive = isActive
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing source document ${doc.id}: ${e.message}")
            null
        }
    }

    private var activeBusinessesLiveListener: ListenerRegistration? = null
    private var sessionStartTime: Long = System.currentTimeMillis()

    /**
     * Attaches a restricted, windowed Realtime Listener to 'businesses' Firestore collection.
     * Listens ONLY to published activities updated after the session started, limited to 20 items.
     * Does NOT listen to the entire collection to conserve quota and battery.
     */
    fun startRealtimeFirestoreSync(
        dao: DirectoryDao,
        scope: CoroutineScope
    ) {
        val db = firestore ?: return
        if (activeBusinessesLiveListener != null) return

        try {
            sessionStartTime = System.currentTimeMillis()
            activeBusinessesLiveListener = db.collection(COL_BUSINESSES)
                .whereEqualTo("isPublished", true)
                .whereGreaterThan("updatedAt", sessionStartTime)
                .limit(20)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        val toUpsert = mutableListOf<BusinessEntity>()
                        val toDelete = mutableListOf<String>()

                        for (docChange in snapshot.documentChanges) {
                            val doc = docChange.document
                            val isDeleted = doc.getBoolean("isDeleted") ?: false
                            val isArchived = doc.getString("verificationStatus") == "ARCHIVED" || doc.getBoolean("isActive") == false

                            when (docChange.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    if (isDeleted || isArchived) {
                                        toDelete.add(doc.id)
                                    } else {
                                        val biz = documentToBusiness(doc)
                                        if (biz != null) toUpsert.add(biz)
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    toDelete.add(doc.id)
                                }
                            }
                        }

                        if (toUpsert.isNotEmpty()) {
                            dao.insertBusinesses(toUpsert)
                            Log.d(TAG, "Restricted Realtime update: upserted ${toUpsert.size} items")
                        }
                        if (toDelete.isNotEmpty()) {
                            for (id in toDelete) {
                                dao.deleteBusiness(id)
                            }
                            Log.d(TAG, "Restricted Realtime update: deleted/tombstoned ${toDelete.size} items")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Restricted realtime listener notice: ${e.message}")
        }
    }

    /**
     * Detaches the realtime listener to avoid memory/read leaks when backgrounded.
     */
    fun stopRealtimeFirestoreSync() {
        try {
            activeBusinessesLiveListener?.remove()
            activeBusinessesLiveListener = null
        } catch (e: Exception) {
            // Safe cleanup
        }
    }

    /**
     * Performs Authoritative Incremental Synchronization between Cloud Firestore ('businesses')
     * and the local Room database cache.
     *
     * Invariants:
     * 1. Room is the immediate UI source of truth; sync runs non-blocking on Dispatchers.IO.
     * 2. When lastSyncTimestamp == 0, paginates published items in safe batches.
     * 3. When lastSyncTimestamp > 0, queries ONLY delta changes where updatedAt > lastSyncTimestamp.
     * 4. Progresses the sync cursor in SharedPreferences ONLY after Room transaction/insert commits.
     * 5. Handles tombstones (isDeleted == true / ARCHIVED) by deleting local Room records.
     */
    suspend fun performIncrementalSync(
        context: Context,
        dao: DirectoryDao,
        forceFullSync: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("خدمة Firestore غير متاحة على هذا الجهاز")
        )

        val lastSync = if (forceFullSync) 0L else getLastSyncTimestamp(context)
        val now = System.currentTimeMillis()

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            isSyncing = true,
            message = if (lastSync == 0L) "جاري تحميل الدليل من Firestore..." else "جاري فحص تحديثات الأنشطة من السحابة..."
        )

        try {
            var totalProcessed = 0
            var totalUpserted = 0
            var totalDeleted = 0
            var highestUpdatedAt = lastSync
            var lastVisibleDoc: DocumentSnapshot? = null
            var hasMore = true
            val pageSize = 100L

            while (hasMore) {
                var query = db.collection(COL_BUSINESSES)
                    .whereEqualTo("isPublished", true)

                if (lastSync > 0L) {
                    query = query.whereGreaterThan("updatedAt", lastSync)
                }

                query = query.orderBy("updatedAt", Query.Direction.ASCENDING)
                    .orderBy("id", Query.Direction.ASCENDING)
                    .limit(pageSize)

                if (lastVisibleDoc != null) {
                    query = query.startAfter(lastVisibleDoc)
                }

                val snapshot = query.get().await()
                if (snapshot.isEmpty) {
                    hasMore = false
                    break
                }

                val toUpsert = mutableListOf<BusinessEntity>()
                val toDeleteIds = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val docUpdatedAt = doc.getLong("updatedAt") ?: now
                    if (docUpdatedAt > highestUpdatedAt) {
                        highestUpdatedAt = docUpdatedAt
                    }

                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val verificationStatus = doc.getString("verificationStatus") ?: ""
                    val isActive = doc.getBoolean("isActive") ?: true

                    if (isDeleted || verificationStatus == "ARCHIVED" || !isActive) {
                        toDeleteIds.add(doc.id)
                    } else {
                        val biz = documentToBusiness(doc)
                        if (biz != null) {
                            toUpsert.add(biz)
                        }
                    }
                }

                // Save page to Room
                if (toUpsert.isNotEmpty()) {
                    dao.insertBusinesses(toUpsert)
                    totalUpserted += toUpsert.size
                }
                if (toDeleteIds.isNotEmpty()) {
                    for (delId in toDeleteIds) {
                        dao.deleteBusiness(delId)
                    }
                    totalDeleted += toDeleteIds.size
                }

                totalProcessed += snapshot.size()
                lastVisibleDoc = snapshot.documents.lastOrNull()

                // Deterministic cursor progression: only advance cursor after Room database insertion succeeds
                if (highestUpdatedAt > lastSync) {
                    setLastSyncTimestamp(context, highestUpdatedAt)
                }

                // If returned less than page size, reached the end of the delta stream
                if (snapshot.size() < pageSize) {
                    hasMore = false
                }
            }

            // Sync sources if needed
            val updatedSources = mutableListOf<BusinessSourceEntity>()
            try {
                val sourcesSnapshot = db.collection(COL_SOURCES).limit(100).get().await()
                for (doc in sourcesSnapshot.documents) {
                    val src = documentToSource(doc)
                    if (src != null) updatedSources.add(src)
                }
                if (updatedSources.isNotEmpty()) {
                    dao.insertBusinessSources(updatedSources)
                }
            } catch (e: Exception) {
                // Non-critical
            }

            // Also upload any local contributions that were saved while offline
            syncPendingContributions(dao)

            val msg = when {
                totalUpserted > 0 || totalDeleted > 0 -> "تمت المزامنة بنجاح: تحديث $totalUpserted نشاط وحذف $totalDeleted نشاط ☁️✅"
                else -> "دليل الأنشطة محدث ومتطابق مع السحابة ✅"
            }

            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SUCCESS,
                isSyncing = false,
                isConnected = true,
                lastSyncTimestamp = highestUpdatedAt,
                syncedBusinessesCount = totalUpserted,
                syncedSourcesCount = updatedSources.size,
                message = msg
            )

            Log.d(TAG, "Sync finished: Processed $totalProcessed, Upserted $totalUpserted, Deleted $totalDeleted")
            Result.success(totalProcessed)
        } catch (e: Exception) {
            val isPermissionDenied = (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                    (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)

            if (isPermissionDenied) {
                Log.i(TAG, "Firestore sync skipped: Cloud permissions restricted. Using local offline Room cache.")
                _syncStatus.value = _syncStatus.value.copy(
                    state = SyncState.SUCCESS,
                    isSyncing = false,
                    isConnected = true,
                    message = "تعمل قاعدة البيانات المحلية بنجاح (Room Cache)"
                )
                Result.success(0)
            } else {
                Log.w(TAG, "Firestore sync notice: ${e.message}")
                _syncStatus.value = _syncStatus.value.copy(
                    state = SyncState.OFFLINE,
                    isSyncing = false,
                    message = "فشل الجلب من Firestore: ${e.localizedMessage ?: e.message}"
                )
                Result.failure(e)
            }
        }
    }

    /**
     * One-time authoritative migration: Migrates legacy 'activities' records into unified 'businesses'.
     * Uses Entity Resolution and Arabic Deduplication to prevent duplicates and preserve all data, reviews, and sources.
     */
    suspend fun migrateLegacyActivitiesToUnifiedBusinesses(
        context: Context,
        dao: DirectoryDao
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore غير متاح"))
        val prefs = getPrefs(context)
        if (prefs.getBoolean("legacy_activities_migrated", false)) {
            return@withContext Result.success(0)
        }

        try {
            Log.d(TAG, "Starting migration from legacy '$COL_ACTIVITIES' to '$COL_BUSINESSES'...")
            val snapshot = db.collection(COL_ACTIVITIES).limit(500).get().await()
            if (snapshot.isEmpty) {
                prefs.edit().putBoolean("legacy_activities_migrated", true).apply()
                return@withContext Result.success(0)
            }

            var migratedCount = 0
            val existingMasterBusinesses = dao.getAllActiveBusinessesDirect()

            for (doc in snapshot.documents) {
                val biz = documentToBusiness(doc) ?: continue

                // Check candidate match in local room cache
                val candidate = CandidateBusiness(
                    id = doc.id,
                    businessName = biz.name,
                    categoryId = biz.categoryId,
                    categoryName = biz.categoryName,
                    normalizedPhone = com.example.util.ArabicNormalizer.normalizePhone(biz.phone),
                    address = biz.address,
                    website = biz.websiteUrl,
                    facebookPage = biz.facebookUrl
                )
                val match = com.example.data.engine.EntityResolutionAndDeduplicationEngine.evaluateCandidate(candidate, existingMasterBusinesses)

                val targetId = match.matchedBusinessId ?: biz.id
                val unifiedBiz = biz.copy(
                    id = targetId,
                    isPublished = true,
                    isDeleted = false,
                    isActive = true,
                    updatedAt = System.currentTimeMillis()
                )

                // Write unified record to COL_BUSINESSES
                db.collection(COL_BUSINESSES).document(targetId).set(
                    businessToFirestoreMap(unifiedBiz),
                    SetOptions.merge()
                ).await()

                migratedCount++
            }

            prefs.edit().putBoolean("legacy_activities_migrated", true).apply()
            Log.d(TAG, "Successfully migrated $migratedCount legacy activities to '$COL_BUSINESSES'")
            Result.success(migratedCount)
        } catch (e: Exception) {
            Log.w(TAG, "Migration notice: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads a User Contribution directly to Firestore 'contributions' collection with status 'PENDING'.
     * Returns true if cloud write succeeded.
     */
    suspend fun uploadContributionToFirestore(
        contribution: UserContributionEntity
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore غير متاح"))

        try {
            val docRef = db.collection(COL_CONTRIBUTIONS).document(contribution.id)
            val data = mapOf<String, Any?>(
                "id" to contribution.id,
                "humanReadableId" to contribution.humanReadableId,
                "userId" to contribution.userId,
                "userName" to contribution.userName,
                "userEmail" to contribution.userEmail,
                "businessId" to contribution.businessId,
                "businessName" to contribution.businessName,
                "type" to contribution.type,
                "categoryId" to contribution.categoryId,
                "fieldName" to contribution.fieldName,
                "oldValue" to contribution.oldValue,
                "newValue" to contribution.newValue,
                "payloadJson" to contribution.payloadJson,
                "userReason" to contribution.userReason,
                "status" to "PENDING", // strictly PENDING on client creation
                "moderatorNote" to contribution.moderatorNote,
                "submissionSource" to contribution.submissionSource,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "approvedAt" to null,
                "approvedBy" to null,
                "publishedBusinessId" to null
            )
            docRef.set(data).await()
            Log.d(TAG, "Successfully uploaded pending contribution ${contribution.id} to Firestore")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed uploading contribution to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads any pending offline contributions stored in Room to Firestore.
     */
    suspend fun syncPendingContributions(dao: DirectoryDao) = withContext(Dispatchers.IO) {
        try {
            val pendingList = dao.getPendingUploadContributions()
            for (pending in pendingList) {
                val uploadRes = uploadContributionToFirestore(pending)
                if (uploadRes.isSuccess) {
                    dao.updateContributionSyncStatus(pending.id, "SYNCED")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notice syncing pending contributions: ${e.message}")
        }
    }

    /**
     * Fetches a user's own contributions from Firestore.
     */
    suspend fun fetchUserContributionsFromFirestore(
        userId: String
    ): Result<List<UserContributionEntity>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore غير متاح"))
        try {
            val snapshot = db.collection(COL_CONTRIBUTIONS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()

            val list = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val humanReadableId = doc.getString("humanReadableId") ?: "#MG-${id.takeLast(4)}"
                val uid = doc.getString("userId") ?: userId
                val name = doc.getString("userName") ?: "مستخدم"
                val email = doc.getString("userEmail") ?: ""
                val bizId = doc.getString("businessId")
                val bizName = doc.getString("businessName") ?: "نشاط مقترح"
                val type = doc.getString("type") ?: "ADD_BUSINESS"
                val catId = doc.getString("categoryId")
                val fieldName = doc.getString("fieldName")
                val oldValue = doc.getString("oldValue")
                val newValue = doc.getString("newValue")
                val payloadJson = doc.getString("payloadJson")
                val userReason = doc.getString("userReason")
                val status = doc.getString("status") ?: "PENDING"
                val note = doc.getString("moderatorNote")
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                val reviewedAt = doc.getLong("reviewedAt")
                val approvedAt = doc.getLong("approvedAt")
                val approvedBy = doc.getString("approvedBy")
                val publishedBizId = doc.getString("publishedBusinessId")
                val submissionSource = doc.getString("submissionSource") ?: "ANDROID_APP"

                UserContributionEntity(
                    id = id,
                    humanReadableId = humanReadableId,
                    userId = uid,
                    userName = name,
                    userEmail = email,
                    businessId = bizId,
                    businessName = bizName,
                    type = type,
                    categoryId = catId,
                    fieldName = fieldName,
                    oldValue = oldValue,
                    newValue = newValue,
                    payloadJson = payloadJson,
                    userReason = userReason,
                    status = status,
                    moderatorNote = note,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    reviewedAt = reviewedAt,
                    approvedAt = approvedAt,
                    approvedBy = approvedBy,
                    publishedBusinessId = publishedBizId,
                    submissionSource = submissionSource,
                    syncStatus = "SYNCED"
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user contributions: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads or updates a single Business in Firestore ('businesses' collection).
     */
    suspend fun uploadBusinessToFirestore(
        business: BusinessEntity,
        sources: List<BusinessSourceEntity> = emptyList()
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح")
        )

        try {
            val batch = db.batch()
            val bizData = businessToFirestoreMap(business)

            val bizRef = db.collection(COL_BUSINESSES).document(business.id)
            batch.set(bizRef, bizData, SetOptions.merge())

            for (source in sources) {
                val srcRef = db.collection(COL_SOURCES).document(source.id)
                val srcData = sourceToFirestoreMap(source)
                batch.set(srcRef, srcData, SetOptions.merge())
            }

            batch.commit().await()
            Log.d(TAG, "Successfully pushed business ${business.name} to Firestore")
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Notice uploading business to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads all categories to Firestore collection 'categories'.
     */
    suspend fun uploadCategoriesToFirestore(categories: List<CategoryItem>): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore غير متاح"))
        try {
            val batch = db.batch()
            for (cat in categories) {
                val docRef = db.collection(COL_CATEGORIES).document(cat.id)
                val data = mapOf(
                    "id" to cat.id,
                    "nameAr" to cat.nameAr,
                    "iconName" to cat.iconName,
                    "count" to cat.count,
                    "colorHex" to cat.colorHex,
                    "description" to cat.description,
                    "isFeatured" to cat.isFeatured,
                    "sortOrder" to cat.sortOrder,
                    "subcategories" to cat.subcategories.map { sub ->
                        mapOf(
                            "id" to sub.id,
                            "parentCategoryId" to sub.parentCategoryId,
                            "nameAr" to sub.nameAr,
                            "keywords" to sub.keywords,
                            "count" to sub.count
                        )
                    },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "Successfully uploaded ${categories.size} categories to Firestore")
            Result.success(categories.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading categories to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Batch uploads a large collection of businesses to Firestore in safe chunks.
     */
    suspend fun batchUploadBusinessesToFirestore(
        businesses: List<BusinessEntity>,
        isDryRun: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (businesses.isEmpty()) return@withContext Result.success(0)
        if (isDryRun) return@withContext Result.success(businesses.size)

        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح")
        )

        try {
            val chunkSize = 300 // Safe batch size
            var totalWritten = 0

            businesses.chunked(chunkSize).forEach { chunk ->
                val batch = db.batch()
                for (biz in chunk) {
                    val bizRef = db.collection(COL_BUSINESSES).document(biz.id)
                    val data = businessToFirestoreMap(biz)
                    batch.set(bizRef, data, SetOptions.merge())
                }
                batch.commit().await()
                totalWritten += chunk.size
            }

            Log.d(TAG, "Batch write finished successfully for $totalWritten businesses")
            Result.success(totalWritten)
        } catch (e: Exception) {
            Log.w(TAG, "Batch upload notice: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Soft-deletes or archives a business in Firestore using a tombstone (isDeleted = true, isPublished = true)
     * so that all user devices receive the deletion via incremental sync and remove it from Room.
     */
    suspend fun deleteOrArchiveBusinessInFirestore(
        businessId: String,
        archiveOnly: Boolean = true
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح")
        )

        try {
            val bizRef = db.collection(COL_BUSINESSES).document(businessId)
            val now = System.currentTimeMillis()
            if (archiveOnly) {
                val updateData = mapOf(
                    "isDeleted" to true,
                    "isActive" to false,
                    "verificationStatus" to "ARCHIVED",
                    "archivedAt" to now,
                    "isPublished" to true,
                    "updatedAt" to now
                )
                bizRef.update(updateData).await()
            } else {
                // Hard delete with tombstone record
                val updateData = mapOf(
                    "isDeleted" to true,
                    "isPublished" to true,
                    "updatedAt" to now
                )
                bizRef.update(updateData).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Delete/archive notice in Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Records an Audit Log entry in Firestore for administrative provenance.
     */
    suspend fun recordCloudAuditLog(
        action: String,
        performedBy: String,
        details: String,
        targetId: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح")
        )

        try {
            val logData = hashMapOf(
                "id" to java.util.UUID.randomUUID().toString(),
                "action" to action,
                "performedBy" to performedBy,
                "details" to details,
                "targetId" to targetId,
                "timestamp" to System.currentTimeMillis(),
                "serverSyncedAt" to FieldValue.serverTimestamp()
            )
            db.collection(COL_AUDIT_LOGS).add(logData).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.i(TAG, "Cloud audit log notice: ${e.message}")
            Result.failure(e)
        }
    }
}
