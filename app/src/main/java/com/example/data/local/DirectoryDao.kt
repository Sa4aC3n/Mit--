package com.example.data.local

import androidx.room.*
import com.example.data.model.AuditLogEntity
import com.example.data.model.BusinessEntity
import com.example.data.model.BusinessSourceEntity
import com.example.data.model.ContributionDraftEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.FieldAuditHistoryEntity
import com.example.data.model.MergeHistoryEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.RawDiscoveredRecordEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.ReviewHelpfulEntity
import com.example.data.model.ReviewReportEntity
import com.example.data.model.UserAccountEntity
import com.example.data.model.UserContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {

    // --- Business Queries ---
    @Query("SELECT * FROM businesses WHERE isActive = 1 ORDER BY isVerified DESC, ratingAverage DESC, viewCount DESC")
    fun getAllActiveBusinesses(): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE isActive = 1 ORDER BY isVerified DESC, ratingAverage DESC, viewCount DESC")
    suspend fun getAllActiveBusinessesDirect(): List<BusinessEntity>

    @Query("SELECT * FROM businesses WHERE id = :id")
    fun getBusinessById(id: String): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :id")
    suspend fun getBusinessByIdDirect(id: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE categoryId = :categoryId AND isActive = 1 ORDER BY isVerified DESC, ratingAverage DESC")
    fun getBusinessesByCategory(categoryId: String): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE (name LIKE '%' || :query || '%' OR specialty LIKE '%' || :query || '%' OR area LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') AND isActive = 1 ORDER BY isVerified DESC, ratingAverage DESC")
    fun searchBusinesses(query: String): Flow<List<BusinessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinesses(businesses: List<BusinessEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("UPDATE businesses SET isActive = :isActive WHERE id = :id")
    suspend fun setBusinessActiveStatus(id: String, isActive: Boolean)

    @Query("UPDATE businesses SET isVerified = :isVerified WHERE id = :id")
    suspend fun setBusinessVerifiedStatus(id: String, isVerified: Boolean)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: String)

    @Query("UPDATE businesses SET viewCount = viewCount + 1 WHERE id = :id")
    suspend fun incrementViewCount(id: String)

    // --- Review Queries ---
    @Query("SELECT * FROM reviews WHERE businessId = :businessId AND status = 'APPROVED' ORDER BY timestamp DESC")
    fun getReviewsForBusiness(businessId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE businessId = :businessId AND userId = :userId AND status != 'DELETED' LIMIT 1")
    suspend fun getUserReviewForBusiness(businessId: String, userId: String): ReviewEntity?

    @Query("SELECT * FROM reviews WHERE userId = :userId AND status != 'DELETED' ORDER BY timestamp DESC")
    fun getReviewsByUser(userId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE id = :reviewId LIMIT 1")
    suspend fun getReviewByIdDirect(reviewId: String): ReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Query("UPDATE reviews SET status = :status WHERE id = :reviewId")
    suspend fun updateReviewStatus(reviewId: String, status: String)

    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteReview(reviewId: String)

    @Query("SELECT AVG(rating) FROM reviews WHERE businessId = :businessId AND status = 'APPROVED'")
    suspend fun getAverageRating(businessId: String): Float?

    @Query("SELECT COUNT(*) FROM reviews WHERE businessId = :businessId AND status = 'APPROVED'")
    suspend fun getReviewCount(businessId: String): Int

    @Query("SELECT COUNT(*) FROM reviews WHERE businessId = :businessId AND status = 'APPROVED' AND CAST(rating AS INT) = :star")
    suspend fun getStarCount(businessId: String, star: Int): Int

    // --- Review Helpful Votes ---
    @Query("SELECT EXISTS(SELECT 1 FROM review_helpful_votes WHERE reviewId = :reviewId AND userId = :userId)")
    fun hasUserVotedHelpful(reviewId: String, userId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM review_helpful_votes WHERE reviewId = :reviewId AND userId = :userId)")
    suspend fun hasUserVotedHelpfulDirect(reviewId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpfulVote(vote: ReviewHelpfulEntity)

    @Query("DELETE FROM review_helpful_votes WHERE reviewId = :reviewId AND userId = :userId")
    suspend fun deleteHelpfulVote(reviewId: String, userId: String)

    @Query("SELECT COUNT(*) FROM review_helpful_votes WHERE reviewId = :reviewId")
    suspend fun getHelpfulVoteCount(reviewId: String): Int

    @Query("UPDATE reviews SET helpfulCount = :count WHERE id = :reviewId")
    suspend fun updateReviewHelpfulCount(reviewId: String, count: Int)

    // --- Review Reports ---
    @Query("SELECT EXISTS(SELECT 1 FROM review_reports WHERE reviewId = :reviewId AND userId = :userId)")
    suspend fun hasUserReportedReview(reviewId: String, userId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewReport(report: ReviewReportEntity)

    // --- Favorite Queries ---
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY timestamp DESC")
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND businessId = :businessId)")
    fun isFavoriteForUser(userId: String, businessId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND businessId = :businessId")
    suspend fun removeFavoriteForUser(userId: String, businessId: String)

    @Query("UPDATE OR IGNORE favorites SET userId = :targetUserId WHERE userId = 'guest'")
    suspend fun migrateGuestFavoritesToUser(targetUserId: String)

    @Query("DELETE FROM favorites WHERE userId = 'guest'")
    suspend fun clearGuestFavorites()

    // --- Notification Queries ---
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    // --- Statistics & Admin Counts ---
    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getTotalBusinessesCount(): Int

    @Query("SELECT COUNT(*) FROM businesses WHERE isActive = 1")
    suspend fun getActiveBusinessesCount(): Int

    @Query("SELECT COUNT(*) FROM businesses WHERE isVerified = 1")
    suspend fun getVerifiedBusinessesCount(): Int

    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun getTotalReviewsCount(): Int

    @Query("SELECT COUNT(*) FROM reviews WHERE status = 'PENDING'")
    suspend fun getPendingReviewsCount(): Int

    @Query("SELECT SUM(viewCount) FROM businesses")
    suspend fun getTotalViewsCount(): Int?

    // --- User Contributions Queries ---
    @Query("SELECT * FROM user_contributions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserContributions(userId: String): Flow<List<UserContributionEntity>>

    @Query("SELECT * FROM user_contributions WHERE id = :id")
    suspend fun getContributionById(id: String): UserContributionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: UserContributionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<UserContributionEntity>)

    @Query("SELECT * FROM user_contributions WHERE syncStatus = 'PENDING_UPLOAD' OR syncStatus = 'WAITING_FOR_UPLOAD'")
    suspend fun getPendingUploadContributions(): List<UserContributionEntity>

    @Query("UPDATE user_contributions SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateContributionSyncStatus(id: String, syncStatus: String)

    @Query("UPDATE user_contributions SET status = :status, moderatorNote = :note, approvedAt = :approvedAt, approvedBy = :approvedBy, publishedBusinessId = :publishedBusinessId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateContributionModerationDetails(
        id: String,
        status: String,
        note: String?,
        approvedAt: Long?,
        approvedBy: String?,
        publishedBusinessId: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE user_contributions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateContributionStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_contributions WHERE id = :id AND userId = :userId AND status = 'PENDING'")
    suspend fun deletePendingContribution(id: String, userId: String)

    // --- Draft Queries ---
    @Query("SELECT * FROM contribution_drafts WHERE userId = :userId AND type = :type AND (businessId = :businessId OR (businessId IS NULL AND :businessId IS NULL)) LIMIT 1")
    suspend fun getDraft(userId: String, type: String, businessId: String?): ContributionDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: ContributionDraftEntity)

    @Query("DELETE FROM contribution_drafts WHERE userId = :userId AND type = :type AND (businessId = :businessId OR (businessId IS NULL AND :businessId IS NULL))")
    suspend fun deleteDraft(userId: String, type: String, businessId: String?)

    // --- Duplicate Business Detection Queries ---
    @Query("SELECT * FROM businesses WHERE (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR phone LIKE '%' || :phone || '%') AND isActive = 1 LIMIT 5")
    suspend fun searchPossibleDuplicates(query: String, phone: String): List<BusinessEntity>

    // --- PHASE 10: ADMIN & MODERATION QUERIES ---

    // Admin KPIs Aggregation
    @Query("SELECT COUNT(*) FROM businesses")
    suspend fun getBusinessesCount(): Int

    @Query("SELECT COUNT(*) FROM user_contributions WHERE type = 'ADD_BUSINESS' AND status = 'PENDING'")
    suspend fun getPendingBusinessesCount(): Int

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun getUsersCount(): Int

    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun getReviewsCount(): Int

    @Query("SELECT COUNT(*) FROM user_contributions WHERE status = 'PENDING'")
    suspend fun getPendingContributionsCount(): Int

    @Query("SELECT COUNT(*) FROM review_reports WHERE status = 'PENDING'")
    suspend fun getOpenReportsCount(): Int

    // Admin Business Management
    @Query("SELECT * FROM businesses ORDER BY updatedAt DESC")
    fun getAllBusinessesAdmin(): Flow<List<BusinessEntity>>

    @Query("UPDATE businesses SET isActive = :isActive WHERE id = :id")
    suspend fun updateBusinessActiveStatus(id: String, isActive: Boolean)

    // Admin Contribution Management
    @Query("SELECT * FROM user_contributions ORDER BY createdAt DESC")
    fun getAllContributionsAdmin(): Flow<List<UserContributionEntity>>

    @Query("UPDATE user_contributions SET status = :status, moderatorNote = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateContributionModeration(id: String, status: String, note: String?, updatedAt: Long = System.currentTimeMillis())

    // User Accounts Management
    @Query("SELECT * FROM user_accounts ORDER BY joinedAt DESC")
    fun getAllUserAccounts(): Flow<List<UserAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET status = :status, suspensionReason = :reason WHERE id = :id")
    suspend fun updateUserStatus(id: String, status: String, reason: String?)

    // Audit Log Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    // Reviews Moderation Queries
    @Query("SELECT * FROM reviews ORDER BY timestamp DESC")
    fun getAllReviewsAdmin(): Flow<List<ReviewEntity>>

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReviewAdmin(id: String)

    // Reports Queries
    @Query("SELECT * FROM review_reports ORDER BY timestamp DESC")
    fun getAllReportsAdmin(): Flow<List<ReviewReportEntity>>

    @Query("UPDATE review_reports SET status = 'RESOLVED' WHERE id = :id")
    suspend fun resolveReportAdmin(id: String)

    // --- PHASE 11: BACKUP, RESTORE & BULK DATA QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupRecord(record: com.example.data.model.BackupRecordEntity)

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    fun getAllBackupRecords(): Flow<List<com.example.data.model.BackupRecordEntity>>

    @Query("DELETE FROM businesses")
    suspend fun deleteAllBusinesses()

    @Query("DELETE FROM reviews")
    suspend fun deleteAllReviews()

    @Query("DELETE FROM user_contributions")
    suspend fun deleteAllContributions()

    @Query("DELETE FROM review_reports")
    suspend fun deleteAllReports()

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    @Query("SELECT * FROM user_accounts")
    suspend fun getAllUserAccountsDirect(): List<UserAccountEntity>

    @Query("SELECT * FROM businesses")
    suspend fun getAllBusinessesDirect(): List<BusinessEntity>

    @Query("SELECT * FROM reviews")
    suspend fun getAllReviewsDirect(): List<ReviewEntity>

    @Query("SELECT * FROM user_contributions")
    suspend fun getAllContributionsDirect(): List<UserContributionEntity>

    @Query("SELECT * FROM review_reports")
    suspend fun getAllReportsDirect(): List<ReviewReportEntity>

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotificationsDirect(): List<NotificationEntity>

    @Query("SELECT * FROM audit_logs")
    suspend fun getAllAuditLogsDirect(): List<AuditLogEntity>

    // --- MULTI-SOURCE & PROVENANCE QUERIES ---
    @Query("SELECT * FROM business_sources WHERE businessId = :businessId AND isActive = 1 ORDER BY isOfficial DESC, sourceConfidence DESC")
    fun getSourcesForBusiness(businessId: String): Flow<List<BusinessSourceEntity>>

    @Query("SELECT * FROM business_sources WHERE businessId = :businessId AND isActive = 1 ORDER BY isOfficial DESC, sourceConfidence DESC")
    suspend fun getSourcesForBusinessDirect(businessId: String): List<BusinessSourceEntity>

    @Query("SELECT * FROM business_sources WHERE isActive = 1")
    suspend fun getAllBusinessSourcesDirect(): List<BusinessSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessSource(source: BusinessSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessSources(sources: List<BusinessSourceEntity>)

    @Query("DELETE FROM business_sources WHERE businessId = :businessId")
    suspend fun deleteSourcesForBusiness(businessId: String)

    // --- FIELD AUDIT HISTORY QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldAudit(audit: FieldAuditHistoryEntity)

    @Query("SELECT * FROM field_audit_history WHERE businessId = :businessId ORDER BY changedAt DESC")
    fun getFieldAuditsForBusiness(businessId: String): Flow<List<FieldAuditHistoryEntity>>

    @Query("SELECT * FROM field_audit_history ORDER BY changedAt DESC LIMIT 100")
    fun getRecentFieldAudits(): Flow<List<FieldAuditHistoryEntity>>

    // --- MERGE HISTORY & ROLLBACK QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMergeHistory(merge: MergeHistoryEntity)

    @Query("SELECT * FROM merge_history ORDER BY mergedAt DESC")
    fun getAllMergeHistories(): Flow<List<MergeHistoryEntity>>

    @Query("SELECT * FROM merge_history ORDER BY mergedAt DESC")
    suspend fun getAllMergeHistoriesDirect(): List<MergeHistoryEntity>

    @Query("SELECT * FROM merge_history WHERE id = :id LIMIT 1")
    suspend fun getMergeHistoryById(id: String): MergeHistoryEntity?

    @Update
    suspend fun updateMergeHistory(merge: MergeHistoryEntity)

    // --- RAW DISCOVERED STAGING QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawDiscoveredRecords(records: List<RawDiscoveredRecordEntity>)

    @Query("SELECT * FROM raw_discovered_records WHERE jobId = :jobId")
    suspend fun getRawRecordsForJob(jobId: String): List<RawDiscoveredRecordEntity>

    // --- CORRUPTED / DUMMY IMPORTED RECORDS CLEANUP ---
    @Query("DELETE FROM businesses WHERE name LIKE 'نشاط مستورد%' OR (phone LIKE '050000000%' AND description LIKE '%الاستيراد الجماعي Excel%')")
    suspend fun deleteCorruptedImportedBusinesses(): Int

    @Query("SELECT COUNT(*) FROM businesses WHERE name LIKE 'نشاط مستورد%' OR (phone LIKE '050000000%' AND description LIKE '%الاستيراد الجماعي Excel%')")
    suspend fun getCorruptedImportedBusinessesCount(): Int
}
