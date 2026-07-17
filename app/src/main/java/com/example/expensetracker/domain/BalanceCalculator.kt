package com.example.expensetracker.domain

import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
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

/**
 * All month-summary numbers computed from the same transaction list so income, expense,
 * net, and per-account breakdown always agree.
 */
data class MonthFinancialSummary(
    val periodIncome: Double,
    val periodExpense: Double,
    val periodNet: Double,
    /** Net wallet change per account this period (income − expense ± transfers). */
    val periodChangeByAccount: AccountBalances,
    /** Income − expense per account only; transfers excluded. */
    val incomeExpenseByAccount: AccountBalances,
    /** Net transfer movement per account; cash + bank always equals zero. */
    val transferImpact: AccountBalances,
    /** Cash/bank available right now — same formula as the Add expense/income screen. */
    val currentBalances: AccountBalances
) {
    val hasTransfers: Boolean get() =
        transferImpact.cash != 0.0 || transferImpact.bank != 0.0
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

    /**
     * Returns income minus expense per account for the given period, excluding transfers.
     * Transfers are internal moves between accounts and do not represent real earning/spending,
     * so they should not appear in a "how did income/expense affect each account?" view.
     *
     * Example: if income 409 (cash) and expense 264.5 (cash), result is cash +144.5, bank 0.
     * A bank→cash transfer does NOT affect this number, making it much easier to read.
     */
    suspend fun getPeriodIncomeExpenseByAccount(startDate: Date, endDate: Date): AccountBalances {
        return buildMonthFinancialSummary(startDate, endDate).incomeExpenseByAccount
    }

    suspend fun getPeriodTransferImpact(startDate: Date, endDate: Date): AccountBalances {
        return buildMonthFinancialSummary(startDate, endDate).transferImpact
    }

    /**
     * Single source of truth for the History month summary.
     * All period figures are derived from one pass over the same transactions.
     */
    suspend fun buildMonthFinancialSummary(startDate: Date, endDate: Date): MonthFinancialSummary {
        val transactions = transactionDao.getTransactionsBetweenDatesSnapshot(startDate, endDate)
        return computeMonthFinancialSummary(transactions, getAllTimeBalances())
    }

    internal fun computeMonthFinancialSummary(
        periodTransactions: List<Transaction>,
        currentBalances: AccountBalances
    ): MonthFinancialSummary {
        var periodIncome = 0.0
        var periodExpense = 0.0
        var cashIncomeExpense = 0.0
        var bankIncomeExpense = 0.0
        var cashTransferImpact = 0.0
        var bankTransferImpact = 0.0

        for (transaction in periodTransactions) {
            when (transaction.type) {
                TransactionType.INCOME -> {
                    periodIncome += transaction.amount
                    when (transaction.accountType) {
                        AccountType.CASH -> cashIncomeExpense += transaction.amount
                        AccountType.BANK -> bankIncomeExpense += transaction.amount
                        AccountType.WALLET -> Unit
                    }
                }
                TransactionType.EXPENSE -> {
                    periodExpense += transaction.amount
                    when (transaction.accountType) {
                        AccountType.CASH -> cashIncomeExpense -= transaction.amount
                        AccountType.BANK -> bankIncomeExpense -= transaction.amount
                        AccountType.WALLET -> Unit
                    }
                }
                TransactionType.TRANSFER -> {
                    when (transaction.accountType) {
                        AccountType.CASH -> cashTransferImpact -= transaction.amount
                        AccountType.BANK -> bankTransferImpact -= transaction.amount
                        AccountType.WALLET -> Unit
                    }
                    when (transaction.toAccountType) {
                        AccountType.CASH -> cashTransferImpact += transaction.amount
                        AccountType.BANK -> bankTransferImpact += transaction.amount
                        AccountType.WALLET, null -> Unit
                    }
                }
                TransactionType.WALLET_MOVE -> {
                    when (transaction.accountType) {
                        AccountType.CASH -> cashTransferImpact -= transaction.amount
                        AccountType.BANK -> bankTransferImpact -= transaction.amount
                        AccountType.WALLET -> Unit
                    }
                    when (transaction.toAccountType) {
                        AccountType.CASH -> cashTransferImpact += transaction.amount
                        AccountType.BANK -> bankTransferImpact += transaction.amount
                        AccountType.WALLET, null -> Unit
                    }
                }
            }
        }

        val incomeExpenseByAccount = AccountBalances(cashIncomeExpense, bankIncomeExpense)
        val transferImpact = AccountBalances(cashTransferImpact, bankTransferImpact)
        val periodChangeByAccount = AccountBalances(
            cash = cashIncomeExpense + cashTransferImpact,
            bank = bankIncomeExpense + bankTransferImpact
        )
        val periodNet = periodIncome - periodExpense

        return MonthFinancialSummary(
            periodIncome = periodIncome,
            periodExpense = periodExpense,
            periodNet = periodNet,
            periodChangeByAccount = periodChangeByAccount,
            incomeExpenseByAccount = incomeExpenseByAccount,
            transferImpact = transferImpact,
            currentBalances = currentBalances
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
        if (account == AccountType.WALLET) return
        val currentComputed = computeBalance(account)
        val currentOpening = getOpeningBalance(account)
        val newOpening = currentOpening + (target - currentComputed)
        when (account) {
            AccountType.CASH -> userPreferences.setOpeningCashBalance(newOpening)
            AccountType.BANK -> userPreferences.setOpeningBankBalance(newOpening)
            AccountType.WALLET -> Unit
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
        val walletOut = transactionDao.getTotalWalletMoveOutExcluding(account, excludeTransactionId)
        val walletIn = transactionDao.getTotalWalletMoveInExcluding(account, excludeTransactionId)
        return opening + income - expense - transferOut + transferIn - walletOut + walletIn
    }

    private suspend fun computeBalance(account: AccountType): Double {
        if (account == AccountType.WALLET) return 0.0
        val opening = getOpeningBalance(account)
        val income = transactionDao.getTotalIncomeForAccount(account)
        val expense = transactionDao.getTotalExpenseForAccount(account)
        val transferOut = transactionDao.getTotalTransferOut(account)
        val transferIn = transactionDao.getTotalTransferIn(account)
        val walletOut = transactionDao.getTotalWalletMoveOut(account)
        val walletIn = transactionDao.getTotalWalletMoveIn(account)
        return opening + income - expense - transferOut + transferIn - walletOut + walletIn
    }

    private suspend fun getOpeningBalance(account: AccountType): Double {
        return when (account) {
            AccountType.CASH -> userPreferences.getOpeningCashBalance()
            AccountType.BANK -> userPreferences.getOpeningBankBalance()
            AccountType.WALLET -> 0.0
        }
    }

    private suspend fun computePeriodChange(
        account: AccountType,
        startDate: Date,
        endDate: Date
    ): Double {
        if (account == AccountType.WALLET) return 0.0
        val income = transactionDao.getPeriodIncomeForAccount(account, startDate, endDate)
        val expense = transactionDao.getPeriodExpenseForAccount(account, startDate, endDate)
        val transferOut = transactionDao.getPeriodTransferOut(account, startDate, endDate)
        val transferIn = transactionDao.getPeriodTransferIn(account, startDate, endDate)
        return income - expense - transferOut + transferIn
    }
}
