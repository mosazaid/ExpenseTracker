package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.SalaryWalletPeriodSummary
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.presentation.model.MonthSummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val recurringRepository: IRecurringRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator
) : ViewModel() {

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: Flow<List<Category>> = categoryRepository.getCategoriesSortedByUsage()

    /**
     * Derives a [MonthSummaryUiState] reactively from [monthMode] + [allTransactions].
     *
     * Using [combine] + [flatMapLatest] ensures:
     * - The computation runs on the ViewModel scope (background thread).
     * - A new computation is started only when month-mode or the transaction
     *   list actually changes — NOT on every recomposition.
     * - Only the **latest** emission is kept; stale computations are cancelled.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthSummary: StateFlow<MonthSummaryUiState> =
        combine(monthMode, allTransactions) { mode, _ -> mode }
            .flatMapLatest { mode ->
                flow { emit(buildMonthSummary(mode, Date())) }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                MonthSummaryUiState()
            )

    /**
     * Computes the month-scoped summary (income, expense, per-account breakdown, reminders).
     * Always uses the salary month (or calendar month) bounds regardless of list period.
     * Kept internal/suspend for backward-compatible one-off calls (e.g., tests).
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
        val currentRemainingIncome = financials.periodNet - walletBalance
        val totalRemaining = financials.currentBalances.total
        val previousMonthRemaining = totalRemaining - currentRemainingIncome

        return MonthSummaryUiState(
            monthBounds = bounds,
            monthIncome = financials.periodIncome,
            monthExpense = financials.periodExpense,
            monthNet = financials.periodNet,
            broughtForward = broughtForward,
            previousMonthRemaining = previousMonthRemaining,
            monthRemaining = currentRemainingIncome,
            totalRemaining = totalRemaining,
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

    suspend fun isRecurringBannerDismissed(recurring: RecurringTransaction): Boolean {
        return userPreferences.isRecurringDismissed(recurring.id, recurring.nextDueDate.time)
    }

    suspend fun dismissRecurringBanner(recurring: RecurringTransaction) {
        userPreferences.dismissRecurringBanner(recurring.id, recurring.nextDueDate.time)
    }
}
