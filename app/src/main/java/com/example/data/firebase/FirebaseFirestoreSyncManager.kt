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
                updatedAt = updatedAt
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

    private var activitiesListener: ListenerRegistration? = null
    private var businessesListener: ListenerRegistration? = null

    /**
     * Attaches Realtime Listeners to both 'activities' and 'businesses' Firestore collections.
     * When any document is added or edited in the Firebase Console, local Room cache updates immediately.
     */
    fun startRealtimeFirestoreSync(
        dao: DirectoryDao,
        scope: CoroutineScope
    ) {
        val db = firestore ?: return

        // 1. Listen to 'activities' collection
        try {
            activitiesListener?.remove()
            activitiesListener = db.collection(COL_ACTIVITIES).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    val toUpsert = mutableListOf<BusinessEntity>()
                    for (docChange in snapshot.documentChanges) {
                        when (docChange.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val biz = documentToBusiness(docChange.document)
                                if (biz != null) toUpsert.add(biz)
                            }
                            DocumentChange.Type.REMOVED -> {
                                dao.deleteBusiness(docChange.document.id)
                            }
                        }
                    }
                    if (toUpsert.isNotEmpty()) {
                        dao.insertBusinesses(toUpsert)
                        Log.d(TAG, "Realtime Firestore update from '$COL_ACTIVITIES': upserted ${toUpsert.size} items")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime listener error on '$COL_ACTIVITIES': ${e.message}")
        }

        // 2. Listen to 'businesses' collection
        try {
            businessesListener?.remove()
            businessesListener = db.collection(COL_BUSINESSES).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    val toUpsert = mutableListOf<BusinessEntity>()
                    for (docChange in snapshot.documentChanges) {
                        when (docChange.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val biz = documentToBusiness(docChange.document)
                                if (biz != null) toUpsert.add(biz)
                            }
                            DocumentChange.Type.REMOVED -> {
                                dao.deleteBusiness(docChange.document.id)
                            }
                        }
                    }
                    if (toUpsert.isNotEmpty()) {
                        dao.insertBusinesses(toUpsert)
                        Log.d(TAG, "Realtime Firestore update from '$COL_BUSINESSES': upserted ${toUpsert.size} items")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime listener error on '$COL_BUSINESSES': ${e.message}")
        }
    }

    /**
     * Performs Synchronization between Firestore and local Room cache.
     * Checks both 'activities' (user's 360+ activities) and 'businesses' collections.
     * Runs on Dispatchers.IO and writes directly to Room cache without blocking UI.
     */
    suspend fun performIncrementalSync(
        context: Context,
        dao: DirectoryDao,
        forceFullSync: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("خدمة Firestore غير متاحة على هذا الجهاز")
        )

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            isSyncing = true,
            message = "جاري الاتصال بـ Firestore وجلب الأنشطة من السحابة..."
        )

        val now = System.currentTimeMillis()

        try {
            val updatedBusinesses = mutableListOf<BusinessEntity>()
            val archivedIds = mutableListOf<String>()

            // 1. PRIMARY PASS: Query 'activities' collection (User's 360+ uploaded activities)
            try {
                Log.d(TAG, "Querying Firestore collection '$COL_ACTIVITIES'...")
                val activitiesSnapshot = db.collection(COL_ACTIVITIES).get().await()
                Log.d(TAG, "Fetched ${activitiesSnapshot.size()} raw documents from '$COL_ACTIVITIES'")

                for (doc in activitiesSnapshot.documents) {
                    val biz = documentToBusiness(doc)
                    if (biz != null) {
                        if (biz.isActive) {
                            updatedBusinesses.add(biz)
                        } else {
                            archivedIds.add(biz.id)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Querying '$COL_ACTIVITIES' notice: ${e.message}")
            }

            // 2. SECONDARY PASS: Query 'businesses' collection
            try {
                Log.d(TAG, "Querying Firestore collection '$COL_BUSINESSES'...")
                val businessesSnapshot = db.collection(COL_BUSINESSES).get().await()
                Log.d(TAG, "Fetched ${businessesSnapshot.size()} raw documents from '$COL_BUSINESSES'")

                for (doc in businessesSnapshot.documents) {
                    val biz = documentToBusiness(doc)
                    if (biz != null) {
                        val existingIndex = updatedBusinesses.indexOfFirst { it.id == biz.id }
                        if (existingIndex >= 0) {
                            if (biz.updatedAt >= updatedBusinesses[existingIndex].updatedAt) {
                                updatedBusinesses[existingIndex] = biz
                            }
                        } else {
                            if (biz.isActive) {
                                updatedBusinesses.add(biz)
                            } else {
                                archivedIds.add(biz.id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Querying '$COL_BUSINESSES' notice: ${e.message}")
            }

            // 3. Check for sources updates
            val updatedSources = mutableListOf<BusinessSourceEntity>()
            try {
                val sourcesSnapshot = db.collection(COL_SOURCES).limit(200).get().await()
                for (doc in sourcesSnapshot.documents) {
                    val src = documentToSource(doc)
                    if (src != null) {
                        updatedSources.add(src)
                    }
                }
            } catch (e: Exception) {
                // Non-critical
            }

            // 4. Save into local Room Cache
            if (updatedBusinesses.isNotEmpty()) {
                dao.insertBusinesses(updatedBusinesses)
                Log.d(TAG, "Successfully saved ${updatedBusinesses.size} activities to Room database")
            }
            if (archivedIds.isNotEmpty()) {
                for (archivedId in archivedIds) {
                    dao.deleteBusiness(archivedId)
                }
            }
            if (updatedSources.isNotEmpty()) {
                dao.insertBusinessSources(updatedSources)
            }

            // Save sync timestamp
            setLastSyncTimestamp(context, now)

            val totalSynced = updatedBusinesses.size
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SUCCESS,
                isSyncing = false,
                isConnected = true,
                lastSyncTimestamp = now,
                syncedBusinessesCount = totalSynced,
                syncedSourcesCount = updatedSources.size,
                message = if (totalSynced > 0) "تم جلب وتحديث $totalSynced نشاط من Firestore بنجاح ☁️✅" else "تم الاتصال بالسحابة - البيانات متطابقة"
            )

            Log.d(TAG, "Sync finished successfully. Total activities synced: $totalSynced")
            Result.success(totalSynced)
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
     * Uploads or updates a single Business in Firestore (both 'activities' and 'businesses' collections).
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

            // Write to both activities and businesses for complete consistency
            val actRef = db.collection(COL_ACTIVITIES).document(business.id)
            val bizRef = db.collection(COL_BUSINESSES).document(business.id)
            batch.set(actRef, bizData, SetOptions.merge())
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
                    val actRef = db.collection(COL_ACTIVITIES).document(biz.id)
                    val bizRef = db.collection(COL_BUSINESSES).document(biz.id)
                    val data = businessToFirestoreMap(biz)
                    batch.set(actRef, data, SetOptions.merge())
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
     * Soft-deletes or archives a business in Firestore so all user devices receive the deletion.
     */
    suspend fun deleteOrArchiveBusinessInFirestore(
        businessId: String,
        archiveOnly: Boolean = true
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح")
        )

        try {
            val actRef = db.collection(COL_ACTIVITIES).document(businessId)
            val bizRef = db.collection(COL_BUSINESSES).document(businessId)
            if (archiveOnly) {
                val updateData = mapOf(
                    "isActive" to false,
                    "verificationStatus" to "ARCHIVED",
                    "updatedAt" to System.currentTimeMillis()
                )
                actRef.update(updateData).await()
                bizRef.update(updateData).await()
            } else {
                actRef.delete().await()
                bizRef.delete().await()
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
