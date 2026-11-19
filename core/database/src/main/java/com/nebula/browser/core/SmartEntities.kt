package com.nebula.browser.core

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @PrimaryKey val query: String,
    val count: Int,
    val lastSearchedAt: Long,
)

@Entity(tableName = "user_scripts")
data class UserScriptEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val code: String,
    val matches: String,
    val grants: String,
    val enabled: Boolean,
    val createdAt: Long,
)

fun SearchQueryEntity.toModel() = SearchQuery(query, count, lastSearchedAt)
fun UserScriptEntity.toModel() = UserScript(id, name, version, code, matches.lines().filter { it.isNotBlank() }, grants.lines().filter { it.isNotBlank() }, enabled, createdAt)
fun UserScript.toEntity() = UserScriptEntity(id, name, version, code, matches.joinToString("\n"), grants.joinToString("\n"), enabled, createdAt)

@Dao
interface SearchQueryDao {
    @Query("SELECT * FROM search_queries ORDER BY count DESC, lastSearchedAt DESC LIMIT 30")
    fun observeTop(): Flow<List<SearchQueryEntity>>

    @Query("SELECT * FROM search_queries WHERE query = :query")
    suspend fun byQuery(query: String): SearchQueryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: SearchQueryEntity)

    @Query("DELETE FROM search_queries WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_queries")
    suspend fun clear()
}

@Dao
interface UserScriptDao {
    @Query("SELECT * FROM user_scripts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<UserScriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(script: UserScriptEntity)

    @Query("DELETE FROM user_scripts WHERE id = :id")
    suspend fun delete(id: String)
}

class SearchRepository(private val dao: SearchQueryDao) {
    val topSearches: Flow<List<SearchQuery>> = dao.observeTop().map { list -> list.map(SearchQueryEntity::toModel) }
    suspend fun record(rawQuery: String) {
        val query = rawQuery.trim().take(200)
        if (query.isBlank()) return
        val existing = dao.byQuery(query)
        dao.save(SearchQueryEntity(query, (existing?.count ?: 0) + 1, System.currentTimeMillis()))
    }
    suspend fun delete(query: String) = dao.delete(query)
    suspend fun clear() = dao.clear()
}

class UserScriptRepository(private val dao: UserScriptDao) {
    val scripts: Flow<List<UserScript>> = dao.observeAll().map { list -> list.map(UserScriptEntity::toModel) }
    suspend fun save(script: UserScript) = dao.save(script.toEntity())
    suspend fun delete(id: String) = dao.delete(id)
}
