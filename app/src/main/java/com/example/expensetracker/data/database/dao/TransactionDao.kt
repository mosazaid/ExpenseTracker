package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsBetweenDatesSnapshot(startDate: Date, endDate: Date): List<Transaction>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>

    @Query(
        "SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate"
    )
    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Date,
        endDate: Date
    ): Double?

    @Query(
        "SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate"
    )
    suspend fun getTotalAmountByCategoryAndDateRange(
        categoryId: Long,
        startDate: Date,
        endDate: Date
    ): Double?

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME' AND accountType = :account
        """
    )
    suspend fun getTotalIncomeForAccount(account: AccountType): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE' AND accountType = :account
        """
    )
    suspend fun getTotalExpenseForAccount(account: AccountType): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER' AND accountType = :account
        """
    )
    suspend fun getTotalTransferOut(account: AccountType): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER' AND toAccountType = :account
        """
    )
    suspend fun getTotalTransferIn(account: AccountType): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME' AND accountType = :account
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getPeriodIncomeForAccount(
        account: AccountType,
        startDate: Date,
        endDate: Date
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE' AND accountType = :account
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getPeriodExpenseForAccount(
        account: AccountType,
        startDate: Date,
        endDate: Date
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER' AND accountType = :account
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getPeriodTransferOut(
        account: AccountType,
        startDate: Date,
        endDate: Date
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER' AND toAccountType = :account
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getPeriodTransferIn(
        account: AccountType,
        startDate: Date,
        endDate: Date
    ): Double

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND accountType = 'BANK'
        AND startsNewPeriod = 1 AND categoryId = :salaryCategoryId
        AND date <= :beforeDate
        ORDER BY date DESC LIMIT 1
        """
    )
    suspend fun getLatestSalaryPeriodStart(
        beforeDate: Date,
        salaryCategoryId: Long
    ): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND accountType = 'BANK'
        AND startsNewPeriod = 1 AND categoryId = :salaryCategoryId
        AND date > :afterDate
        ORDER BY date ASC LIMIT 1
        """
    )
    suspend fun getNextSalaryPeriodStart(
        afterDate: Date,
        salaryCategoryId: Long
    ): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND accountType = 'BANK' AND categoryId = :salaryCategoryId
        AND date <= :beforeDate
        ORDER BY date DESC LIMIT 1
        """
    )
    suspend fun getLatestBankSalaryIncome(
        beforeDate: Date,
        salaryCategoryId: Long
    ): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND accountType = 'BANK' AND categoryId = :salaryCategoryId
        AND date > :afterDate
        ORDER BY date ASC LIMIT 1
        """
    )
    suspend fun getNextBankSalaryIncome(
        afterDate: Date,
        salaryCategoryId: Long
    ): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND categoryId = :salaryCategoryId
        AND startsNewPeriod = 1
        ORDER BY date ASC
        """
    )
    suspend fun getSalaryPeriodAnchors(salaryCategoryId: Long): List<Transaction>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND categoryId = :salaryCategoryId
        ORDER BY date ASC
        """
    )
    suspend fun getAllSalaryIncomes(salaryCategoryId: Long): List<Transaction>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'INCOME' AND categoryId = :salaryCategoryId
        AND date <= :beforeDate
        ORDER BY date DESC LIMIT 1
        """
    )
    suspend fun getLatestSalaryIncomeBefore(
        beforeDate: Date,
        salaryCategoryId: Long
    ): Transaction?

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'EXPENSE'
          AND awaitingReimbursement = 1
          AND date BETWEEN :startDate AND :endDate
          AND id NOT IN (
              SELECT linkedExpenseId FROM transactions
              WHERE linkedExpenseId IS NOT NULL
          )
        ORDER BY date DESC
        """
    )
    suspend fun getUnreimbursedExpenses(startDate: Date, endDate: Date): List<Transaction>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE linkedExpenseId = :expenseId
        )
        """
    )
    suspend fun hasLinkedDeptIncome(expenseId: Long): Boolean

    @Query(
        """
        SELECT * FROM transactions
        WHERE linkedExpenseId = :expenseId
        ORDER BY date DESC
        """
    )
    suspend fun getDeptIncomesLinkedTo(expenseId: Long): List<Transaction>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'INCOME' AND accountType = :account
          AND id != :excludeId
        """
    )
    suspend fun getTotalIncomeForAccountExcluding(account: AccountType, excludeId: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'EXPENSE' AND accountType = :account
          AND id != :excludeId
        """
    )
    suspend fun getTotalExpenseForAccountExcluding(account: AccountType, excludeId: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER' AND accountType = :account
          AND id != :excludeId
        """
    )
    suspend fun getTotalTransferOutExcluding(account: AccountType, excludeId: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'TRANSFER' AND toAccountType = :account
          AND id != :excludeId
        """
    )
    suspend fun getTotalTransferInExcluding(account: AccountType, excludeId: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'WALLET_MOVE' AND accountType = :account
        """
    )
    suspend fun getTotalWalletMoveOut(account: AccountType): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'WALLET_MOVE' AND toAccountType = :account
        """
    )
    suspend fun getTotalWalletMoveIn(account: AccountType): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'WALLET_MOVE' AND toAccountType = 'WALLET'
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getPeriodWalletIn(startDate: Date, endDate: Date): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'WALLET_MOVE' AND accountType = 'WALLET'
        AND date BETWEEN :startDate AND :endDate
        """
    )
    suspend fun getPeriodWalletOut(startDate: Date, endDate: Date): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'WALLET_MOVE' AND accountType = :account
          AND id != :excludeId
        """
    )
    suspend fun getTotalWalletMoveOutExcluding(account: AccountType, excludeId: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'WALLET_MOVE' AND toAccountType = :account
          AND id != :excludeId
        """
    )
    suspend fun getTotalWalletMoveInExcluding(account: AccountType, excludeId: Long): Double

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
}
