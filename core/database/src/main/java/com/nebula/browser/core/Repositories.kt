package com.nebula.browser.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(private val dao: BookmarkDao) {
    val bookmarks: Flow<List<Bookmark>> = dao.observeAll().map { list -> list.map(BookmarkEntity::toModel) }
    fun search(query: String): Flow<List<Bookmark>> = dao.search(query).map { list -> list.map(BookmarkEntity::toModel) }
    suspend fun isBookmarked(url: String) = dao.exists(url)
    suspend fun save(bookmark: Bookmark) = dao.save(bookmark.toEntity())
    suspend fun update(bookmark: Bookmark) = dao.update(bookmark.toEntity())
    suspend fun delete(id: Long) = dao.deleteById(id)
}

class HistoryRepository(private val dao: HistoryDao) {
    val history: Flow<List<HistoryEntry>> = dao.observeRecent().map { list -> list.map(HistoryEntity::toModel) }
    fun search(query: String): Flow<List<HistoryEntry>> = dao.search(query).map { list -> list.map(HistoryEntity::toModel) }

    suspend fun record(url: String, title: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val existing = dao.byUrl(url)
        dao.insert(
            HistoryEntity(
                id = existing?.id ?: 0,
                title = title.ifBlank { existing?.title ?: url },
                url = url,
                visitedAt = System.currentTimeMillis(),
                visitCount = (existing?.visitCount ?: 0) + 1,
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
    suspend fun clear() = dao.clear()
    suspend fun clearRange(start: Long, end: Long) = dao.deleteRange(start, end)
}
