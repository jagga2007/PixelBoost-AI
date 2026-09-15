package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM enhancement_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM enhancement_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 5): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM enhancement_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM enhancement_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM enhancement_history")
    suspend fun clearAll()
}
