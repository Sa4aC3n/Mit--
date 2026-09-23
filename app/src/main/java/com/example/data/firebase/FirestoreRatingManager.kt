package com.example.data.firebase

import android.util.Log
import com.example.data.model.ReviewEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * FirestoreRatingManager handles saving, updating, and syncing star ratings and reviews
 * to Firebase Firestore (Source of Truth).
 */
object FirestoreRatingManager {

    private const val TAG = "FirestoreRatingManager"
    private const val COL_REVIEWS = "reviews"
    private const val COL_BUSINESSES = "businesses"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore instance not available: ${e.message}")
            null
        }
    }

    /**
     * Saves or updates a review in Firestore and updates the aggregated rating on the business.
     */
    suspend fun saveRatingAndReview(
        businessId: String,
        businessName: String,
        review: ReviewEntity,
        newAverageRating: Float,
        newRatingCount: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح لحفظ التقييم")
        )

        return@withContext try {
            val reviewMap = hashMapOf<String, Any?>(
                "id" to review.id,
                "businessId" to businessId,
                "businessName" to businessName,
                "userId" to review.userId,
                "userName" to review.userName,
                "userAvatarUrl" to (review.userAvatarUrl ?: ""),
                "userProvider" to review.userProvider,
                "rating" to review.rating.toDouble(),
                "comment" to review.comment,
                "status" to review.status,
                "timestamp" to review.timestamp,
                "updatedAt" to review.updatedAt,
                "serverSyncedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()

            // 1. Write review document under reviews/{reviewId}
            val reviewRef = db.collection(COL_REVIEWS).document(review.id)
            batch.set(reviewRef, reviewMap, SetOptions.merge())

            // 2. Update aggregate rating on businesses/{businessId}
            val businessRef = db.collection(COL_BUSINESSES).document(businessId)
            batch.update(
                businessRef,
                mapOf(
                    "ratingAverage" to newAverageRating.toDouble(),
                    "ratingCount" to newRatingCount,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            batch.commit().await()
            Log.d(TAG, "Successfully synced rating to Firestore for $businessName (${review.rating} stars)")
            Result.success(true)
        } catch (e: Exception) {
            val isPermission = (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                    (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)
            if (isPermission) {
                Log.i(TAG, "Firestore rating sync restricted. Rating is saved locally in Room.")
            } else {
                Log.w(TAG, "Notice saving rating to Firestore: ${e.message}")
            }
            Result.failure(e)
        }
    }

    /**
     * Deletes a review document in Firestore and atomically recalculates the aggregated rating on businesses/{businessId}.
     */
    suspend fun deleteRatingAndReview(
        businessId: String,
        reviewId: String,
        newAverageRating: Float,
        newRatingCount: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح لحذف التقييم")
        )

        return@withContext try {
            val batch = db.batch()
            val reviewRef = db.collection(COL_REVIEWS).document(reviewId)
            batch.delete(reviewRef)

            val businessRef = db.collection(COL_BUSINESSES).document(businessId)
            batch.update(
                businessRef,
                mapOf(
                    "ratingAverage" to newAverageRating.toDouble(),
                    "ratingCount" to newRatingCount,
                    "updatedAt" to System.currentTimeMillis()
                )
            )

            batch.commit().await()
            Log.d(TAG, "Successfully deleted review $reviewId and updated business $businessId in Firestore")
            Result.success(true)
        } catch (e: Exception) {
            val isPermission = (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                    (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)
            if (isPermission) {
                Log.i(TAG, "Firestore review deletion restricted. Removed locally in Room.")
            } else {
                Log.w(TAG, "Notice deleting review in Firestore: ${e.message}")
            }
            Result.failure(e)
        }
    }

    /**
     * Loads paginated reviews for a business directly from Firestore.
     */
    suspend fun fetchPaginatedReviews(
        businessId: String,
        limit: Long = 10,
        lastTimestamp: Long? = null
    ): Result<List<ReviewEntity>> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore غير متاح")
        )

        try {
            var query = db.collection(COL_REVIEWS)
                .whereEqualTo("businessId", businessId)
                .whereEqualTo("status", "APPROVED")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastTimestamp != null) {
                query = query.startAfter(lastTimestamp)
            }

            val snapshot = query.get().await()
            val reviews = mutableListOf<ReviewEntity>()

            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                val bizId = doc.getString("businessId") ?: businessId
                val userId = doc.getString("userId") ?: "user"
                val userName = doc.getString("userName") ?: "مستخدم"
                val userAvatarUrl = doc.getString("userAvatarUrl")
                val userProvider = doc.getString("userProvider") ?: "GOOGLE"
                val rating = (doc.getDouble("rating"))?.toFloat() ?: 5.0f
                val comment = doc.getString("comment") ?: ""
                val status = doc.getString("status") ?: "APPROVED"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: timestamp
                val helpfulCount = (doc.getLong("helpfulCount"))?.toInt() ?: 0
                val ownerReply = doc.getString("ownerReply")
                val ownerReplyAt = doc.getLong("ownerReplyAt")

                reviews.add(
                    ReviewEntity(
                        id = id,
                        businessId = bizId,
                        userId = userId,
                        userName = userName,
                        userAvatarUrl = userAvatarUrl,
                        userProvider = userProvider,
                        rating = rating,
                        comment = comment,
                        status = status,
                        timestamp = timestamp,
                        updatedAt = updatedAt,
                        helpfulCount = helpfulCount,
                        ownerReply = ownerReply,
                        ownerReplyAt = ownerReplyAt
                    )
                )
            }

            Result.success(reviews)
        } catch (e: Exception) {
            val isPermission = (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                    (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)
            if (isPermission) {
                Log.i(TAG, "Firestore reviews access restricted. Local Room reviews active.")
            } else {
                Log.w(TAG, "Notice loading reviews from Firestore: ${e.message}")
            }
            Result.failure(e)
        }
    }
}
