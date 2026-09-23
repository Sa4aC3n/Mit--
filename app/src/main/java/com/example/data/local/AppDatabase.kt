package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.BackupRecordEntity
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

@Database(
    entities = [
        BusinessEntity::class,
        BusinessSourceEntity::class,
        FieldAuditHistoryEntity::class,
        MergeHistoryEntity::class,
        RawDiscoveredRecordEntity::class,
        ReviewEntity::class,
        ReviewHelpfulEntity::class,
        ReviewReportEntity::class,
        FavoriteEntity::class,
        NotificationEntity::class,
        UserContributionEntity::class,
        ContributionDraftEntity::class,
        UserAccountEntity::class,
        AuditLogEntity::class,
        BackupRecordEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun directoryDao(): DirectoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Ensure backup_records table exists for data persistence
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `backup_records` (
                        `id` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `backupType` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `storageLocation` TEXT NOT NULL,
                        `cloudUrl` TEXT,
                        `checksumSha256` TEXT NOT NULL,
                        `recordCountsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `createdByUserEmail` TEXT,
                        `status` TEXT NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "met_ghamr_directory.db"
                )
                    .addMigrations(MIGRATION_9_10)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
