package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM processing_history ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM processing_history ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 5): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM processing_history WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity): Long

    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE processing_history SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM processing_history")
    suspend fun clearAll()
}
