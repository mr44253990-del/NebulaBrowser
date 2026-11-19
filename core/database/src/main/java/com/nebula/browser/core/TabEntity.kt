package com.nebula.browser.core

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val isPinned: Boolean,
    val workspaceId: String,
    val groupName: String?,
    val desktopMode: Boolean,
    val previewPath: String?,
    val createdAt: Long,
    val lastOpenedAt: Long,
)

fun TabEntity.toModel() = BrowserTab(
    id = id, url = url, title = title, isPrivate = false, isPinned = isPinned,
    workspaceId = workspaceId, groupName = groupName, desktopMode = desktopMode,
    previewPath = previewPath, createdAt = createdAt, lastOpenedAt = lastOpenedAt,
)
fun BrowserTab.toEntity() = TabEntity(
    id, url, title, isPinned, workspaceId, groupName, desktopMode, previewPath, createdAt, lastOpenedAt,
)

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY lastOpenedAt DESC")
    suspend fun savedTabs(): List<TabEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tabs")
    suspend fun clear()
}

class TabRepository(private val dao: TabDao) {
    suspend fun savedTabs(): List<BrowserTab> = dao.savedTabs().map(TabEntity::toModel)
    suspend fun save(tab: BrowserTab) { if (!tab.isPrivate) dao.save(tab.toEntity()) }
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun clear() = dao.clear()
}
