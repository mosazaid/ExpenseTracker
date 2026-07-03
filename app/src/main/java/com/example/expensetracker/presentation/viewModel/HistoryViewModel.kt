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
    private val budgetProgressCalculator: BudgetProgressCalculator
) : ViewModel() {

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val allCategories = categoryRepository.getAllCategories()

    data class HistoryUiState(
        val periodBalances: AccountBalances = AccountBalances(0.0, 0.0),
        val allTimeBalances: AccountBalances = AccountBalances(0.0, 0.0),
        val categoryTotals: Map<Long, Double> = emptyMap(),
        val budgetProgress: Map<Long, CategoryBudgetProgress> = emptyMap(),
        val dueReminders: List<RecurringTransaction> = emptyList()
    )

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

    suspend fun getPeriodBalances(startDate: Date, endDate: Date): AccountBalances {
        return balanceCalculator.getPeriodChange(startDate, endDate)
    }

    suspend fun getAllTimeBalances(): AccountBalances {
        return balanceCalculator.getAllTimeBalances()
    }

    suspend fun getCategoryExpenseTotals(
        categories: List<Category>,
        startDate: Date,
        endDate: Date
    ): Map<Long, Double> {
        return categories
            .filter { it.type == TransactionType.EXPENSE }
            .associate { category ->
                category.id to transactionRepository.getTotalAmountByCategoryAndDateRange(
                    category.id,
                    startDate,
                    endDate
                )
            }
    }

    suspend fun getBudgetProgressMap(
        categoryIds: List<Long>,
        periodStart: Date,
        periodEnd: Date
    ): Map<Long, CategoryBudgetProgress> {
        return budgetProgressCalculator.getProgressMap(categoryIds, periodStart, periodEnd)
    }

    suspend fun buildUiState(
        categories: List<Category>,
        expenseCategoryIds: List<Long>,
        startDate: Date,
        endDate: Date
    ): HistoryUiState {
        val periodBalances = balanceCalculator.getPeriodChange(startDate, endDate)
        val allTimeBalances = balanceCalculator.getAllTimeBalances()
        val categoryTotals = categories
            .filter { it.type == TransactionType.EXPENSE }
            .associate { category ->
                category.id to transactionRepository.getTotalAmountByCategoryAndDateRange(
                    category.id,
                    startDate,
                    endDate
                )
            }
        val budgetProgress = budgetProgressCalculator.getProgressMap(expenseCategoryIds, startDate, endDate)
        val dueReminders = recurringRepository.getDueReminders()

        return HistoryUiState(
            periodBalances = periodBalances,
            allTimeBalances = allTimeBalances,
            categoryTotals = categoryTotals,
            budgetProgress = budgetProgress,
            dueReminders = dueReminders
        )
    }

    suspend fun getDueReminders(): List<RecurringTransaction> {
        return recurringRepository.getDueReminders()
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
