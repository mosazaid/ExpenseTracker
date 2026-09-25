package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface ITransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getAllTransactionsSnapshot(): List<Transaction>
    suspend fun getTransactionById(id: Long): Transaction?
    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>>
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double
    suspend fun getTotalAmountByCategoryAndDateRange(categoryId: Long, startDate: Date, endDate: Date): Double
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(id: Long)
    suspend fun getUnreimbursedExpenses(startDate: Date, endDate: Date): List<Transaction>
    suspend fun hasLinkedDeptIncome(expenseId: Long): Boolean
    suspend fun getDeptIncomesLinkedTo(expenseId: Long): List<Transaction>
    suspend fun getExpenseCountBetweenDates(startDate: Date, endDate: Date): Int
    fun getOutstandingLentDebtsFlow(): Flow<List<Transaction>>
    fun getOutstandingBorrowedDebtsFlow(): Flow<List<Transaction>>
    fun getAllOutstandingDebtsFlow(): Flow<List<Transaction>>
    suspend fun getAllOutstandingDebtsSnapshot(): List<Transaction>
    suspend fun setDebtSettled(id: Long, settled: Boolean)
}
