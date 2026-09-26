package com.example.expensetracker.presentation.model

import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.domain.AccountBalances
import com.example.expensetracker.domain.PeriodBounds

/**
 * Holds summary data that is scoped to the current salary/calendar month,
 * independent of which period the transaction list is filtered to.
 *
 * [incomeExpenseByAccount] = income − expense per account, transfers excluded.
 * [transferImpact] = net transfer movement per account (informational; total always 0).
 */
data class MonthSummaryUiState(
    val monthBounds: PeriodBounds? = null,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthNet: Double = 0.0,
    val broughtForward: Double = 0.0,
    val previousMonthRemaining: Double = 0.0,
    val monthRemaining: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val periodChangeByAccount: AccountBalances = AccountBalances(0.0, 0.0),
    val incomeExpenseByAccount: AccountBalances = AccountBalances(0.0, 0.0),
    val transferImpact: AccountBalances = AccountBalances(0.0, 0.0),
    val currentBalances: AccountBalances = AccountBalances(0.0, 0.0),
    val walletBalance: Double = 0.0,
    val dueReminders: List<RecurringTransaction> = emptyList(),
    val periodLoansDeducted: Double = 0.0,
    val totalPaidLoans: Double = 0.0,
    val paidLoans: List<com.example.expensetracker.data.database.dao.PaidLoanInfo> = emptyList()
) {
    val hasBroughtForward: Boolean get() = previousMonthRemaining != 0.0 || broughtForward != 0.0
    val remainingIncome: Double get() = monthNet - walletBalance
    val hasWallet: Boolean get() = walletBalance != 0.0
    val hasTransfers: Boolean get() =
        transferImpact.cash != 0.0 || transferImpact.bank != 0.0
}
