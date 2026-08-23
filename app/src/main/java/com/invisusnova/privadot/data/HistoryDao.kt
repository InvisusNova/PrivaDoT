package com.invisusnova.privadot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM history_log ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    @Query("DELETE FROM history_log")
    suspend fun clearHistory()
}
