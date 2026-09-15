package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val specialty: String,
    val description: String,
    val phone: String,
    val phoneSecondary: String? = null,
    val whatsapp: String? = null,
    val city: String = "مدينة ميت غمر", // المدينة أو القرية (مثل ميت غمر، بشلا، صهرجت الكبرى، ميت ناجي...)
    val address: String,
    val area: String, // e.g. شارع الحرية، صهرجت الكبرى، أتميدة، كوم النور، دنديط
    val facebookUrl: String? = null,
    val websiteUrl: String? = null,
    val latitude: Double = 30.7183,
    val longitude: Double = 31.2568,
    val workingHours: String,
    val isOpenNow: Boolean = true,
    val isVerified: Boolean = false,
    val isActive: Boolean = true,
    val ratingAverage: Float = 0.0f,
    val ratingCount: Int = 0,
    val viewCount: Int = 0,
    val imageUrl: String? = null,
    // --- Data Quality, Multi-Source & Provenance ---
    val verificationStatus: String = if (isVerified) "VERIFIED" else "UNVERIFIED",
    val dataQualityScore: Int = 85,
    val phoneSource: String? = null,
    val websiteSource: String? = null,
    val facebookSource: String? = null,
    val addressSource: String? = null,
    val workingHoursSource: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = true,
    val isDeleted: Boolean = false,
    val archivedAt: Long? = null,
    val approvedContributionId: String? = null
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val userProvider: String, // "GOOGLE", "FACEBOOK", "MICROSOFT"
    val rating: Float, // 1.0 to 5.0
    val comment: String,
    val status: String = "APPROVED", // "APPROVED", "PENDING", "REJECTED", "HIDDEN", "DELETED"
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = timestamp,
    val helpfulCount: Int = 0,
    val ownerReply: String? = null,
    val ownerReplyAt: Long? = null
)

@Entity(tableName = "review_helpful_votes", primaryKeys = ["reviewId", "userId"])
data class ReviewHelpfulEntity(
    val reviewId: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "review_reports")
data class ReviewReportEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val userId: String,
    val reason: String,
    val description: String = "",
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val timestamp: Long = System.currentTimeMillis()
)

data class RatingDistribution(
    val totalCount: Int,
    val averageRating: Float,
    val star5Count: Int,
    val star4Count: Int,
    val star3Count: Int,
    val star2Count: Int,
    val star1Count: Int
) {
    fun getStarPercentage(star: Int): Float {
        if (totalCount <= 0) return 0f
        val count = when (star) {
            5 -> star5Count
            4 -> star4Count
            3 -> star3Count
            2 -> star2Count
            1 -> star1Count
            else -> 0
        }
        return count.toFloat() / totalCount.toFloat()
    }
}

enum class ReviewSortOption(val displayNameAr: String) {
    NEWEST("الأحدث"),
    OLDEST("الأقدم"),
    HIGHEST_RATED("الأعلى تقييمًا"),
    LOWEST_RATED("الأقل تقييمًا"),
    MOST_HELPFUL("الأكثر فائدة")
}

@Entity(tableName = "favorites", primaryKeys = ["userId", "businessId"])
data class FavoriteEntity(
    val userId: String = "guest",
    val businessId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val categoryId: String? = null,
    val businessId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class SubcategoryItem(
    val id: String,
    val parentCategoryId: String,
    val nameAr: String,
    val keywords: List<String> = emptyList(),
    val count: Int = 0
)

data class CategoryItem(
    val id: String,
    val nameAr: String,
    val iconName: String,
    val count: Int = 0,
    val colorHex: Long,
    val description: String = "",
    val isFeatured: Boolean = false,
    val sortOrder: Int = 0,
    val subcategories: List<SubcategoryItem> = emptyList()
)

enum class AuthProvider(val displayName: String, val brandColor: Long) {
    EMAIL("البريد الإلكتروني", 0xFF0284C7),
    GOOGLE("Google", 0xFF4285F4),
    FACEBOOK("Facebook", 0xFF1877F2),
    MICROSOFT("Microsoft", 0xFF00A4EF)
}

enum class AuthState {
    UNKNOWN,
    CHECKING,
    GUEST,
    AUTHENTICATING,
    AUTHENTICATED,
    ERROR
}

sealed class EmailAuthResult {
    data class Success(val user: UserAccount) : EmailAuthResult()
    data class VerificationRequired(val email: String, val message: String? = null) : EmailAuthResult()
    data class Error(val message: String) : EmailAuthResult()
}

data class UserAccount(
    val id: String,
    val providerId: String = "",
    val providerType: AuthProvider = AuthProvider.GOOGLE,
    val email: String,
    val displayName: String,
    val firstName: String = "",
    val lastName: String = "",
    val photoUrl: String? = null,
    val phone: String? = null,
    val role: String = if (email.trim().equals("m.k3shka@gmail.com", ignoreCase = true)) "SUPER_ADMIN" else "USER",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    val isSuperAdmin: Boolean
        get() = role.uppercase() == "SUPER_ADMIN" || email.trim().equals("m.k3shka@gmail.com", ignoreCase = true)

    // Backward compatibility helper properties
    val name: String get() = displayName
    val avatarUrl: String get() = photoUrl ?: ""
    val provider: AuthProvider get() = providerType
}

data class AdminAnalytics(
    val totalBusinesses: Int,
    val activeBusinesses: Int,
    val verifiedBusinesses: Int,
    val totalUsers: Int,
    val totalReviews: Int,
    val pendingReviews: Int,
    val totalViews: Int,
    val categoryCounts: Map<String, Int>,
    val monthlyGrowth: List<Pair<String, Int>>
)

// ==========================================
// PHASE 9: USER CONTRIBUTIONS MODELS & ENUMS
// ==========================================

enum class ContributionType(val displayNameAr: String) {
    ADD_BUSINESS("إضافة نشاط تجاري جديد"),
    SUGGEST_EDIT("اقتراح تعديل بيانات"),
    REPORT_INCORRECT_DATA("الإبلاغ عن بيانات غير صحيحة"),
    REPORT_CLOSED("الإبلاغ عن إغلاق النشاط"),
    REPORT_DUPLICATE("الإبلاغ عن نشاط مكرر"),
    SUGGEST_CATEGORY("اقتراح تصنيف جديد"),
    OTHER("أخرى")
}

enum class ContributionStatus(val displayNameAr: String, val badgeColorHex: Long) {
    PENDING("قيد المراجعة", 0xFFFFA000),      // Amber
    UNDER_REVIEW("جاري الفحص", 0xFF1E88E5),   // Blue
    APPROVED("تمت الموافقة", 0xFF4CAF50),    // Green
    REJECTED("غير معتمد", 0xFFE53935),       // Red
    CANCELLED("ملغي من المستخدم", 0xFF757575)  // Grey
}

@Entity(tableName = "user_contributions")
data class UserContributionEntity(
    @PrimaryKey val id: String,
    val humanReadableId: String,                // e.g. "#MG-000104"
    val userId: String,
    val userName: String,
    val userEmail: String,
    val businessId: String? = null,            // Null for ADD_BUSINESS
    val businessName: String,                   // Display title
    val type: String,                           // ContributionType.name
    val categoryId: String? = null,
    val fieldName: String? = null,             // Field name if SUGGEST_EDIT
    val oldValue: String? = null,              // Previous value
    val newValue: String? = null,              // Suggested new value
    val payloadJson: String? = null,           // Complete structured payload (e.g. AddBusiness form)
    val userReason: String? = null,            // Additional user note
    val status: String = "PENDING",            // ContributionStatus.name
    val moderatorNote: String? = null,         // Feedback from review team
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val publishedBusinessId: String? = null,
    val submissionSource: String = "ANDROID_APP",
    val syncStatus: String = "SYNCED"          // "SYNCED", "PENDING_UPLOAD"
)

@Entity(tableName = "contribution_drafts")
data class ContributionDraftEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val businessId: String? = null,
    val payloadJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AddBusinessPayload(
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val subcategoryId: String = "",
    val specialization: String = "",
    val phone: String,
    val secondaryPhone: String = "",
    val whatsapp: String = "",
    val city: String = "مدينة ميت غمر",
    val address: String,
    val district: String = "",
    val workingHours: String = "",
    val workingDays: String = "",
    val morningShift: String = "",
    val eveningShift: String = "",
    val isTwoShifts: Boolean = false,
    val description: String = "",
    val facebookUrl: String = "",
    val websiteUrl: String = "",
    val googleMapsUrl: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val userNotes: String = ""
)

object MetGhamrLocations {
    val CITIES_AND_VILLAGES = listOf(
        "مدينة ميت غمر",
        "بشلا",
        "صهرجت الكبرى",
        "ميت ناجي",
        "أتميدة",
        "كوم النور",
        "دنديط",
        "تفهنا الأشراف",
        "سنفا",
        "ميت العز",
        "سنتماي",
        "ميت أبو خالد",
        "دماص",
        "كفر المقدام",
        "المعصرة",
        "الرحمانية",
        "ميت القرموص",
        "أوليلة",
        "بهيدة",
        "ميت يعيش",
        "ميت المحسن",
        "قرية أخرى"
    )
}

