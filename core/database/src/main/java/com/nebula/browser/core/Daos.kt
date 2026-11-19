package com.nebula.browser.core

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun exists(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 500): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visitedAt DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun byUrl(url: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE visitedAt BETWEEN :start AND :end")
    suspend fun deleteRange(start: Long, end: Long)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun byId(id: Long): DownloadEntity?

    @Query("SELECT id FROM downloads")
    suspend fun ids(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}
