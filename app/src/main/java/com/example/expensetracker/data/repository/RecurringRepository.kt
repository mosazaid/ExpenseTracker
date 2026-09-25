package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.RecurringTransactionDao
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.RecurrenceFrequency
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao
) {

    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getActiveRecurringTransactions()
    }

    suspend fun getDueReminders(date: Date = Date()): List<RecurringTransaction> {
        return recurringTransactionDao.getDueRecurringTransactions(date)
    }

    suspend fun getRecurringById(id: Long): RecurringTransaction? {
        return recurringTransactionDao.getRecurringTransactionById(id)
    }

    suspend fun createSalaryReminder(
        amount: Double,
        accountType: AccountType,
        salaryCategoryId: Long,
        dayOfMonth: Int,
        description: String = "Salary"
    ): Long {
        val nextDue = computeNextDueDate(dayOfMonth)
        return recurringTransactionDao.insertRecurringTransaction(
            RecurringTransaction(
                amount = amount,
                description = description,
                type = TransactionType.INCOME,
                categoryId = salaryCategoryId,
                accountType = accountType,
                frequency = RecurrenceFrequency.MONTHLY,
                nextDueDate = nextDue
            )
        )
    }

    suspend fun advanceRecurringDueDate(recurring: RecurringTransaction) {
        val calendar = Calendar.getInstance()
        calendar.time = recurring.nextDueDate
        calendar.add(Calendar.MONTH, 1)
        recurringTransactionDao.updateRecurringTransaction(
            recurring.copy(nextDueDate = calendar.time)
        )
    }

    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRecurringTransactions()
    }

    suspend fun deactivateRecurring(id: Long) {
        recurringTransactionDao.deactivateRecurringTransaction(id)
    }

    suspend fun setRecurringActive(id: Long, isActive: Boolean) {
        recurringTransactionDao.setRecurringActive(id, isActive)
    }

    suspend fun deleteRecurringById(id: Long) {
        recurringTransactionDao.deleteRecurringTransactionById(id)
    }

    suspend fun updateRecurring(recurring: RecurringTransaction) {
        recurringTransactionDao.updateRecurringTransaction(recurring)
    }

    private fun computeNextDueDate(dayOfMonth: Int): Date {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        if (today >= dayOfMonth) {
            calendar.add(Calendar.MONTH, 1)
        }
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}
