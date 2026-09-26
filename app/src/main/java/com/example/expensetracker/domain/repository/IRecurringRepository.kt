package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface IRecurringRepository {
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun getDueReminders(date: Date = Date()): List<RecurringTransaction>
    suspend fun getRecurringById(id: Long): RecurringTransaction?
    suspend fun createSalaryReminder(
        amount: Double,
        accountType: AccountType,
        salaryCategoryId: Long,
        dayOfMonth: Int,
        description: String = "Salary"
    ): Long
    suspend fun advanceRecurringDueDate(recurring: RecurringTransaction)
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun deactivateRecurring(id: Long)
    suspend fun setRecurringActive(id: Long, isActive: Boolean)
    suspend fun deleteRecurringById(id: Long)
    suspend fun updateRecurring(recurring: RecurringTransaction)
    suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long
}
