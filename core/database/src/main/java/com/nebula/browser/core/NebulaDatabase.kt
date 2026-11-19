package com.nebula.browser.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, DownloadEntity::class, TabEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class NebulaDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
    abstract fun history(): HistoryDao
    abstract fun downloads(): DownloadDao
    abstract fun tabs(): TabDao

    companion object {
        @Volatile private var instance: NebulaDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS tabs (id TEXT NOT NULL PRIMARY KEY, url TEXT NOT NULL, title TEXT NOT NULL, isPinned INTEGER NOT NULL, workspaceId TEXT NOT NULL, groupName TEXT, desktopMode INTEGER NOT NULL, previewPath TEXT, createdAt INTEGER NOT NULL, lastOpenedAt INTEGER NOT NULL)")
            }
        }

        fun get(context: Context): NebulaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NebulaDatabase::class.java, "nebula.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { instance = it }
        }
    }
}
