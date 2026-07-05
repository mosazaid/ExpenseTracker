package com.example.expensetracker.domain

import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.preferences.UserPreferences
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class AccountBalances(
    val cash: Double,
    val bank: Double
) {
    val total: Double get() = cash + bank
}

@Singleton
class BalanceCalculator @Inject constructor(
    private val transactionDao: TransactionDao,
    private val userPreferences: UserPreferences
) {

    suspend fun getAllTimeBalances(): AccountBalances {
        return AccountBalances(
            cash = computeBalance(AccountType.CASH),
            bank = computeBalance(AccountType.BANK)
        )
    }

    suspend fun getPeriodChange(startDate: Date, endDate: Date): AccountBalances {
        return AccountBalances(
            cash = computePeriodChange(AccountType.CASH, startDate, endDate),
            bank = computePeriodChange(AccountType.BANK, startDate, endDate)
        )
    }

    suspend fun getAvailableBalance(account: AccountType): Double {
        return computeBalance(account)
    }

    /**
     * Recalibrates the account so its computed available balance becomes exactly [target],
     * regardless of the net effect of previously recorded transactions. This is what
     * "set current balance" in Settings should call -- setting the raw opening balance
     * directly would instead add [target] on top of all historical income/expense, which
     * rarely matches the real-world balance the user is trying to enter.
     */
    suspend fun setCurrentBalance(account: AccountType, target: Double) {
        val currentComputed = computeBalance(account)
        val currentOpening = getOpeningBalance(account)
        val newOpening = currentOpening + (target - currentComputed)
        when (account) {
            AccountType.CASH -> userPreferences.setOpeningCashBalance(newOpening)
            AccountType.BANK -> userPreferences.setOpeningBankBalance(newOpening)
        }
    }

    suspend fun getAvailableBalanceExcluding(
        account: AccountType,
        excludeTransactionId: Long
    ): Double {
        val opening = getOpeningBalance(account)
        val income = transactionDao.getTotalIncomeForAccountExcluding(account, excludeTransactionId)
        val expense = transactionDao.getTotalExpenseForAccountExcluding(account, excludeTransactionId)
        val transferOut = transactionDao.getTotalTransferOutExcluding(account, excludeTransactionId)
        val transferIn = transactionDao.getTotalTransferInExcluding(account, excludeTransactionId)
        return opening + income - expense - transferOut + transferIn
    }

    private suspend fun computeBalance(account: AccountType): Double {
        val opening = getOpeningBalance(account)
        val income = transactionDao.getTotalIncomeForAccount(account)
        val expense = transactionDao.getTotalExpenseForAccount(account)
        val transferOut = transactionDao.getTotalTransferOut(account)
        val transferIn = transactionDao.getTotalTransferIn(account)
        return opening + income - expense - transferOut + transferIn
    }

    private suspend fun getOpeningBalance(account: AccountType): Double {
        return when (account) {
            AccountType.CASH -> userPreferences.getOpeningCashBalance()
            AccountType.BANK -> userPreferences.getOpeningBankBalance()
        }
    }

    private suspend fun computePeriodChange(
        account: AccountType,
        startDate: Date,
        endDate: Date
    ): Double {
        val income = transactionDao.getPeriodIncomeForAccount(account, startDate, endDate)
        val expense = transactionDao.getPeriodExpenseForAccount(account, startDate, endDate)
        val transferOut = transactionDao.getPeriodTransferOut(account, startDate, endDate)
        val transferIn = transactionDao.getPeriodTransferIn(account, startDate, endDate)
        return income - expense - transferOut + transferIn
    }
}
