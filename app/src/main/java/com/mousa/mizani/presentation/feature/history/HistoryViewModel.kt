package com.mousa.mizani.presentation.feature.history

import com.mousa.mizani.domain.calculator.CategoryBudgetProgress
import com.mousa.mizani.domain.calculator.PeriodBounds
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.SubCategory
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BudgetProgressCalculator
import com.mousa.mizani.domain.constants.HistoryPeriod
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.calculator.SalaryWalletPeriodSummary
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import com.mousa.mizani.presentation.feature.overview.model.MonthSummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    val splitsMap: StateFlow<Map<Long, List<com.mousa.mizani.data.database.entities.TransactionSplit>>> =
        transactionRepository.getAllSplitsFlow()
            .map { splits -> splits.groupBy { it.transactionId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allCategories = categoryRepository.getCategoriesSortedByUsage()

    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        return categoryRepository.getSubCategories(categoryId)
    }

    suspend fun getSplitsForTransaction(transactionId: Long): List<com.mousa.mizani.data.database.entities.TransactionSplit> {
        return transactionRepository.getSplitsForTransactionSnapshot(transactionId)
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

typealias MonthSummaryUiState = com.mousa.mizani.presentation.feature.overview.model.MonthSummaryUiState

