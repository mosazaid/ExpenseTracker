package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.WalletCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CategoryExpenseShare(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val amount: Double,
    val percentage: Float
)

data class MonthlyBreakdown(
    val monthLabel: String,
    val startDate: Date,
    val endDate: Date,
    val income: Double,
    val expense: Double,
    val wallet: Double,
    val netBalance: Double,
    val categoryBreakdown: List<CategoryExpenseShare>
)

data class ThreeMonthStats(
    val months: List<MonthlyBreakdown> = emptyList(), // Chronological: [Month 1 (2 months ago), Month 2 (1 month ago), Month 3 (current)]
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalWallet: Double = 0.0,
    val averageMonthlyExpense: Double = 0.0,
    val combinedCategoryBreakdown: List<CategoryExpenseShare> = emptyList()
)

data class StatisticsState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalWallet: Double = 0.0,
    val balance: Double = 0.0,
    val periodLabel: String = "",
    val isLoading: Boolean = false,
    val threeMonthStats: ThreeMonthStats = ThreeMonthStats()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val periodCalculator: PeriodCalculator,
    private val walletCalculator: WalletCalculator,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _statisticsState = MutableStateFlow(StatisticsState())
    val statisticsState: StateFlow<StatisticsState> = _statisticsState.asStateFlow()

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    fun loadStatisticsForPeriod(period: HistoryPeriod, referenceDate: Date = Date()) {
        viewModelScope.launch {
            _statisticsState.value = _statisticsState.value.copy(isLoading = true)
            val bounds = periodCalculator.getBounds(period, referenceDate, monthMode.value)
            val totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME, bounds.start, bounds.end
            )
            val totalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE, bounds.start, bounds.end
            )
            val totalWallet = walletCalculator.getWalletBalance(bounds.start, bounds.end)

            val threeMonthStats = calculateThreeMonthStats(referenceDate, monthMode.value)

            _statisticsState.value = _statisticsState.value.copy(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                totalWallet = totalWallet,
                balance = totalIncome - totalExpense,
                periodLabel = bounds.label,
                threeMonthStats = threeMonthStats,
                isLoading = false
            )
        }
    }

    private suspend fun calculateThreeMonthStats(
        referenceDate: Date,
        mode: MonthMode
    ): ThreeMonthStats {
        val categories = categoryRepository.getAllCategoriesSnapshot()
        val catMap = categories.associateBy { it.id }

        // Determine 3 period intervals
        val periods = if (mode == MonthMode.SALARY) {
            getSalaryThreeMonthIntervals(referenceDate)
        } else {
            getCalendarThreeMonthIntervals(referenceDate)
        }

        val allThreeMonthExpenses = mutableListOf<com.example.expensetracker.data.database.entities.Transaction>()

        val monthlyBreakdowns = periods.map { bounds ->
            val txns = transactionRepository.getTransactionsBetweenDates(bounds.start, bounds.end).first()
            val income = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val wallet = walletCalculator.getWalletBalance(bounds.start, bounds.end)

            val expenseTxns = txns.filter { it.type == TransactionType.EXPENSE }
            allThreeMonthExpenses.addAll(expenseTxns)

            val catBreakdown = expenseTxns
                .groupBy { it.categoryId ?: 0L }
                .map { (catId, catTxns) ->
                    val cat = catMap[catId]
                    val amount = catTxns.sumOf { it.amount }
                    val pct = if (expense > 0) (amount / expense).toFloat() * 100f else 0f
                    CategoryExpenseShare(
                        categoryId = catId,
                        categoryName = cat?.name ?: "Other / Uncategorized",
                        categoryIcon = cat?.icon ?: "💰",
                        categoryColor = cat?.color ?: "#757575",
                        amount = amount,
                        percentage = pct
                    )
                }
                .sortedByDescending { it.amount }

            MonthlyBreakdown(
                monthLabel = bounds.label,
                startDate = bounds.start,
                endDate = bounds.end,
                income = income,
                expense = expense,
                wallet = wallet,
                netBalance = income - expense - wallet,
                categoryBreakdown = catBreakdown
            )
        }

        val totalIncome = monthlyBreakdowns.sumOf { it.income }
        val totalExpense = monthlyBreakdowns.sumOf { it.expense }
        val totalWallet = monthlyBreakdowns.sumOf { it.wallet }
        val avgExpense = if (monthlyBreakdowns.isNotEmpty()) totalExpense / monthlyBreakdowns.size else 0.0

        val combinedBreakdown = allThreeMonthExpenses
            .groupBy { it.categoryId ?: 0L }
            .map { (catId, catTxns) ->
                val cat = catMap[catId]
                val amount = catTxns.sumOf { it.amount }
                val pct = if (totalExpense > 0) (amount / totalExpense).toFloat() * 100f else 0f
                CategoryExpenseShare(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Other / Uncategorized",
                    categoryIcon = cat?.icon ?: "💰",
                    categoryColor = cat?.color ?: "#757575",
                    amount = amount,
                    percentage = pct
                )
            }
            .sortedByDescending { it.amount }

        return ThreeMonthStats(
            months = monthlyBreakdowns,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalWallet = totalWallet,
            averageMonthlyExpense = avgExpense,
            combinedCategoryBreakdown = combinedBreakdown
        )
    }

    private fun getCalendarThreeMonthIntervals(referenceDate: Date): List<PeriodBounds> {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val intervals = mutableListOf<PeriodBounds>()

        // 3 months: [2 months ago, 1 month ago, current]
        for (i in 2 downTo 0) {
            val cal = Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.MONTH, -i)
            }
            val start = DateUtils.getStartOfMonth(cal.time)
            val end = DateUtils.getEndOfMonth(cal.time)
            intervals.add(
                PeriodBounds(
                    start = start,
                    end = end,
                    label = monthFormat.format(start)
                )
            )
        }
        return intervals
    }

    private suspend fun getSalaryThreeMonthIntervals(referenceDate: Date): List<PeriodBounds> {
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val b3 = periodCalculator.getBounds(HistoryPeriod.MONTH, referenceDate, MonthMode.SALARY)

        val b2 = if (!b3.isSalaryFallback) {
            periodCalculator.getPreviousSalaryPeriodBounds(b3.start)
        } else null

        val b1 = if (b2 != null && !b2.isSalaryFallback) {
            periodCalculator.getPreviousSalaryPeriodBounds(b2.start)
        } else null

        val intervals = mutableListOf<PeriodBounds>()

        // Fallback generator if salary bounds are missing
        fun calendarFallback(monthsAgo: Int): PeriodBounds {
            val cal = Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.MONTH, -monthsAgo)
            }
            val start = DateUtils.getStartOfMonth(cal.time)
            val end = DateUtils.getEndOfMonth(cal.time)
            return PeriodBounds(start = start, end = end, label = monthFormat.format(start))
        }

        // Chronological order: [b1, b2, b3]
        intervals.add(b1 ?: calendarFallback(2))
        intervals.add(b2 ?: calendarFallback(1))
        intervals.add(b3)

        return intervals
    }
}
