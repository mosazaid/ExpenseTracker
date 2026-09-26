package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.ITransactionRepository
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
    val currentBalances: AccountBalances,
    val periodLoansDeducted: Double = 0.0,
    val totalPaidLoans: Double = 0.0,
    val paidLoans: List<com.example.expensetracker.data.database.dao.PaidLoanInfo> = emptyList()
) {
    val hasTransfers: Boolean get() =
        transferImpact.cash != 0.0 || transferImpact.bank != 0.0
}

@Singleton
class BalanceCalculator @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val userPreferences: UserPreferences,
    private val monthlyLoanPaymentDao: com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao? = null
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
        val transactions = transactionRepository.getTransactionsBetweenDatesSnapshot(startDate, endDate)
        val paidLoans = monthlyLoanPaymentDao?.getPaidLoansBetweenDates(startDate, endDate) ?: emptyList()
        return computeMonthFinancialSummary(transactions, getAllTimeBalances(), paidLoans)
    }

    internal fun computeMonthFinancialSummary(
        periodTransactions: List<Transaction>,
        currentBalances: AccountBalances,
        paidLoans: List<com.example.expensetracker.data.database.dao.PaidLoanInfo> = emptyList()
    ): MonthFinancialSummary {
        var rawIncome = 0.0
        var periodExpense = 0.0
        var cashIncome = 0.0
        var bankIncome = 0.0
        var cashExpense = 0.0
        var bankExpense = 0.0
        var cashTransferImpact = 0.0
        var bankTransferImpact = 0.0

        val loansDeductedFromIncome = paidLoans.filter { it.deductFromIncome }
        val loansDeductedTxnIds = loansDeductedFromIncome.mapNotNull { it.transactionId }.toSet()

        for (transaction in periodTransactions) {
            when (transaction.type) {
                TransactionType.INCOME -> {
                    rawIncome += transaction.amount
                    when (transaction.accountType) {
                        AccountType.CASH -> cashIncome += transaction.amount
                        AccountType.BANK -> bankIncome += transaction.amount
                        AccountType.WALLET -> Unit
                    }
                }
                TransactionType.EXPENSE -> {
                    if (transaction.id in loansDeductedTxnIds) {
                        // Loan payment is deducted directly from income and excluded from monthly expenses
                    } else {
                        periodExpense += transaction.amount
                        when (transaction.accountType) {
                            AccountType.CASH -> cashExpense += transaction.amount
                            AccountType.BANK -> bankExpense += transaction.amount
                            AccountType.WALLET -> Unit
                        }
                    }
                }
                TransactionType.TRANSFER, TransactionType.WALLET_MOVE -> {
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

        val totalLoansDeducted = loansDeductedFromIncome.sumOf { it.amount }
        val cashLoanDeductions = loansDeductedFromIncome.filter { it.accountType == AccountType.CASH }.sumOf { it.amount }
        val bankLoanDeductions = loansDeductedFromIncome.filter { it.accountType == AccountType.BANK }.sumOf { it.amount }
        val totalPaidLoans = paidLoans.sumOf { it.amount }

        val periodIncome = (rawIncome - totalLoansDeducted).coerceAtLeast(0.0)
        val adjustedCashIncome = (cashIncome - cashLoanDeductions).coerceAtLeast(0.0)
        val adjustedBankIncome = (bankIncome - bankLoanDeductions).coerceAtLeast(0.0)

        val cashIncomeExpense = adjustedCashIncome - cashExpense
        val bankIncomeExpense = adjustedBankIncome - bankExpense

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
            currentBalances = currentBalances,
            periodLoansDeducted = totalLoansDeducted,
            totalPaidLoans = totalPaidLoans,
            paidLoans = paidLoans
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
        val income = transactionRepository.getTotalIncomeForAccountExcluding(account, excludeTransactionId)
        val expense = transactionRepository.getTotalExpenseForAccountExcluding(account, excludeTransactionId)
        val transferOut = transactionRepository.getTotalTransferOutExcluding(account, excludeTransactionId)
        val transferIn = transactionRepository.getTotalTransferInExcluding(account, excludeTransactionId)
        val walletOut = transactionRepository.getTotalWalletMoveOutExcluding(account, excludeTransactionId)
        val walletIn = transactionRepository.getTotalWalletMoveInExcluding(account, excludeTransactionId)
        return opening + income - expense - transferOut + transferIn - walletOut + walletIn
    }

    private suspend fun computeBalance(account: AccountType): Double {
        if (account == AccountType.WALLET) return 0.0
        val opening = getOpeningBalance(account)
        val income = transactionRepository.getTotalIncomeForAccount(account)
        val expense = transactionRepository.getTotalExpenseForAccount(account)
        val transferOut = transactionRepository.getTotalTransferOut(account)
        val transferIn = transactionRepository.getTotalTransferIn(account)
        val walletOut = transactionRepository.getTotalWalletMoveOut(account)
        val walletIn = transactionRepository.getTotalWalletMoveIn(account)
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
        val income = transactionRepository.getPeriodIncomeForAccount(account, startDate, endDate)
        val expense = transactionRepository.getPeriodExpenseForAccount(account, startDate, endDate)
        val transferOut = transactionRepository.getPeriodTransferOut(account, startDate, endDate)
        val transferIn = transactionRepository.getPeriodTransferIn(account, startDate, endDate)
        return income - expense - transferOut + transferIn
    }
}
