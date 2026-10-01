package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.TransactionSplit
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionSplitDao {

    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId")
    fun getSplitsForTransaction(transactionId: Long): Flow<List<TransactionSplit>>

    @Query("SELECT * FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun getSplitsForTransactionSnapshot(transactionId: Long): List<TransactionSplit>

    @Query("SELECT * FROM transaction_splits WHERE isDebt = 1 AND isDebtSettled = 0")
    fun getUnsettledDebtSplits(): Flow<List<TransactionSplit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplit(split: TransactionSplit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<TransactionSplit>): List<Long>

    @Update
    suspend fun updateSplit(split: TransactionSplit)

    @Delete
    suspend fun deleteSplit(split: TransactionSplit)

    @Query("DELETE FROM transaction_splits WHERE transactionId = :transactionId")
    suspend fun deleteSplitsForTransaction(transactionId: Long)

    @Query("UPDATE transaction_splits SET isDebtSettled = :settled WHERE id = :splitId")
    suspend fun setDebtSettled(splitId: Long, settled: Boolean)
}
