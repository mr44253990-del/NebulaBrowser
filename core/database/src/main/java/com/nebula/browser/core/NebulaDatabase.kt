package com.nebula.browser.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, DownloadEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NebulaDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
    abstract fun history(): HistoryDao
    abstract fun downloads(): DownloadDao

    companion object {
        @Volatile private var instance: NebulaDatabase? = null

        fun get(context: Context): NebulaDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NebulaDatabase::class.java, "nebula.db")
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                .also { instance = it }
        }
    }
}
