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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "met_ghamr_directory.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
