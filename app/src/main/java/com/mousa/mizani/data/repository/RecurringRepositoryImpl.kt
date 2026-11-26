package com.mousa.mizani.data.repository

import com.mousa.mizani.data.database.dao.RecurringTransactionDao
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.domain.calculator.advanced
import com.mousa.mizani.domain.repository.IRecurringRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao
) : IRecurringRepository {

    override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getActiveRecurringTransactions()
    }

    override suspend fun getDueReminders(date: Date): List<RecurringTransaction> {
        return recurringTransactionDao.getDueRecurringTransactions(date)
    }

    override suspend fun getRecurringById(id: Long): RecurringTransaction? {
        return recurringTransactionDao.getRecurringTransactionById(id)
    }

    override suspend fun createSalaryReminder(
        amount: Double,
        accountType: AccountType,
        salaryCategoryId: Long,
        dayOfMonth: Int,
        description: String
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
                nextDueDate = nextDue,
                anchorDay = dayOfMonth
            )
        )
    }

    override suspend fun advanceRecurringDueDate(recurring: RecurringTransaction) {
        recurringTransactionDao.updateRecurringTransaction(recurring.advanced())
    }

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRecurringTransactions()
    }

    override suspend fun deactivateRecurring(id: Long) {
        recurringTransactionDao.deactivateRecurringTransaction(id)
    }

    override suspend fun setRecurringActive(id: Long, isActive: Boolean) {
        recurringTransactionDao.setRecurringActive(id, isActive)
    }

    override suspend fun deleteRecurringById(id: Long) {
        recurringTransactionDao.deleteRecurringTransactionById(id)
    }

    override suspend fun updateRecurring(recurring: RecurringTransaction) {
        recurringTransactionDao.updateRecurringTransaction(recurring)
    }

    override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long {
        return recurringTransactionDao.insertRecurringTransaction(recurring)
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

typealias RecurringRepository = RecurringRepositoryImpl
