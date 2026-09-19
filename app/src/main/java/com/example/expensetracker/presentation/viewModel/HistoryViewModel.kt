package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.AccountBalances
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.BudgetProgressCalculator
import com.example.expensetracker.domain.CategoryBudgetProgress
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.SalaryWalletPeriodSummary
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.data.database.entities.SubCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator,
    private val budgetProgressCalculator: BudgetProgressCalculator
) : ViewModel() {

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = categoryRepository.getAllCategories()

    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        return categoryRepository.getSubCategories(categoryId)
    }

    /**
     * Holds summary data that is ALWAYS scoped to the current salary/calendar month,
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
        val monthRemaining: Double = 0.0,
        val periodChangeByAccount: AccountBalances = AccountBalances(0.0, 0.0),
        val incomeExpenseByAccount: AccountBalances = AccountBalances(0.0, 0.0),
        val transferImpact: AccountBalances = AccountBalances(0.0, 0.0),
        val currentBalances: AccountBalances = AccountBalances(0.0, 0.0),
        val walletBalance: Double = 0.0,
        val dueReminders: List<RecurringTransaction> = emptyList()
    ) {
        val hasBroughtForward: Boolean get() = broughtForward != 0.0
        val hasWallet: Boolean get() = walletBalance != 0.0
        val hasTransfers: Boolean get() =
            transferImpact.cash != 0.0 || transferImpact.bank != 0.0
    }

    /**
     * Holds data scoped to whatever period the user has selected for the transaction list
     * (day / week / month / year). Used for category totals and budget progress only.
     */
    data class PeriodUiState(
        val categoryTotals: Map<Long, Double> = emptyMap(),
        val budgetProgress: Map<Long, CategoryBudgetProgress> = emptyMap()
    )

    /**
     * Computes the month-scoped summary (income, expense, per-account breakdown, reminders).
     * Always uses the salary month (or calendar month) bounds regardless of list period.
     *
     * Per-account breakdown uses income − expense only (transfers excluded) so internal
     * cash↔bank movements do not distort the "earning/spending" picture.
     */
    suspend fun buildMonthSummary(
        mode: MonthMode,
        referenceDate: Date
    ): MonthSummaryUiState {
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, referenceDate, mode)
        val financials = balanceCalculator.buildMonthFinancialSummary(bounds.start, bounds.end)
        val anchor = if (mode == MonthMode.SALARY) {
            periodCalculator.getCurrentPeriodAnchor(referenceDate)
        } else {
            null
        }
        val broughtForward = anchor?.carriedForwardBalance ?: 0.0
        val walletBalance = if (mode == MonthMode.SALARY) {
            walletCalculator.getWalletBalance(bounds.start, bounds.end)
        } else {
            0.0
        }
        val dueReminders = recurringRepository.getDueReminders()
        val remaining = broughtForward + financials.periodNet - walletBalance
        return MonthSummaryUiState(
            monthBounds = bounds,
            monthIncome = financials.periodIncome,
            monthExpense = financials.periodExpense,
            monthNet = financials.periodNet,
            broughtForward = broughtForward,
            monthRemaining = remaining,
            periodChangeByAccount = financials.periodChangeByAccount,
            incomeExpenseByAccount = financials.incomeExpenseByAccount,
            transferImpact = financials.transferImpact,
            currentBalances = financials.currentBalances,
            walletBalance = walletBalance,
            dueReminders = dueReminders
        )
    }

    suspend fun buildSalaryWalletYearSummary(year: Int): List<SalaryWalletPeriodSummary> {
        return walletCalculator.getSalaryPeriodSummariesForYear(year)
    }

    suspend fun canModifyTransaction(transaction: Transaction): Boolean {
        if (transaction.type != TransactionType.WALLET_MOVE) return true
        return walletCalculator.isDateInOpenSalaryPeriod(transaction.date)
    }

    /**
     * Computes category totals and budget progress for the user's selected list period.
     */
    suspend fun buildPeriodUiState(
        categories: List<Category>,
        expenseCategoryIds: List<Long>,
        startDate: Date,
        endDate: Date
    ): PeriodUiState {
        val categoryTotals = categories
            .filter { it.type == TransactionType.EXPENSE }
            .associate { category ->
                category.id to transactionRepository.getTotalAmountByCategoryAndDateRange(
                    category.id, startDate, endDate
                )
            }
        val budgetProgress = budgetProgressCalculator.getProgressMap(
            expenseCategoryIds, startDate, endDate
        )
        return PeriodUiState(
            categoryTotals = categoryTotals,
            budgetProgress = budgetProgress
        )
    }

    suspend fun getPeriodBounds(
        period: HistoryPeriod,
        mode: MonthMode,
        referenceDate: Date = Date()
    ): PeriodBounds {
        return periodCalculator.getBounds(period, referenceDate, mode)
    }

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsBetweenDates(startDate, endDate)
    }

    suspend fun isRecurringBannerDismissed(recurring: RecurringTransaction): Boolean {
        return userPreferences.isRecurringDismissed(recurring.id, recurring.nextDueDate.time)
    }

    suspend fun dismissRecurringBanner(recurring: RecurringTransaction) {
        userPreferences.dismissRecurringBanner(recurring.id, recurring.nextDueDate.time)
    }

    fun setMonthMode(mode: MonthMode) {
        viewModelScope.launch {
            userPreferences.setMonthMode(mode)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}
