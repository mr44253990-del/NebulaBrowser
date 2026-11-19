package com.nebula.browser.core

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks", indices = [Index(value = ["url"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val folder: String,
    val tags: String,
    val notes: String,
    val createdAt: Long,
)

@Entity(tableName = "history", indices = [Index(value = ["url"], unique = true), Index(value = ["visitedAt"])])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val visitedAt: Long,
    val visitCount: Int,
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val status: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val createdAt: Long,
    val error: String?,
)

fun BookmarkEntity.toModel() = Bookmark(id, title, url, folder, tags, notes, createdAt)
fun Bookmark.toEntity() = BookmarkEntity(id, title, url, folder, tags, notes, createdAt)
fun HistoryEntity.toModel() = HistoryEntry(id, title, url, visitedAt, visitCount)
fun DownloadEntity.toModel() = DownloadItem(
    id, url, fileName, mimeType, DownloadStatus.valueOf(status), bytesDownloaded, totalBytes, createdAt, error
)
fun DownloadItem.toEntity() = DownloadEntity(
    id, url, fileName, mimeType, status.name, bytesDownloaded, totalBytes, createdAt, error
)
