package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ==========================================
// PHASE 10: ADMIN & MODERATION MODELS & RBAC
// ==========================================

enum class AdminRole(val displayNameAr: String) {
    SUPER_ADMIN("مدير النظام الأعلى"),
    ADMIN("مدير عام"),
    MODERATOR("مشرف محتوى"),
    EDITOR("محرر بيانات"),
    SUPPORT("دعم فني"),
    ANALYST("محصل إحصائيات")
}

enum class AdminPermission {
    BUSINESS_VIEW,
    BUSINESS_CREATE,
    BUSINESS_EDIT,
    BUSINESS_DELETE,
    BUSINESS_APPROVE,
    BUSINESS_REJECT,

    CATEGORY_VIEW,
    CATEGORY_CREATE,
    CATEGORY_EDIT,
    CATEGORY_DELETE,

    REVIEW_VIEW,
    REVIEW_HIDE,
    REVIEW_DELETE,
    REVIEW_RESTORE,
    REVIEW_MODERATE,

    CONTRIBUTION_VIEW,
    CONTRIBUTION_APPROVE,
    CONTRIBUTION_REJECT,
    CONTRIBUTION_EDIT,

    USER_VIEW,
    USER_SUSPEND,
    USER_RESTORE,

    REPORT_VIEW,
    REPORT_RESOLVE,

    ANALYTICS_VIEW,
    NOTIFICATION_SEND,
    AUDIT_VIEW
}

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val provider: String = "GOOGLE",
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED, DISABLED
    val role: String = "USER",      // USER, MODERATOR, ADMIN, SUPER_ADMIN
    val reviewsCount: Int = 0,
    val contributionsCount: Int = 0,
    val reportsCount: Int = 0,
    val joinedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val suspensionReason: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val detailsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminKPIsSummary(
    val totalBusinesses: Int = 0,
    val activeBusinesses: Int = 0,
    val pendingBusinesses: Int = 0,
    val totalUsers: Int = 0,
    val totalReviews: Int = 0,
    val pendingContributions: Int = 0,
    val openReports: Int = 0
)

data class DataQualityMetrics(
    val totalBusinesses: Int = 0,
    val missingPhoneCount: Int = 0,
    val missingAddressCount: Int = 0,
    val missingHoursCount: Int = 0,
    val missingImageCount: Int = 0
)
