package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguredLoanDao {

    @Query("SELECT * FROM configured_loans ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<ConfiguredLoan>>

    @Query("SELECT * FROM configured_loans WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveLoans(): List<ConfiguredLoan>

    @Query("SELECT * FROM configured_loans WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveLoansFlow(): Flow<List<ConfiguredLoan>>

    @Query("SELECT * FROM configured_loans WHERE id = :id")
    suspend fun getLoanById(id: Long): ConfiguredLoan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: ConfiguredLoan): Long

    @Update
    suspend fun updateLoan(loan: ConfiguredLoan)

    @Delete
    suspend fun deleteLoan(loan: ConfiguredLoan)

    @Query("DELETE FROM configured_loans WHERE id = :id")
    suspend fun deleteLoanById(id: Long)
}
