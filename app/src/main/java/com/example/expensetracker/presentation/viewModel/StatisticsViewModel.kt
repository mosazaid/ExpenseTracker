package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
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

data class SubCategoryShare(
    val name: String,
    val amount: Double,
    val percentageOfCategory: Float
)

data class SubCategoryMonthlyTrend(
    val subCategoryName: String,
    val monthlyAmounts: List<Double>, // [Month 1, Month 2, Month 3]
    val deltaPercent: Float? // difference between current and previous month
)

data class SubCategoryComparisonData(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val totalCategoryExpenseCurrent: Double,
    val currentPeriodSubCategories: List<SubCategoryShare> = emptyList(),
    val monthlyTrends: List<SubCategoryMonthlyTrend> = emptyList(),
    val monthLabels: List<String> = emptyList()
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
    val threeMonthStats: ThreeMonthStats = ThreeMonthStats(),
    val expenseCategories: List<com.example.expensetracker.data.database.entities.Category> = emptyList(),
    val selectedSubCategoryId: Long? = null,
    val subCategoryComparison: SubCategoryComparisonData? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val periodCalculator: PeriodCalculator,
    private val walletCalculator: WalletCalculator,
    private val userPreferences: UserPreferences,
    private val monthlyLoanPaymentDao: com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao? = null
) : ViewModel() {

    private val _statisticsState = MutableStateFlow(StatisticsState())
    val statisticsState: StateFlow<StatisticsState> = _statisticsState.asStateFlow()

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    private var cachedExpenseTxnsByPeriod: List<List<com.example.expensetracker.data.database.entities.Transaction>> = emptyList()
    private var cachedMonthLabels: List<String> = emptyList()
    private var cachedSplitsByTxnId: Map<Long, List<com.example.expensetracker.data.database.entities.TransactionSplit>> = emptyMap()
    private var cachedCategories: Map<Long, com.example.expensetracker.data.database.entities.Category> = emptyMap()

    fun loadStatisticsForPeriod(period: HistoryPeriod, referenceDate: Date = Date()) {
        viewModelScope.launch {
            _statisticsState.value = _statisticsState.value.copy(isLoading = true)
            val bounds = periodCalculator.getBounds(period, referenceDate, monthMode.value)
            val rawTotalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME, bounds.start, bounds.end
            )
            val rawTotalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE, bounds.start, bounds.end
            )
            val paidLoans = monthlyLoanPaymentDao?.getPaidLoansBetweenDates(bounds.start, bounds.end) ?: emptyList()
            val loansDeductedFromIncome = paidLoans.filter { it.deductFromIncome }
            val loansDeducted = loansDeductedFromIncome.sumOf { it.amount }

            val totalIncome = (rawTotalIncome - loansDeducted).coerceAtLeast(0.0)
            val totalExpense = (rawTotalExpense - loansDeducted).coerceAtLeast(0.0)
            val totalWallet = walletCalculator.getWalletBalance(bounds.start, bounds.end)

            val threeMonthStats = calculateThreeMonthStats(referenceDate, monthMode.value)

            val expenseCats = categoryRepository.getAllCategoriesSnapshot().filter { it.type == TransactionType.EXPENSE }
            val currentSelectedId = _statisticsState.value.selectedSubCategoryId
            val defaultTargetId = if (currentSelectedId != null && expenseCats.any { it.id == currentSelectedId }) {
                currentSelectedId
            } else {
                threeMonthStats.months.lastOrNull()?.categoryBreakdown?.firstOrNull()?.categoryId
                    ?: expenseCats.firstOrNull()?.id
            }

            val subCategoryComparison = defaultTargetId?.let { buildSubCategoryComparison(it) }

            _statisticsState.value = _statisticsState.value.copy(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                totalWallet = totalWallet,
                balance = totalIncome - totalExpense,
                periodLabel = bounds.label,
                threeMonthStats = threeMonthStats,
                expenseCategories = expenseCats,
                selectedSubCategoryId = defaultTargetId,
                subCategoryComparison = subCategoryComparison,
                isLoading = false
            )
        }
    }

    fun selectCategoryForSubCategoryComparison(categoryId: Long) {
        val comparison = buildSubCategoryComparison(categoryId)
        _statisticsState.value = _statisticsState.value.copy(
            selectedSubCategoryId = categoryId,
            subCategoryComparison = comparison
        )
    }

    private fun buildSubCategoryComparison(categoryId: Long): SubCategoryComparisonData? {
        val cat = cachedCategories[categoryId] ?: return null
        if (cachedExpenseTxnsByPeriod.isEmpty()) return null

        val currentPeriodTxns = cachedExpenseTxnsByPeriod.lastOrNull().orEmpty()
        val currentSubSpend = extractSubCategorySpend(currentPeriodTxns, categoryId, cachedSplitsByTxnId)
        val totalCatCurrent = currentSubSpend.values.sum()

        val currentSubShares = currentSubSpend.map { (name, amt) ->
            SubCategoryShare(
                name = name,
                amount = amt,
                percentageOfCategory = if (totalCatCurrent > 0) ((amt / totalCatCurrent) * 100f).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        val periodSubSpends = cachedExpenseTxnsByPeriod.map { txns ->
            extractSubCategorySpend(txns, categoryId, cachedSplitsByTxnId)
        }

        val allSubNames = periodSubSpends.flatMap { it.keys }.distinct()

        val monthlyTrends = allSubNames.map { name ->
            val amounts = periodSubSpends.map { it[name] ?: 0.0 }
            val prevAmt = if (amounts.size >= 2) amounts[amounts.size - 2] else 0.0
            val currAmt = amounts.lastOrNull() ?: 0.0
            val deltaPercent = if (prevAmt > 0) {
                (((currAmt - prevAmt) / prevAmt) * 100f).toFloat()
            } else null

            SubCategoryMonthlyTrend(
                subCategoryName = name,
                monthlyAmounts = amounts,
                deltaPercent = deltaPercent
            )
        }.sortedByDescending { it.monthlyAmounts.lastOrNull() ?: 0.0 }

        return SubCategoryComparisonData(
            categoryId = categoryId,
            categoryName = cat.name,
            categoryIcon = cat.icon,
            categoryColor = cat.color,
            totalCategoryExpenseCurrent = totalCatCurrent,
            currentPeriodSubCategories = currentSubShares,
            monthlyTrends = monthlyTrends,
            monthLabels = cachedMonthLabels
        )
    }

    private fun extractSubCategorySpend(
        expenseTxns: List<com.example.expensetracker.data.database.entities.Transaction>,
        categoryId: Long,
        allSplitsByTxnId: Map<Long, List<com.example.expensetracker.data.database.entities.TransactionSplit>>
    ): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        val catTxns = expenseTxns.filter { it.categoryId == categoryId }

        for (txn in catTxns) {
            val splits = allSplitsByTxnId[txn.id].orEmpty()
            if (splits.isNotEmpty()) {
                for (split in splits) {
                    val rawName = split.subCategoryName?.trim()
                    val key = if (!rawName.isNullOrBlank()) rawName else "Other"
                    map[key] = (map[key] ?: 0.0) + split.amount
                }
            } else {
                val rawName = txn.subDescription?.trim()
                val key = if (!rawName.isNullOrBlank()) rawName else "Other"
                map[key] = (map[key] ?: 0.0) + txn.amount
            }
        }
        return map
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
        val periodExpenseList = mutableListOf<List<com.example.expensetracker.data.database.entities.Transaction>>()

        val monthlyBreakdowns = periods.map { bounds ->
            val txns = transactionRepository.getTransactionsBetweenDates(bounds.start, bounds.end).first()
            val paidLoans = monthlyLoanPaymentDao?.getPaidLoansBetweenDates(bounds.start, bounds.end) ?: emptyList()
            val loansDeductedFromIncome = paidLoans.filter { it.deductFromIncome }
            val paidTxnIds = loansDeductedFromIncome.mapNotNull { it.transactionId }.toSet()
            val loansDeducted = loansDeductedFromIncome.sumOf { it.amount }

            val rawIncome = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val income = (rawIncome - loansDeducted).coerceAtLeast(0.0)

            val expenseTxns = txns.filter { it.type == TransactionType.EXPENSE && it.id !in paidTxnIds }
            val expense = expenseTxns.sumOf { it.amount }
            val wallet = walletCalculator.getWalletBalance(bounds.start, bounds.end)

            allThreeMonthExpenses.addAll(expenseTxns)
            periodExpenseList.add(expenseTxns)

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

        val splits = transactionRepository.getAllSplitsSnapshot()
        cachedExpenseTxnsByPeriod = periodExpenseList
        cachedMonthLabels = periods.map { it.label }
        cachedSplitsByTxnId = splits.groupBy { it.transactionId }
        cachedCategories = catMap

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
