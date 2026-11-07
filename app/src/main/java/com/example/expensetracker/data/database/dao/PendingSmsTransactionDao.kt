package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expensetracker.data.database.entities.PendingSmsTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSmsTransactionDao {

    @Query("SELECT * FROM pending_sms_transactions WHERE isProcessed = 0 AND isDismissed = 0 ORDER BY timestamp DESC")
    fun getPendingSms(): Flow<List<PendingSmsTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSms(item: PendingSmsTransaction): Long

    @Query("UPDATE pending_sms_transactions SET isProcessed = 1 WHERE id = :id")
    suspend fun markProcessed(id: Long)

    @Query("UPDATE pending_sms_transactions SET isDismissed = 1 WHERE id = :id")
    suspend fun markDismissed(id: Long)

    @Query("DELETE FROM pending_sms_transactions WHERE id = :id")
    suspend fun deletePendingSms(id: Long)
}

