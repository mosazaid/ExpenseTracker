package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : ITransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    override suspend fun getAllTransactionsSnapshot(): List<Transaction> {
        return getAllTransactions().first()
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    override fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
    }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId)
    }

    override suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Date,
        endDate: Date
    ): Double {
        return transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    }

    override suspend fun getTotalAmountByCategoryAndDateRange(
        categoryId: Long,
        startDate: Date,
        endDate: Date
    ): Double {
        return transactionDao.getTotalAmountByCategoryAndDateRange(categoryId, startDate, endDate) ?: 0.0
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.copy(updatedAt = Date()))
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    override suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun getUnreimbursedExpenses(startDate: Date, endDate: Date): List<Transaction> {
        return transactionDao.getUnreimbursedExpenses(startDate, endDate)
    }

    override suspend fun hasLinkedDeptIncome(expenseId: Long): Boolean {
        return transactionDao.hasLinkedDeptIncome(expenseId)
    }

    override suspend fun getDeptIncomesLinkedTo(expenseId: Long): List<Transaction> {
        return transactionDao.getDeptIncomesLinkedTo(expenseId)
    }

    override suspend fun getExpenseCountBetweenDates(startDate: Date, endDate: Date): Int {
        return transactionDao.getExpenseCountBetweenDates(startDate, endDate)
    }

    override fun getOutstandingLentDebtsFlow(): Flow<List<Transaction>> {
        return transactionDao.getOutstandingLentDebtsFlow()
    }

    override fun getOutstandingBorrowedDebtsFlow(): Flow<List<Transaction>> {
        return transactionDao.getOutstandingBorrowedDebtsFlow()
    }

    override fun getAllOutstandingDebtsFlow(): Flow<List<Transaction>> {
        return transactionDao.getAllOutstandingDebtsFlow()
    }

    override suspend fun getAllOutstandingDebtsSnapshot(): List<Transaction> {
        return transactionDao.getAllOutstandingDebtsSnapshot()
    }

    override suspend fun setDebtSettled(id: Long, settled: Boolean) {
        transactionDao.setDebtSettled(id, settled)
    }
}

typealias TransactionRepository = TransactionRepositoryImpl
