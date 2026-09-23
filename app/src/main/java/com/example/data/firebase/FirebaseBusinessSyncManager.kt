package com.example.data.firebase

import android.util.Log
import com.example.data.model.BusinessEntity
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val isConnected: Boolean = true,
    val isListening: Boolean = true,
    val lastSyncTimestamp: Long = 0L,
    val cloudCount: Int = 0,
    val pendingUploads: Int = 0,
    val lastSyncMessage: String = "جاهز للمزامنة السحابية"
)

/**
 * FirebaseBusinessSyncManager handles full two-way cloud persistence and synchronization
 * for all businesses and enriched directory data across all client devices.
 */
object FirebaseBusinessSyncManager {

    private const val TAG = "FirebaseBusinessSync"
    private const val BUSINESSES_NODE = "businesses"
    private const val META_SYNC_NODE = "meta_sync/businesses"

    private val _syncStatus = MutableStateFlow(CloudSyncStatus())
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private var activeChildListener: ChildEventListener? = null
    private var isListenerAttached = false

    private val database: FirebaseDatabase? by lazy {
        try {
            val db = FirebaseDatabase.getInstance()
            // Enable persistence if possible
            try {
                db.setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Ignore if already initialized
            }
            db
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseDatabase instance could not be created: ${e.message}")
            null
        }
    }

    fun isFirebaseAvailable(): Boolean = database != null

    /**
     * Converts a BusinessEntity to a Firebase-compatible Map representation.
     */
    fun businessToMap(biz: BusinessEntity): HashMap<String, Any?> {
        return hashMapOf(
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
            "serverSyncedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Safely constructs a BusinessEntity from a Firebase DataSnapshot.
     */
    fun snapshotToBusiness(snapshot: DataSnapshot): BusinessEntity? {
        return try {
            val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return null
            val name = snapshot.child("name").getValue(String::class.java) ?: return null
            val categoryId = snapshot.child("categoryId").getValue(String::class.java) ?: "cat_general"
            val categoryName = snapshot.child("categoryName").getValue(String::class.java) ?: "خدمات عامة"
            val specialty = snapshot.child("specialty").getValue(String::class.java) ?: ""
            val description = snapshot.child("description").getValue(String::class.java) ?: ""
            val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
            val phoneSecondary = snapshot.child("phoneSecondary").getValue(String::class.java)
            val whatsapp = snapshot.child("whatsapp").getValue(String::class.java)
            val city = snapshot.child("city").getValue(String::class.java) ?: "مدينة ميت غمر"
            val address = snapshot.child("address").getValue(String::class.java) ?: "ميت غمر"
            val area = snapshot.child("area").getValue(String::class.java) ?: "وسط البلد"
            val facebookUrl = snapshot.child("facebookUrl").getValue(String::class.java)
            val websiteUrl = snapshot.child("websiteUrl").getValue(String::class.java)
            
            val latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 30.7183
            val longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 31.2568
            val workingHours = snapshot.child("workingHours").getValue(String::class.java) ?: "09:00 ص - 10:00 م"
            val isOpenNow = snapshot.child("isOpenNow").getValue(Boolean::class.java) ?: true
            val isVerified = snapshot.child("isVerified").getValue(Boolean::class.java) ?: false
            val isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: true
            
            val ratingAvgRaw = snapshot.child("ratingAverage").value
            val ratingAverage = when (ratingAvgRaw) {
                is Double -> ratingAvgRaw.toFloat()
                is Long -> ratingAvgRaw.toFloat()
                is Float -> ratingAvgRaw
                else -> 0.0f
            }
            
            val ratingCount = (snapshot.child("ratingCount").value as? Long)?.toInt() ?: 0
            val viewCount = (snapshot.child("viewCount").value as? Long)?.toInt() ?: 0
            val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)
            val verificationStatus = snapshot.child("verificationStatus").getValue(String::class.java) ?: if (isVerified) "VERIFIED" else "UNVERIFIED"
            val dataQualityScore = (snapshot.child("dataQualityScore").value as? Long)?.toInt() ?: 85
            val phoneSource = snapshot.child("phoneSource").getValue(String::class.java)
            val websiteSource = snapshot.child("websiteSource").getValue(String::class.java)
            val facebookSource = snapshot.child("facebookSource").getValue(String::class.java)
            val addressSource = snapshot.child("addressSource").getValue(String::class.java)
            val workingHoursSource = snapshot.child("workingHoursSource").getValue(String::class.java)
            
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val lastVerifiedAt = snapshot.child("lastVerifiedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

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
                latitude = latitude,
                longitude = longitude,
                workingHours = workingHours,
                isOpenNow = isOpenNow,
                isVerified = isVerified,
                isActive = isActive,
                ratingAverage = ratingAverage,
                ratingCount = ratingCount,
                viewCount = viewCount,
                imageUrl = imageUrl,
                verificationStatus = verificationStatus,
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
            Log.e(TAG, "Error parsing BusinessEntity snapshot: ${e.message}", e)
            null
        }
    }

    /**
     * Pushes a single business to Firebase Cloud Database.
     */
    suspend fun pushBusinessToCloud(business: BusinessEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext Result.failure(IllegalStateException("Firebase Database unavailable"))
        try {
            _syncStatus.value = _syncStatus.value.copy(isSyncing = true)
            val map = businessToMap(business)
            
            // Write to /businesses/{id}
            db.getReference(BUSINESSES_NODE)
                .child(business.id)
                .setValue(map)
                .await()

            // Update meta
            db.getReference(META_SYNC_NODE).updateChildren(
                mapOf(
                    "lastSyncTimestamp" to System.currentTimeMillis(),
                    "lastUpdatedBusinessId" to business.id,
                    "lastUpdatedBusinessName" to business.name
                )
            ).await()

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "تم تثبيت نشاط (${business.name}) على السيرفر السحابي بنجاح ☁️"
            )
            Log.d(TAG, "Successfully synced business ${business.name} (${business.id}) to Cloud.")
            Result.success(true)
        } catch (e: Exception) {
            val isPermission = e.message?.contains("Permission denied", ignoreCase = true) == true
            if (isPermission) {
                Log.i(TAG, "Firebase push permission restricted: ${e.message}")
            } else {
                Log.w(TAG, "Firebase push warning: ${e.message}")
            }
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isConnected = !isPermission,
                lastSyncMessage = if (isPermission) "تم الحفظ محلياً (السيرفر السحابي مقيد)" else "فشل التثبيت السحابي: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    /**
     * Pushes a batch of businesses (e.g. from Smart Data Engine discovery or Bulk Excel Import) to Cloud.
     */
    suspend fun pushBusinessesBatchToCloud(businesses: List<BusinessEntity>): Result<Int> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext Result.failure(IllegalStateException("Firebase Database unavailable"))
        if (businesses.isEmpty()) return@withContext Result.success(0)

        try {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                pendingUploads = businesses.size,
                lastSyncMessage = "جاري تثبيت ${businesses.size} نشاط على السيرفر السحابي..."
            )

            val rootRef = db.reference
            val updates = hashMapOf<String, Any?>()

            businesses.forEach { biz ->
                val path = "$BUSINESSES_NODE/${biz.id}"
                updates[path] = businessToMap(biz)
            }

            updates["$META_SYNC_NODE/lastSyncTimestamp"] = System.currentTimeMillis()
            updates["$META_SYNC_NODE/lastBatchCount"] = businesses.size

            rootRef.updateChildren(updates).await()

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                pendingUploads = 0,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "تم تثبيت ${businesses.size} نشاط على السيرفر السحابي ومتاح لجميع المستخدمين فوراً! 🚀"
            )

            Log.d(TAG, "Batch synced ${businesses.size} businesses to Firebase Cloud.")
            Result.success(businesses.size)
        } catch (e: Exception) {
            val isPermission = e.message?.contains("Permission denied", ignoreCase = true) == true
            if (isPermission) {
                Log.i(TAG, "Firebase batch push permission restricted: ${e.message}")
            } else {
                Log.w(TAG, "Firebase batch push warning: ${e.message}")
            }
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isConnected = !isPermission,
                lastSyncMessage = if (isPermission) "تم الحفظ محلياً في قاعدة البيانات (السيرفر السحابي مقيد)" else "خطأ أثناء التثبيت السحابي الجماعي: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    /**
     * Deletes a business from Firebase Cloud.
     */
    suspend fun deleteBusinessFromCloud(businessId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext Result.failure(IllegalStateException("Firebase Database unavailable"))
        try {
            db.getReference(BUSINESSES_NODE)
                .child(businessId)
                .removeValue()
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.d(TAG, "Notice deleting business from cloud: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Pulls all businesses from Firebase Cloud to synchronize down to the local device cache.
     */
    suspend fun fetchAllBusinessesFromCloud(): Result<List<BusinessEntity>> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext Result.failure(IllegalStateException("Firebase Database unavailable"))
        try {
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = true,
                lastSyncMessage = "جاري جلب كافة الأنشطة من السيرفر السحابي..."
            )

            val snapshot = db.getReference(BUSINESSES_NODE).get().await()
            val businessesList = mutableListOf<BusinessEntity>()

            for (child in snapshot.children) {
                snapshotToBusiness(child)?.let {
                    businessesList.add(it)
                }
            }

            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                cloudCount = businessesList.size,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastSyncMessage = "تم استلام ${businessesList.size} نشاط من السيرفر السحابي بنجاح ☁️"
            )

            Log.d(TAG, "Fetched ${businessesList.size} businesses from Firebase Cloud.")
            Result.success(businessesList)
        } catch (e: Exception) {
            val isPermission = e.message?.contains("Permission denied", ignoreCase = true) == true
            if (isPermission) {
                Log.i(TAG, "Firebase Cloud access restricted: ${e.message}. Operating on local Room cache.")
            } else {
                Log.w(TAG, "Notice fetching businesses from cloud: ${e.message}")
            }
            _syncStatus.value = _syncStatus.value.copy(
                isSyncing = false,
                isConnected = !isPermission,
                lastSyncMessage = if (isPermission) "تعمل قاعدة البيانات المحلية بالكامل (السيرفر السحابي مقيد)" else "تعذر الاتصال بالسيرفر: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    /**
     * Attaches a real-time ChildEventListener to the businesses node.
     * When any device or admin adds/edits an activity on the server,
     * this callback is triggered immediately on all other devices.
     */
    fun startRealtimeCloudSync(
        onBusinessUpserted: (BusinessEntity) -> Unit,
        onBusinessDeleted: (String) -> Unit
    ) {
        val db = database ?: return
        if (isListenerAttached) return

        try {
            val businessesRef = db.getReference(BUSINESSES_NODE)
            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshotToBusiness(snapshot)?.let { biz ->
                        Log.d(TAG, "Realtime Cloud Sync: Remote Business Added -> ${biz.name}")
                        onBusinessUpserted(biz)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshotToBusiness(snapshot)?.let { biz ->
                        Log.d(TAG, "Realtime Cloud Sync: Remote Business Changed -> ${biz.name}")
                        onBusinessUpserted(biz)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    Log.d(TAG, "Realtime Cloud Sync: Remote Business Removed -> $id")
                    onBusinessDeleted(id)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    Log.i(TAG, "Realtime Cloud Sync status: ${error.message}")
                    _syncStatus.value = _syncStatus.value.copy(
                        isConnected = false,
                        isListening = false,
                        lastSyncMessage = "المزامنة السحابية غير مفعلة (${error.message})"
                    )
                }
            }

            businessesRef.addChildEventListener(listener)
            activeChildListener = listener
            isListenerAttached = true
            Log.d(TAG, "Realtime Cloud Listener attached to /businesses node.")
        } catch (e: Exception) {
            Log.i(TAG, "Realtime Cloud Sync notice: ${e.message}")
        }
    }

    fun stopRealtimeCloudSync() {
        val db = database ?: return
        activeChildListener?.let {
            try {
                db.getReference(BUSINESSES_NODE).removeEventListener(it)
                isListenerAttached = false
                activeChildListener = null
            } catch (e: Exception) {
                Log.w(TAG, "Error removing listener: ${e.message}")
            }
        }
    }

    private const val KEY_RTDB_MIGRATED = "rtdb_businesses_migrated_to_firestore_v1"

    /**
     * One-time safe idempotent migration: Reads all legacy business records from Realtime Database,
     * uploads them to Cloud Firestore (Single Source of Truth), and updates Room local cache.
     * Prevents duplication and marks migration complete in SharedPreferences.
     */
    suspend fun migrateRealtimeDatabaseToFirestore(
        context: android.content.Context,
        dao: com.example.data.local.DirectoryDao
    ): Result<Int> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("met_ghamr_sync_migration_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_RTDB_MIGRATED, false)) {
            return@withContext Result.success(0)
        }

        val db = database ?: return@withContext Result.failure(IllegalStateException("Realtime Database غير متاح"))

        try {
            val snapshot = db.getReference(BUSINESSES_NODE).get().await()
            if (!snapshot.exists() || !snapshot.hasChildren()) {
                prefs.edit().putBoolean(KEY_RTDB_MIGRATED, true).apply()
                return@withContext Result.success(0)
            }

            val rtdbList = mutableListOf<BusinessEntity>()
            for (child in snapshot.children) {
                snapshotToBusiness(child)?.let { rtdbList.add(it) }
            }

            if (rtdbList.isEmpty()) {
                prefs.edit().putBoolean(KEY_RTDB_MIGRATED, true).apply()
                return@withContext Result.success(0)
            }

            Log.d(TAG, "Migrating ${rtdbList.size} businesses from Realtime Database to Cloud Firestore...")
            
            // Upload to Cloud Firestore as authoritative source
            FirebaseFirestoreSyncManager.batchUploadBusinessesToFirestore(rtdbList)
            
            // Insert into Room local database cache
            dao.insertBusinesses(rtdbList)

            prefs.edit().putBoolean(KEY_RTDB_MIGRATED, true).apply()
            Log.d(TAG, "Realtime Database migration to Cloud Firestore completed successfully (${rtdbList.size} items).")
            Result.success(rtdbList.size)
        } catch (e: Exception) {
            Log.w(TAG, "Notice during RTDB migration: ${e.message}")
            Result.failure(e)
        }
    }
}
