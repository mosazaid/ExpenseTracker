package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId)
    }

    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Date,
        endDate: Date
    ): Double {
        return transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    }

    suspend fun getTotalAmountByCategoryAndDateRange(
        categoryId: Long,
        startDate: Date,
        endDate: Date
    ): Double {
        return transactionDao.getTotalAmountByCategoryAndDateRange(categoryId, startDate, endDate) ?: 0.0
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = Date()))
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
}
