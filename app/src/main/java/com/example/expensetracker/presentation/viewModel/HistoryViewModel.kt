package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BudgetProgressCalculator
import com.example.expensetracker.domain.CategoryBudgetProgress
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.SalaryWalletPeriodSummary
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.presentation.model.MonthSummaryUiState
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
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val walletCalculator: WalletCalculator,
    private val budgetProgressCalculator: BudgetProgressCalculator
) : ViewModel() {

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = categoryRepository.getCategoriesSortedByUsage()

    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        return categoryRepository.getSubCategories(categoryId)
    }

    /**
     * Holds data scoped to whatever period the user has selected for the transaction list
     * (day / week / month / year). Used for category totals and budget progress only.
     */
    data class PeriodUiState(
        val categoryTotals: Map<Long, Double> = emptyMap(),
        val budgetProgress: Map<Long, CategoryBudgetProgress> = emptyMap()
    )

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

typealias MonthSummaryUiState = com.example.expensetracker.presentation.model.MonthSummaryUiState

