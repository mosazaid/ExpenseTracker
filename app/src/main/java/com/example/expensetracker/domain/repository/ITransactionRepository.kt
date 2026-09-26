package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.AccountType
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
    suspend fun getTransactionsBetweenDatesSnapshot(startDate: Date, endDate: Date): List<Transaction>
    suspend fun getTotalIncomeForAccount(account: AccountType): Double
    suspend fun getTotalExpenseForAccount(account: AccountType): Double
    suspend fun getTotalTransferOut(account: AccountType): Double
    suspend fun getTotalTransferIn(account: AccountType): Double
    suspend fun getTotalWalletMoveOut(account: AccountType): Double
    suspend fun getTotalWalletMoveIn(account: AccountType): Double
    suspend fun getTotalIncomeForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double
    suspend fun getTotalExpenseForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double
    suspend fun getTotalTransferOutExcluding(account: AccountType, excludeTransactionId: Long): Double
    suspend fun getTotalTransferInExcluding(account: AccountType, excludeTransactionId: Long): Double
    suspend fun getTotalWalletMoveOutExcluding(account: AccountType, excludeTransactionId: Long): Double
    suspend fun getTotalWalletMoveInExcluding(account: AccountType, excludeTransactionId: Long): Double
    suspend fun getPeriodIncomeForAccount(account: AccountType, startDate: Date, endDate: Date): Double
    suspend fun getPeriodExpenseForAccount(account: AccountType, startDate: Date, endDate: Date): Double
    suspend fun getPeriodTransferOut(account: AccountType, startDate: Date, endDate: Date): Double
    suspend fun getPeriodTransferIn(account: AccountType, startDate: Date, endDate: Date): Double
    suspend fun getPeriodWalletIn(startDate: Date, endDate: Date): Double
    suspend fun getPeriodWalletOut(startDate: Date, endDate: Date): Double
    suspend fun getSalaryPeriodAnchors(salaryCategoryId: Long): List<Transaction>
    suspend fun getLatestSalaryIncomeBefore(beforeDate: Date, salaryCategoryId: Long): Transaction?
    suspend fun setDebtSettled(id: Long, settled: Boolean)
    fun getTransactionsForRecurringFlow(
        recurringId: Long,
        description: String,
        type: TransactionType
    ): Flow<List<Transaction>>
}
