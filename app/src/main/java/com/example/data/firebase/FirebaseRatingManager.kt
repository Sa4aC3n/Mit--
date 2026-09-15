package com.example.data.firebase

import android.util.Log
import com.example.data.model.ReviewEntity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * FirebaseRatingManager handles saving, updating, and syncing star ratings and reviews
 * to Firebase Realtime Database.
 */
object FirebaseRatingManager {

    private const val TAG = "FirebaseRatingManager"
    private const val RATINGS_NODE = "ratings"
    private const val PLACES_RATINGS_NODE = "places_ratings"

    private val database: FirebaseDatabase? by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseDatabase instance not available: ${e.message}")
            null
        }
    }

    /**
     * Saves or updates a place star rating and review in Firebase Realtime Database.
     */
    suspend fun saveRating(
        businessId: String,
        businessName: String,
        review: ReviewEntity,
        newAverageRating: Float,
        newRatingCount: Int
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext Result.failure(
            IllegalStateException("Firebase Realtime Database غير مهيأ")
        )

        return@withContext try {
            val ratingMap = hashMapOf<String, Any>(
                "reviewId" to review.id,
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
                "syncedAt" to System.currentTimeMillis()
            )

            // 1. Save specific review under ratings/{businessId}/{reviewId}
            db.getReference(RATINGS_NODE)
                .child(businessId)
                .child(review.id)
                .setValue(ratingMap)
                .await()

            // 2. Save aggregated ratings under places_ratings/{businessId}
            val summaryMap = hashMapOf<String, Any>(
                "businessId" to businessId,
                "businessName" to businessName,
                "ratingAverage" to newAverageRating.toDouble(),
                "ratingCount" to newRatingCount,
                "lastRatedAt" to System.currentTimeMillis()
            )

            db.getReference(PLACES_RATINGS_NODE)
                .child(businessId)
                .setValue(summaryMap)
                .await()

            Log.d(TAG, "Successfully synced rating to Firebase for $businessName (${review.rating} stars)")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save rating to Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a review rating from Firebase Realtime Database.
     */
    suspend fun deleteRating(
        businessId: String,
        reviewId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val db = database ?: return@withContext Result.failure(
            IllegalStateException("Firebase Database unavailable")
        )

        return@withContext try {
            db.getReference(RATINGS_NODE)
                .child(businessId)
                .child(reviewId)
                .removeValue()
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete rating from Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if Firebase Realtime Database is accessible.
     */
    fun isFirebaseConnected(): Boolean {
        return database != null
    }
}
