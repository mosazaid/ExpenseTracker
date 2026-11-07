package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionSplit
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodBounds
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.presentation.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

data class DepositYearStats(
    val totalPurchases: Double = 0.0,
    val transactions: List<com.example.expensetracker.data.database.entities.Transaction> = emptyList(),
    val percentageOfAnnualExpense: Float = 0f
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
    val subCategoryComparison: SubCategoryComparisonData? = null,
    val isYearPeriod: Boolean = false,
    val depositYearStats: DepositYearStats? = null,
    val advancedComparison: AdvancedComparisonData? = null,
    val comparisonMode: ComparisonMode = ComparisonMode.DAY_NORMALIZED,
    val basePeriodIndex: Int = 0,
    val comparePeriodIndex: Int = 1,
    val drillDownData: SubcategoryDrillDownData? = null
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
    private var cachedDrillDownBaseTxns: List<Transaction> = emptyList()
    private var cachedDrillDownBaseBounds: PeriodBounds? = null

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

            val isYear = period == HistoryPeriod.YEAR
            val depositYearStats = if (isYear) {
                val purchases = transactionRepository.getPeriodExpenseForAccount(
                    com.example.expensetracker.data.database.entities.AccountType.DEPOSIT,
                    bounds.start,
                    bounds.end
                )
                val depositTxns = transactionRepository.getTransactionsBetweenDatesSnapshot(bounds.start, bounds.end)
                    .filter { it.accountType == com.example.expensetracker.data.database.entities.AccountType.DEPOSIT && it.type == TransactionType.EXPENSE }
                val combinedSpend = totalExpense + purchases
                val pct = if (combinedSpend > 0) ((purchases / combinedSpend) * 100f).toFloat() else 0f
                DepositYearStats(
                    totalPurchases = purchases,
                    transactions = depositTxns,
                    percentageOfAnnualExpense = pct
                )
            } else null

            val advancedComp = buildAdvancedComparison(
                baseIndex = _statisticsState.value.basePeriodIndex,
                compareIndex = _statisticsState.value.comparePeriodIndex,
                mode = _statisticsState.value.comparisonMode,
                referenceDate = referenceDate
            )

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
                isYearPeriod = isYear,
                depositYearStats = depositYearStats,
                advancedComparison = advancedComp,
                isLoading = false
            )
        }
    }

    fun setComparisonMode(mode: ComparisonMode) {
        viewModelScope.launch {
            _statisticsState.value = _statisticsState.value.copy(comparisonMode = mode)
            refreshAdvancedComparison()
        }
    }

    fun setBasePeriodIndex(index: Int) {
        viewModelScope.launch {
            _statisticsState.value = _statisticsState.value.copy(basePeriodIndex = index)
            refreshAdvancedComparison()
        }
    }

    fun setComparePeriodIndex(index: Int) {
        viewModelScope.launch {
            _statisticsState.value = _statisticsState.value.copy(comparePeriodIndex = index)
            refreshAdvancedComparison()
        }
    }

    fun refreshAdvancedComparison(referenceDate: Date = Date()) {
        viewModelScope.launch {
            val currentState = _statisticsState.value
            val advancedComp = buildAdvancedComparison(
                baseIndex = currentState.basePeriodIndex,
                compareIndex = currentState.comparePeriodIndex,
                mode = currentState.comparisonMode,
                referenceDate = referenceDate
            )
            _statisticsState.value = _statisticsState.value.copy(advancedComparison = advancedComp)
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

            val expenseTxns = txns.filter {
                it.type == TransactionType.EXPENSE &&
                it.accountType != AccountType.WALLET &&
                it.accountType != AccountType.DEPOSIT &&
                !it.excludeFromMonthExpenses &&
                it.id !in paidTxnIds
            }
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

    private suspend fun buildAdvancedComparison(
        baseIndex: Int,
        compareIndex: Int,
        mode: ComparisonMode,
        referenceDate: Date
    ): AdvancedComparisonData {
        val monthModeVal = monthMode.value
        val availablePeriods = if (monthModeVal == MonthMode.SALARY) {
            getAvailableSalaryPeriods(referenceDate)
        } else {
            getAvailableCalendarPeriods(referenceDate)
        }

        val safeBaseIndex = baseIndex.coerceIn(0, (availablePeriods.size - 1).coerceAtLeast(0))
        val safeCompareIndex = compareIndex.coerceIn(0, (availablePeriods.size - 1).coerceAtLeast(0))
        val baseOption = availablePeriods.getOrNull(safeBaseIndex) ?: availablePeriods.firstOrNull()
            ?: PeriodOption("cur", "Current", PeriodBounds(referenceDate, referenceDate, "Current"))
        val compareOption = availablePeriods.getOrNull(safeCompareIndex) ?: availablePeriods.getOrElse(1) { baseOption }

        val now = Date()
        val isBaseActive = now.time in baseOption.bounds.start.time..baseOption.bounds.end.time
        val cutoffDay = if (mode == ComparisonMode.DAY_NORMALIZED) {
            if (isBaseActive) {
                max(1, ((now.time - baseOption.bounds.start.time) / (1000L * 60 * 60 * 24)).toInt() + 1)
            } else {
                val baseDays = max(1, ((baseOption.bounds.end.time - baseOption.bounds.start.time) / (1000L * 60 * 60 * 24)).toInt() + 1)
                val compareDays = max(1, ((compareOption.bounds.end.time - compareOption.bounds.start.time) / (1000L * 60 * 60 * 24)).toInt() + 1)
                min(baseDays, compareDays)
            }
        } else null

        val baseRawTxns = transactionRepository.getTransactionsBetweenDatesSnapshot(baseOption.bounds.start, baseOption.bounds.end)
        val compareRawTxns = transactionRepository.getTransactionsBetweenDatesSnapshot(compareOption.bounds.start, compareOption.bounds.end)

        val paidLoansBase = monthlyLoanPaymentDao?.getPaidLoansBetweenDates(baseOption.bounds.start, baseOption.bounds.end) ?: emptyList()
        val paidLoansBaseIds = paidLoansBase.filter { it.deductFromIncome }.mapNotNull { it.transactionId }.toSet()

        val paidLoansCompare = monthlyLoanPaymentDao?.getPaidLoansBetweenDates(compareOption.bounds.start, compareOption.bounds.end) ?: emptyList()
        val paidLoansCompareIds = paidLoansCompare.filter { it.deductFromIncome }.mapNotNull { it.transactionId }.toSet()

        fun filterTxns(txns: List<Transaction>, bounds: PeriodBounds, paidLoanIds: Set<Long>): List<Transaction> {
            return txns.filter { txn ->
                txn.type == TransactionType.EXPENSE &&
                txn.accountType != AccountType.WALLET &&
                txn.accountType != AccountType.DEPOSIT &&
                !txn.excludeFromMonthExpenses &&
                txn.id !in paidLoanIds &&
                (cutoffDay == null || getDayOffset(txn.date, bounds.start) <= cutoffDay)
            }
        }

        val baseTxns = filterTxns(baseRawTxns, baseOption.bounds, paidLoansBaseIds)
        val compareTxns = filterTxns(compareRawTxns, compareOption.bounds, paidLoansCompareIds)

        cachedDrillDownBaseTxns = baseTxns
        cachedDrillDownBaseBounds = baseOption.bounds

        val splits = transactionRepository.getAllSplitsSnapshot().groupBy { it.transactionId }
        val categories = categoryRepository.getAllCategoriesSnapshot().associateBy { it.id }

        val baseCatSubMap = aggregateSpendByCatAndSub(baseTxns, splits)
        val compareCatSubMap = aggregateSpendByCatAndSub(compareTxns, splits)

        val allCatIds = (baseCatSubMap.keys + compareCatSubMap.keys).distinct()

        val categoryItems = allCatIds.mapNotNull { catId ->
            val cat = categories[catId] ?: return@mapNotNull null
            val baseSubs = baseCatSubMap[catId].orEmpty()
            val compareSubs = compareCatSubMap[catId].orEmpty()

            val allSubNames = (baseSubs.keys + compareSubs.keys).distinct()
            val subItems = allSubNames.map { subName ->
                val bInfo = baseSubs[subName]
                val bAmt = bInfo?.first ?: 0.0
                val bCount = bInfo?.second?.size ?: 0

                val cAmt = compareSubs[subName]?.first ?: 0.0
                val delta = bAmt - cAmt
                val deltaPct = if (cAmt > 0) (((bAmt - cAmt) / cAmt) * 100f).toFloat() else null

                val insight = if (deltaPct != null && deltaPct > 30.0f) {
                    val topTxn = bInfo?.second?.maxByOrNull { it.amount }
                    if (topTxn != null && topTxn.description.isNotBlank()) {
                        "Spike +${deltaPct.roundToInt()}%: Top \"${topTxn.description}\" (${CurrencyUtils.formatCurrency(topTxn.amount)})"
                    } else {
                        "Spike +${deltaPct.roundToInt()}% across $bCount purchases"
                    }
                } else null

                SubcategoryComparisonItem(
                    name = subName,
                    baseAmount = bAmt,
                    compareAmount = cAmt,
                    deltaAmount = delta,
                    deltaPercent = deltaPct,
                    baseTxnCount = bCount,
                    varianceInsight = insight
                )
            }.sortedByDescending { it.baseAmount }

            val baseTotal = subItems.sumOf { it.baseAmount }
            val compareTotal = subItems.sumOf { it.compareAmount }
            val catDelta = baseTotal - compareTotal
            val catDeltaPct = if (compareTotal > 0) (((baseTotal - compareTotal) / compareTotal) * 100f).toFloat() else null

            val topSpikingSub = subItems.filter { it.deltaPercent != null && it.deltaPercent > 20.0f }
                .maxByOrNull { it.deltaAmount }
            val topDriver = topSpikingSub?.let { "Driven by ${it.name} (+${it.deltaPercent?.roundToInt()}%)" }

            CategoryComparisonItem(
                categoryId = catId,
                categoryName = cat.name,
                categoryIcon = cat.icon,
                categoryColor = cat.color,
                baseTotal = baseTotal,
                compareTotal = compareTotal,
                deltaAmount = catDelta,
                deltaPercent = catDeltaPct,
                subcategories = subItems,
                topSpikeInsight = topDriver
            )
        }.sortedByDescending { it.baseTotal }

        val baseTotalExp = categoryItems.sumOf { it.baseTotal }
        val compareTotalExp = categoryItems.sumOf { it.compareTotal }
        val overallDelta = if (compareTotalExp > 0) (((baseTotalExp - compareTotalExp) / compareTotalExp) * 100f).toFloat() else null

        return AdvancedComparisonData(
            basePeriod = baseOption.bounds,
            comparePeriod = compareOption.bounds,
            mode = mode,
            cutoffDay = cutoffDay,
            baseTotalExpense = baseTotalExp,
            compareTotalExpense = compareTotalExp,
            overallDeltaPercent = overallDelta,
            categories = categoryItems,
            availablePeriods = availablePeriods
        )
    }

    private fun getDayOffset(date: Date, start: Date): Int {
        val diffMs = date.time - start.time
        return max(1, (diffMs / (1000L * 60 * 60 * 24)).toInt() + 1)
    }

    private fun aggregateSpendByCatAndSub(
        txns: List<Transaction>,
        splitsByTxnId: Map<Long, List<TransactionSplit>>
    ): Map<Long, Map<String, Pair<Double, List<Transaction>>>> {
        val result = mutableMapOf<Long, MutableMap<String, Pair<Double, MutableList<Transaction>>>>()
        for (txn in txns) {
            val catId = txn.categoryId ?: continue
            val splits = splitsByTxnId[txn.id].orEmpty()
            val catMap = result.getOrPut(catId) { mutableMapOf() }
            if (splits.isNotEmpty()) {
                for (split in splits) {
                    val subName = split.subCategoryName?.trim()?.takeIf { it.isNotBlank() } ?: "Other"
                    val curr = catMap[subName]
                    val newAmt = (curr?.first ?: 0.0) + split.amount
                    val list = curr?.second ?: mutableListOf()
                    list.add(txn)
                    catMap[subName] = Pair(newAmt, list)
                }
            } else {
                val subName = txn.subDescription?.trim()?.takeIf { it.isNotBlank() } ?: "Other"
                val curr = catMap[subName]
                val newAmt = (curr?.first ?: 0.0) + txn.amount
                val list = curr?.second ?: mutableListOf()
                list.add(txn)
                catMap[subName] = Pair(newAmt, list)
            }
        }
        return result
    }

    private fun getAvailableCalendarPeriods(referenceDate: Date, count: Int = 12): List<PeriodOption> {
        val options = mutableListOf<PeriodOption>()
        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        for (i in 0 until count) {
            val c = Calendar.getInstance().apply {
                time = referenceDate
                add(Calendar.MONTH, -i)
            }
            val start = DateUtils.getStartOfMonth(c.time)
            val end = DateUtils.getEndOfMonth(c.time)
            val monthLabel = format.format(start)
            val label = if (i == 0) "$monthLabel (Current)" else monthLabel
            options.add(PeriodOption(key = "cal_$i", label = label, bounds = PeriodBounds(start, end, monthLabel)))
        }
        return options
    }

    private suspend fun getAvailableSalaryPeriods(referenceDate: Date, count: Int = 12): List<PeriodOption> {
        val options = mutableListOf<PeriodOption>()
        var currentRef = referenceDate
        for (i in 0 until count) {
            val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, currentRef, MonthMode.SALARY)
            val label = if (i == 0) "${bounds.label} (Current)" else bounds.label
            options.add(PeriodOption(key = "sal_$i", label = label, bounds = bounds))
            val nextRefCal = Calendar.getInstance().apply {
                time = bounds.start
                add(Calendar.DAY_OF_MONTH, -1)
            }
            currentRef = nextRefCal.time
        }
        return options
    }

    fun openSubcategoryDrillDown(categoryId: Long, subcategoryName: String) {
        val cat = cachedCategories[categoryId] ?: return
        val baseTxns = cachedDrillDownBaseTxns
        val splits = cachedSplitsByTxnId

        val matchingItems = mutableListOf<DrillDownTransactionItem>()
        var totalAmount = 0.0

        for (txn in baseTxns.filter { it.categoryId == categoryId }) {
            val txnSplits = splits[txn.id].orEmpty()
            if (txnSplits.isNotEmpty()) {
                val matchingSplits = txnSplits.filter {
                    val name = it.subCategoryName?.trim()?.takeIf { s -> s.isNotBlank() } ?: "Other"
                    name.equals(subcategoryName, ignoreCase = true)
                }
                for (split in matchingSplits) {
                    matchingItems.add(
                        DrillDownTransactionItem(
                            id = txn.id,
                            title = split.note?.takeIf { it.isNotBlank() } ?: txn.description.takeIf { it.isNotBlank() } ?: cat.name,
                            note = "Part of Split: ${CurrencyUtils.formatCurrency(txn.amount)}",
                            amount = split.amount,
                            date = txn.date,
                            accountType = txn.accountType,
                            isSplit = true,
                            parentAmount = txn.amount
                        )
                    )
                    totalAmount += split.amount
                }
            } else {
                val subName = txn.subDescription?.trim()?.takeIf { it.isNotBlank() } ?: "Other"
                if (subName.equals(subcategoryName, ignoreCase = true)) {
                    matchingItems.add(
                        DrillDownTransactionItem(
                            id = txn.id,
                            title = txn.description.takeIf { it.isNotBlank() } ?: subcategoryName,
                            note = txn.subDescription,
                            amount = txn.amount,
                            date = txn.date,
                            accountType = txn.accountType,
                            isSplit = false
                        )
                    )
                    totalAmount += txn.amount
                }
            }
        }

        _statisticsState.value = _statisticsState.value.copy(
            drillDownData = SubcategoryDrillDownData(
                categoryId = categoryId,
                categoryName = cat.name,
                categoryIcon = cat.icon,
                subcategoryName = subcategoryName,
                periodLabel = cachedDrillDownBaseBounds?.label.orEmpty(),
                totalAmount = totalAmount,
                transactionCount = matchingItems.size,
                transactions = matchingItems.sortedByDescending { it.date }
            )
        )
    }

    fun closeDrillDown() {
        _statisticsState.value = _statisticsState.value.copy(drillDownData = null)
    }
}
