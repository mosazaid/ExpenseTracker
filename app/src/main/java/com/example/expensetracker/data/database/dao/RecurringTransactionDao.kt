package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE nextDueDate <= :date AND isActive = 1")
    suspend fun getDueRecurringTransactions(date: Date): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getRecurringTransactionById(id: Long): RecurringTransaction?

    @Insert
    suspend fun insertRecurringTransaction(transaction: RecurringTransaction): Long

    @Update
    suspend fun updateRecurringTransaction(transaction: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(transaction: RecurringTransaction)

    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, nextDueDate ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("UPDATE recurring_transactions SET isActive = 0 WHERE id = :id")
    suspend fun deactivateRecurringTransaction(id: Long)

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun setRecurringActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: Long)
}
