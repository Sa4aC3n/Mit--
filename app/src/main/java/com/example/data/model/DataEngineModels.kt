package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ============================================================
// SMART DIRECTORY DATA ENGINE MODELS
// Met Ghamr Directory - Ajilika Technologies
// ============================================================

enum class BusinessSourceType(val displayNameAr: String, val defaultReliability: Int) {
    GOOGLE("Google Places / Maps", 90),
    FACEBOOK("Facebook Business Pages", 85),
    WEBSITE("Official Website", 95),
    YELLOW_PAGES("Yellow Pages & Public Directories", 80),
    INSTAGRAM("Instagram Business", 80),
    USER("Ø§Ù‚ØªØ±Ø§Ø­Ø§Øª Ø§Ù„Ù…Ø³ØªØ®Ø¯Ù…ÙŠÙ†", 70),
    ADMIN("Ø¥Ø¯Ø®Ø§Ù„ ÙˆØªØ¯Ù‚ÙŠÙ‚ Ø§Ù„Ù…Ø´Ø±Ù", 100),
    OTHER("Ù…ØµØ§Ø¯Ø± Ù…Ø­Ù„ÙŠØ© Ø¹Ø§Ù…Ø©", 75)
}

enum class DiscoverySourceType(val displayNameAr: String, val defaultReliability: Int) {
    GOOGLE_PLACES("Google Places / Maps", 90),
    FACEBOOK_PAGES("Facebook Business Pages", 85),
    YELLOW_PAGES("Yellow Pages & Public Directories", 80),
    OFFICIAL_WEBSITE("Ø§Ù„Ù…ÙˆÙ‚Ø¹ Ø§Ù„Ø¥Ù„ÙƒØªØ±ÙˆÙ†ÙŠ Ø§Ù„Ø±Ø³Ù…ÙŠ", 95),
    PUBLIC_LOCAL("Ø§Ù„Ù…ØµØ§Ø¯Ø± ÙˆØ§Ù„Ø®Ø±Ø§Ø¦Ø· Ø§Ù„Ù…Ø­Ù„ÙŠØ© Ø§Ù„Ø¹Ø§Ù…Ø©", 75),
    USER_CONTRIBUTION("Ø§Ù‚ØªØ±Ø§Ø­Ø§Øª Ø§Ù„Ù…Ø³ØªØ®Ø¯Ù…ÙŠÙ† ÙˆØ§Ù„Ù…Ø¬ØªÙ…Ø¹", 70),
    ADMIN_MANUAL("Ø¥Ø¯Ø®Ø§Ù„ ÙˆØªØ¯Ù‚ÙŠÙ‚ Ø§Ù„Ù…Ø´Ø±Ù (Admin)", 100)
}

/**
 * First-Class Multi-Source Model.
 * Represents a single external source providing data for a Business.
 * A single Business can have multiple BusinessSources (e.g. Google + Facebook + Website + Yellow Pages).
 */
@Entity(
    tableName = "business_sources",
    indices = [Index(value = ["businessId"]), Index(value = ["sourceType", "sourceId"])]
)
data class BusinessSourceEntity(
    @PrimaryKey val id: String, // e.g. "src_google_12345"
    val businessId: String,
    val sourceType: String, // "GOOGLE", "FACEBOOK", "WEBSITE", "YELLOW_PAGES", "INSTAGRAM", "USER", "ADMIN", "OTHER"
    val sourceId: String? = null, // e.g. Google Place ID, Facebook Page ID
    val sourceUrl: String? = null,
    val sourceName: String, // Display label e.g. "Google Maps", "Facebook Page"
    val rawPayloadJson: String? = null, // Stored raw attributes
    val discoveredAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long? = null,
    val sourceConfidence: Int = 85, // 0 - 100
    val isOfficial: Boolean = false,
    val isActive: Boolean = true
)

/**
 * Field-level provenance change history log.
 */
@Entity(tableName = "field_audit_history", indices = [Index(value = ["businessId"])])
data class FieldAuditHistoryEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val fieldName: String,
    val fieldLabelAr: String,
    val oldValue: String?,
    val newValue: String?,
    val source: String, // e.g. "GOOGLE", "FACEBOOK", "OFFICIAL_WEBSITE", "ADMIN"
    val sourceUrl: String? = null,
    val changedAt: Long = System.currentTimeMillis(),
    val changedBy: String = "DATA_ENGINE"
)

/**
 * Merge audit history supporting full Rollback / Undo Merge capability.
 */
@Entity(tableName = "merge_history", indices = [Index(value = ["masterBusinessId"])])
data class MergeHistoryEntity(
    @PrimaryKey val id: String,
    val masterBusinessId: String,
    val masterBusinessName: String,
    val candidateId: String,
    val candidateName: String,
    val previousMasterJson: String, // Snapshot for full rollback
    val candidateJson: String,      // Snapshot of candidate
    val mergedFieldsSummary: String, // e.g. "Phone, Website, Facebook"
    val mergedAt: Long = System.currentTimeMillis(),
    val mergedBy: String = "",  // must be set from Firebase Auth currentUser.uid â€” never hardcoded
    val isReverted: Boolean = false,
    val revertedAt: Long? = null
)

/**
 * Raw Staging Record. Discovered data that is NEVER published directly.
 */
@Entity(tableName = "raw_discovered_records")
data class RawDiscoveredRecordEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val source: String, // "GOOGLE", "FACEBOOK", etc.
    val sourceUrl: String? = null,
    val rawName: String,
    val rawPhone: String? = null,
    val rawAddress: String? = null,
    val rawCategory: String? = null,
    val rawDataJson: String? = null,
    val status: String = "STAGED", // "STAGED", "NORMALIZED", "RESOLVED", "MERGED", "REJECTED"
    val discoveredAt: Long = System.currentTimeMillis()
)

data class BusinessWithSources(
    val business: BusinessEntity,
    val sources: List<BusinessSourceEntity>
)

data class SideBySideMergePreview(
    val masterBusiness: BusinessEntity,
    val candidate: CandidateBusiness,
    val mergedResult: BusinessEntity,
    val sourcesToAttach: List<BusinessSourceEntity>,
    val detectedConflicts: List<DataConflictItem>,
    val updatedFields: List<String>
)

enum class CandidateStatus(val displayNameAr: String, val colorHex: Long) {
    NEW("Ø¬Ø¯ÙŠØ¯ ÙƒÙ„ÙŠØ§Ù‹ (New)", 0xFF10B981),
    DEFINITE_DUPLICATE("Ù…ÙƒØ±Ø± Ù…Ø¤ÙƒØ¯ (Duplicate)", 0xFFEF4444),
    LIKELY_DUPLICATE("Ù…ÙƒØ±Ø± Ù…Ø­ØªÙ…Ù„ Ø¬Ø¯Ø§Ù‹ (Likely Duplicate)", 0xFFF59E0B),
    POSSIBLE_DUPLICATE("ØªØ´Ø§Ø¨Ù‡ Ø¨Ø­Ø§Ø¬Ø© Ù„Ù…Ø±Ø§Ø¬Ø¹Ø© (Possible Duplicate)", 0xFF3B82F6),
    CONFLICT("ØªØ¹Ø§Ø±Ø¶ Ø¨ÙŠØ§Ù†Ø§Øª (Data Conflict)", 0xFF8B5CF6),
    ENRICHED("Ù†Ø´Ø§Ø· Ù…Ø«Ø±Ù‰ (Enriched)", 0xFF06B6D4),
    UPDATED("ØªØ­Ø¯ÙŠØ« Ø¨ÙŠØ§Ù†Ø§Øª (Updated)", 0xFF14B8A6),
    NEEDS_REVIEW("Ø¨Ø­Ø§Ø¬Ø© Ù„Ù…Ø±Ø§Ø¬Ø¹Ø© Ø¯Ù‚ÙŠÙ‚Ø©", 0xFFF97316),
    APPROVED("Ù…Ø¹ØªÙ…Ø¯ ÙˆÙ…Ø¶Ø§Ù Ù„Ù„Ù‚Ø§Ø¹Ø¯Ø©", 0xFF059669),
    REJECTED("Ù…Ø±ÙÙˆØ¶", 0xFF6B7280)
}

enum class VerificationState(val displayNameAr: String) {
    UNVERIFIED("ØºÙŠØ± Ù…ÙˆØ«Ù‚ (Unverified)"),
    PARTIALLY_VERIFIED("Ù…ÙˆØ«Ù‚ Ø¬Ø²Ø¦ÙŠØ§Ù‹ (Partially Verified)"),
    VERIFIED("Ù…ÙˆØ«Ù‚ ÙˆÙ…Ø¹ØªÙ…Ø¯ Ø¨Ø§Ù„ÙƒØ§Ù…Ù„ (Verified)"),
    NEEDS_REVIEW("ÙŠØªØ·Ù„Ø¨ Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„ØªØ­Ù‚Ù‚ (Needs Review)"),
    POSSIBLY_CLOSED("Ù…Ø­ØªÙ…Ù„ Ø¥ØºÙ„Ø§Ù‚Ù‡ Ù†Ù‡Ø§Ø¦ÙŠØ§Ù‹ (Possibly Closed)")
}

data class FieldProvenance(
    val fieldName: String,
    val value: String,
    val source: DiscoverySourceType,
    val sourceUrl: String? = null,
    val confidence: Int = 85,
    val extractedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "candidate_businesses")
data class CandidateBusiness(
    @PrimaryKey val id: String,
    val businessName: String,
    val categoryId: String,
    val categoryName: String,
    val subcategory: String? = null,
    val specialty: String = "",
    val description: String = "",
    val phone: String? = null,
    val displayPhone: String? = null,
    val normalizedPhone: String? = null,
    val secondaryPhone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val address: String = "",
    val area: String = "",
    val city: String = "Ù…Ø¯ÙŠÙ†Ø© Ù…ÙŠØª ØºÙ…Ø±",
    val governorate: String = "Ø§Ù„Ø¯Ù‚Ù‡Ù„ÙŠØ©",
    val latitude: Double? = null,   // null = no confirmed coordinates; never fabricate
    val longitude: Double? = null,  // null = no confirmed coordinates; never fabricate
    val googlePlaceId: String? = null,
    val website: String? = null,
    val facebookPage: String? = null,
    val instagram: String? = null,
    val youtube: String? = null,
    val workingHours: String = "",
    val is24Hours: Boolean = false,
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val priceLevel: String? = null,
    val services: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    // Provenance & Source Metadata
    val primarySource: DiscoverySourceType = DiscoverySourceType.GOOGLE_PLACES,
    val sourceUrl: String? = null,
    val sourceIds: List<String> = emptyList(),
    val fieldProvenances: Map<String, String> = emptyMap(), // fieldName -> SourceName:Url
    // Deduplication & Matching
    val duplicateScore: Int = 0, // 0 - 100
    val matchCandidateId: String? = null, // existing Business ID or Candidate ID
    val duplicateReasons: List<String> = emptyList(),
    val isBranch: Boolean = false,
    val branchName: String? = null,
    // Conflict & Quality
    val hasConflict: Boolean = false,
    val conflictDetails: String? = null,
    val qualityScore: Int = 0, // 0 - 100
    val status: CandidateStatus = CandidateStatus.NEW,
    val verificationStatus: VerificationState = VerificationState.UNVERIFIED,
    val jobId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis()
)

data class DataConflictItem(
    val conflictId: String,
    val candidateId: String,
    val businessName: String,
    val fieldName: String,
    val fieldLabelAr: String,
    val valueA: String,
    val sourceA: DiscoverySourceType,
    val valueB: String,
    val sourceB: DiscoverySourceType,
    val status: String = "OPEN", // "OPEN", "RESOLVED"
    val resolvedValue: String? = null,
    val resolvedBy: String? = null
)

data class DuplicateCandidateGroup(
    val groupId: String,
    val primaryBusiness: CandidateBusiness,
    val duplicateCandidates: List<CandidateBusiness>,
    val similarityScore: Int,
    val matchFactors: List<String>,
    val recommendationAr: String
)

data class SuggestedCategoryItem(
    val id: String,
    val nameAr: String,
    val suggestedParentId: String,
    val parentCategoryName: String,
    val sampleBusinessName: String,
    val countFound: Int = 1,
    val source: DiscoverySourceType = DiscoverySourceType.GOOGLE_PLACES,
    val confidence: Int = 85,
    val status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED"
)

data class SuggestedAreaItem(
    val id: String,
    val nameAr: String,
    val type: String, // "VILLAGE", "NEIGHBORHOOD", "COMMERCIAL_STREET", "INDUSTRIAL_ZONE"
    val typeLabelAr: String,
    val parentCenter: String = "Ù…Ø±ÙƒØ² Ù…ÙŠØª ØºÙ…Ø±",
    val sampleBusinessName: String,
    val source: DiscoverySourceType = DiscoverySourceType.GOOGLE_PLACES,
    val confidence: Int = 85,
    val status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED"
)

enum class QueryPriority(val labelAr: String, val weight: Int) {
    HIGH("Ø£ÙˆÙ„ÙˆÙŠØ© Ù…Ø±ØªÙØ¹Ø© (High)", 1),
    MEDIUM("Ø£ÙˆÙ„ÙˆÙŠØ© Ù…ØªÙˆØ³Ø·Ø© (Medium)", 2),
    LOW("Ø£ÙˆÙ„ÙˆÙŠØ© Ù…Ù†Ø®ÙØ¶Ø© (Low)", 3)
}

data class AreaCoverageStat(
    val areaName: String,
    val areaType: String,
    val totalBusinessesFound: Int,
    val verifiedCount: Int,
    val estimatedTotal: Int,
    val coverageScorePercent: Int,
    val lastDiscoveryJobAt: Long? = null
)

enum class JobStatus(val titleAr: String) {
    QUEUED("ÙÙŠ Ø§Ù„Ø§Ù†ØªØ¸Ø§Ø±"),
    RUNNING("Ø¬Ø§Ø±ÙŠ Ø§Ù„Ø¹Ù…Ù„ ÙˆØ§Ù„Ø¬Ù…Ø¹..."),
    PAUSED("Ù…ØªÙˆÙ‚Ù Ù…Ø¤Ù‚ØªØ§Ù‹"),
    COMPLETED("Ø§ÙƒØªÙ…Ù„ Ø¨Ù†Ø¬Ø§Ø­"),
    CANCELLED("Ù…Ù„ØºÙŠ"),
    FAILED("ÙØ´Ù„ Ø§Ù„Ø§Ø³ØªØ¹Ù„Ø§Ù…")
}

@Entity(tableName = "discovery_jobs")
data class DiscoveryJob(
    @PrimaryKey val id: String,
    val title: String,
    val targetCategory: String = "ALL",
    val targetArea: String = "ALL",
    val sources: List<String> = listOf("GOOGLE_PLACES", "FACEBOOK_PAGES", "YELLOW_PAGES", "OFFICIAL_WEBSITE"),
    val targetCount: Int = 100,
    val status: JobStatus = JobStatus.QUEUED,
    val progressPercent: Int = 0,
    val currentQuery: String = "",
    val totalDiscovered: Int = 0,
    val newCount: Int = 0,
    val duplicateCount: Int = 0,
    val possibleDuplicateCount: Int = 0,
    val enrichedCount: Int = 0,
    val conflictsCount: Int = 0,
    val qualityScoreAvg: Int = 0,
    val errorMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val createdBy: String = "",  // must be set from Firebase Auth currentUser.uid — never hardcoded
)

data class CoverageReportCategory(
    val categoryId: String,
    val categoryNameAr: String,
    val existingCount: Int,
    val discoveredCount: Int,
    val estimatedTotal: Int,
    val coveragePercent: Int,
    val verifiedPercent: Int,
    val missingPhonePercent: Int,
    val missingWebsitePercent: Int,
    val missingFacebookPercent: Int
)

data class EngineAnalyticsSummary(
    val totalDiscoveredToday: Int = 0,
    val totalDiscoveredThisWeek: Int = 0,
    val totalDiscoveredThisMonth: Int = 0,
    val verifiedPercentage: Int = 0,
    val unverifiedPercentage: Int = 0,
    val missingPhonePercentage: Int = 0,
    val missingWebsitePercentage: Int = 0,
    val missingFacebookPercentage: Int = 0,
    val duplicateDetectionRate: Int = 0,
    val averageQualityScore: Int = 0,
    val activeJobsCount: Int = 0,
    val pendingReviewCount: Int = 0,
    val openConflictsCount: Int = 0,
    val suggestedCategoriesCount: Int = 0
)

data class GeographicDistrictItem(
    val id: String,
    val nameAr: String,
    val type: String, // "CITY_DISTRICT", "VILLAGE", "CENTER_MAIN_STREET"
    val parentCity: String = "Ù…Ø±ÙƒØ² ÙˆÙ…Ø¯ÙŠÙ†Ø© Ù…ÙŠØª ØºÙ…Ø±",
    val latitude: Double? = null,   // null = no confirmed coordinates; never fabricate
    val longitude: Double? = null,  // null = no confirmed coordinates; never fabricate
    val priority: Int = 1
)

