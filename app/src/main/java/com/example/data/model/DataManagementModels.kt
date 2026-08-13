package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ============================================================
// PHASE 11: BACKUP, RESTORE & BULK DATA IMPORT MODELS
// Met Ghamr Directory - Ajilika Technologies
// ============================================================

enum class BackupType(val titleAr: String, val descriptionAr: String) {
    FULL("نسخة كاملة (Full Backup)", "تشمل المستخدمين، الأنشطة، التقييمات، المساهمات، والإشعار وسجل الرقابة"),
    OPERATIONAL("نسخة بيانات تشغيلية (Operational Data)", "تشمل الأنشطة التجارية والتصنيفات والتقييمات فقط")
}

data class BackupManifest(
    val backupId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "m.k3shka@gmail.com",
    val appVersion: String = "2.5.0",
    val schemaVersion: Int = 5,
    val databaseVersion: Int = 5,
    val backupType: String = BackupType.FULL.name,
    val recordCounts: Map<String, Int> = emptyMap(),
    val checksum: String = "",
    val formatVersion: String = "1.0"
)

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val backupType: String,
    val sizeBytes: Long,
    val storageLocation: String, // "LOCAL_DEVICE", "GOOGLE_DRIVE", "SERVER_STORAGE"
    val status: String, // "SUCCESS", "PRE_RESTORE_SAFETY", "FAILED"
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "m.k3shka@gmail.com",
    val recordCountsJson: String = "{}",
    val checksum: String = "",
    val driveFileId: String? = null
)

data class RestoreReportSummary(
    val restoreId: String,
    val backupId: String,
    val restoredAt: Long = System.currentTimeMillis(),
    val restoredBy: String = "m.k3shka@gmail.com",
    val usersRestored: Int = 0,
    val businessesRestored: Int = 0,
    val categoriesRestored: Int = 0,
    val reviewsRestored: Int = 0,
    val contributionsRestored: Int = 0,
    val reportsRestored: Int = 0,
    val notificationsRestored: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val preRestoreSafetyBackupId: String? = null,
    val isSuccess: Boolean = true,
    val notes: String = "تمت استعادة قاعدة البيانات والتحقق من النزاهة بنجاح"
)

enum class BulkImportType(val titleAr: String) {
    BUSINESSES("الأنشطة التجارية والعيادات"),
    CATEGORIES("التصنيفات والقطاعات")
}

enum class ImportMode(val titleAr: String) {
    INSERT_ONLY("إضافة السجلات الجديدة فقط (تخطي المكرر)"),
    UPSERT("إضافة الجديد وتحديث البيانات الموجودة عند التطابق")
}

data class ImportRowError(
    val rowNumber: Int,
    val field: String,
    val value: String,
    val error: String,
    val suggestedFix: String
)

data class ImportResultSummary(
    val importId: String,
    val type: String,
    val fileName: String,
    val totalRows: Int = 0,
    val successCount: Int = 0,
    val updatedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<ImportRowError> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isDryRun: Boolean = false,
    val batchId: String = ""
)
