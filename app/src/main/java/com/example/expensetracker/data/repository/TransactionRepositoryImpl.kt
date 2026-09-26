package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.AccountType
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
    private val transactionDao: TransactionDao,
    private val monthlyLoanPaymentDao: MonthlyLoanPaymentDao? = null
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
        monthlyLoanPaymentDao?.unmarkPaymentByTransactionId(transaction.id)
        transactionDao.deleteTransaction(transaction)
    }

    override suspend fun deleteTransactionById(id: Long) {
        monthlyLoanPaymentDao?.unmarkPaymentByTransactionId(id)
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

    override suspend fun getTransactionsBetweenDatesSnapshot(startDate: Date, endDate: Date): List<Transaction> {
        return transactionDao.getTransactionsBetweenDatesSnapshot(startDate, endDate)
    }

    override suspend fun getTotalIncomeForAccount(account: AccountType): Double {
        return transactionDao.getTotalIncomeForAccount(account)
    }

    override suspend fun getTotalExpenseForAccount(account: AccountType): Double {
        return transactionDao.getTotalExpenseForAccount(account)
    }

    override suspend fun getTotalTransferOut(account: AccountType): Double {
        return transactionDao.getTotalTransferOut(account)
    }

    override suspend fun getTotalTransferIn(account: AccountType): Double {
        return transactionDao.getTotalTransferIn(account)
    }

    override suspend fun getTotalWalletMoveOut(account: AccountType): Double {
        return transactionDao.getTotalWalletMoveOut(account)
    }

    override suspend fun getTotalWalletMoveIn(account: AccountType): Double {
        return transactionDao.getTotalWalletMoveIn(account)
    }

    override suspend fun getTotalIncomeForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double {
        return transactionDao.getTotalIncomeForAccountExcluding(account, excludeTransactionId)
    }

    override suspend fun getTotalExpenseForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double {
        return transactionDao.getTotalExpenseForAccountExcluding(account, excludeTransactionId)
    }

    override suspend fun getTotalTransferOutExcluding(account: AccountType, excludeTransactionId: Long): Double {
        return transactionDao.getTotalTransferOutExcluding(account, excludeTransactionId)
    }

    override suspend fun getTotalTransferInExcluding(account: AccountType, excludeTransactionId: Long): Double {
        return transactionDao.getTotalTransferInExcluding(account, excludeTransactionId)
    }

    override suspend fun getTotalWalletMoveOutExcluding(account: AccountType, excludeTransactionId: Long): Double {
        return transactionDao.getTotalWalletMoveOutExcluding(account, excludeTransactionId)
    }

    override suspend fun getTotalWalletMoveInExcluding(account: AccountType, excludeTransactionId: Long): Double {
        return transactionDao.getTotalWalletMoveInExcluding(account, excludeTransactionId)
    }

    override suspend fun getPeriodIncomeForAccount(account: AccountType, startDate: Date, endDate: Date): Double {
        return transactionDao.getPeriodIncomeForAccount(account, startDate, endDate)
    }

    override suspend fun getPeriodExpenseForAccount(account: AccountType, startDate: Date, endDate: Date): Double {
        return transactionDao.getPeriodExpenseForAccount(account, startDate, endDate)
    }

    override suspend fun getPeriodTransferOut(account: AccountType, startDate: Date, endDate: Date): Double {
        return transactionDao.getPeriodTransferOut(account, startDate, endDate)
    }

    override suspend fun getPeriodTransferIn(account: AccountType, startDate: Date, endDate: Date): Double {
        return transactionDao.getPeriodTransferIn(account, startDate, endDate)
    }

    override suspend fun getPeriodWalletIn(startDate: Date, endDate: Date): Double {
        return transactionDao.getPeriodWalletIn(startDate, endDate)
    }

    override suspend fun getPeriodWalletOut(startDate: Date, endDate: Date): Double {
        return transactionDao.getPeriodWalletOut(startDate, endDate)
    }

    override suspend fun getSalaryPeriodAnchors(salaryCategoryId: Long): List<Transaction> {
        return transactionDao.getSalaryPeriodAnchors(salaryCategoryId)
    }

    override suspend fun getLatestSalaryIncomeBefore(beforeDate: Date, salaryCategoryId: Long): Transaction? {
        return transactionDao.getLatestSalaryIncomeBefore(beforeDate, salaryCategoryId)
    }

    override fun getTransactionsForRecurringFlow(
        recurringId: Long,
        description: String,
        type: TransactionType
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForRecurringFlow(recurringId, description, type)
    }
}

typealias TransactionRepository = TransactionRepositoryImpl
