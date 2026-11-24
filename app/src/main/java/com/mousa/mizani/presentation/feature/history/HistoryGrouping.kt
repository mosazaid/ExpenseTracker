package com.mousa.mizani.presentation.feature.history

import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.database.entities.TransactionSplit
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.domain.constants.HistoryPeriod
import com.mousa.mizani.domain.calculator.SalaryWalletPeriodSummary
import com.mousa.mizani.core.time.DateUtils
import java.util.Calendar
import java.util.Date

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    WALLET("Wallet"),
    OWED("Owed")
}

enum class TransactionSortOrder(val label: String) {
    DESC("Newest first"),
    ASC("Oldest first")
}

data class HistorySection(
    val key: String,
    val title: String,
    val expenseTotal: Double,
    val transactions: List<Transaction>
)

data class HistoryMonthOption(
    val id: String,
    val label: String,
    val start: Date,
    val end: Date
)

const val SUBCATEGORY_OTHER = "Other"

object HistoryGrouping {
    const val SUBCATEGORY_OTHER = "Other"

    fun buildMonthOptionsForYear(
        referenceDate: Date,
        monthMode: MonthMode = MonthMode.CALENDAR,
        salaryPeriods: List<SalaryWalletPeriodSummary> = emptyList()
    ): List<HistoryMonthOption> {
        return if (monthMode == MonthMode.SALARY && salaryPeriods.isNotEmpty()) {
            salaryPeriods
                .sortedByDescending { it.start.time }
                .map { period ->
                    HistoryMonthOption(
                        id = "salary-${period.start.time}-${period.end.time}",
                        label = period.label,
                        start = period.start,
                        end = period.end
                    )
                }
        } else {
            val cal = Calendar.getInstance().apply { time = referenceDate }
            val year = cal.get(Calendar.YEAR)
            (11 downTo 0).map { monthIdx ->
                val mCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, monthIdx)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = DateUtils.getStartOfMonth(mCal.time)
                val end = DateUtils.getEndOfMonth(mCal.time)
                val label = DateUtils.formatMonthYear(start)
                HistoryMonthOption(
                    id = "month-$year-$monthIdx",
                    label = label,
                    start = start,
                    end = end
                )
            }
        }
    }

    fun group(
        transactions: List<Transaction>,
        period: HistoryPeriod,
        typeFilter: TransactionTypeFilter,
        categoryIdFilter: Long? = null,
        subCategoryFilter: String? = null,
        sortOrder: TransactionSortOrder = TransactionSortOrder.DESC,
        reimbursedExpenseIds: Set<Long> = emptySet(),
        monthMode: MonthMode = MonthMode.CALENDAR,
        salaryPeriods: List<SalaryWalletPeriodSummary> = emptyList(),
        splitsMap: Map<Long, List<TransactionSplit>> = emptyMap(),
        monthOptionFilter: HistoryMonthOption? = null,
        excludedCategoryIds: Set<Long> = emptySet(),
        excludedSubCategories: Set<String> = emptySet()
    ): List<HistorySection> {
        val baseTransactions = if (period == HistoryPeriod.YEAR && monthOptionFilter != null) {
            transactions.filter { it.date.time in monthOptionFilter.start.time..monthOptionFilter.end.time }
        } else {
            transactions
        }

        var filtered = when (typeFilter) {
            TransactionTypeFilter.ALL -> baseTransactions
            TransactionTypeFilter.INCOME -> baseTransactions.filter { it.type == TransactionType.INCOME }
            TransactionTypeFilter.EXPENSE -> baseTransactions.filter { it.type == TransactionType.EXPENSE }
            TransactionTypeFilter.WALLET -> baseTransactions.filter { it.type == TransactionType.WALLET_MOVE }
            TransactionTypeFilter.OWED -> baseTransactions.filter {
                it.type == TransactionType.EXPENSE &&
                    it.awaitingReimbursement &&
                    it.id !in reimbursedExpenseIds
            }
        }

        if (categoryIdFilter != null) {
            filtered = filtered.filter { it.categoryId == categoryIdFilter }
        }

        if (excludedCategoryIds.isNotEmpty()) {
            filtered = filtered.filter { it.categoryId !in excludedCategoryIds }
        }

        if (!subCategoryFilter.isNullOrBlank()) {
            filtered = if (subCategoryFilter.equals(SUBCATEGORY_OTHER, ignoreCase = true)) {
                filtered.filter { txn ->
                    val splits = splitsMap[txn.id].orEmpty()
                    if (splits.isNotEmpty()) {
                        splits.any { it.subCategoryName.isNullOrBlank() || it.subCategoryName.equals(SUBCATEGORY_OTHER, ignoreCase = true) }
                    } else {
                        txn.subDescription.isNullOrBlank() || txn.subDescription.equals(SUBCATEGORY_OTHER, ignoreCase = true)
                    }
                }
            } else {
                filtered.filter { txn ->
                    val splits = splitsMap[txn.id].orEmpty()
                    if (splits.isNotEmpty()) {
                        splits.any { it.subCategoryName.equals(subCategoryFilter, ignoreCase = true) }
                    } else {
                        txn.subDescription.equals(subCategoryFilter, ignoreCase = true)
                    }
                }
            }
        }

        if (excludedSubCategories.isNotEmpty()) {
            val lowerExcluded = excludedSubCategories.map { it.trim().lowercase() }.toSet()
            filtered = filtered.filterNot { txn ->
                val splits = splitsMap[txn.id].orEmpty()
                if (splits.isNotEmpty()) {
                    splits.all {
                        val name = it.subCategoryName?.trim()?.lowercase() ?: SUBCATEGORY_OTHER.lowercase()
                        name in lowerExcluded
                    }
                } else {
                    val sub = txn.subDescription?.trim()?.lowercase() ?: SUBCATEGORY_OTHER.lowercase()
                    sub in lowerExcluded
                }
            }
        }

        if (filtered.isEmpty()) return emptyList()

        val expenseAmountFor: (Transaction) -> Double = { txn ->
            if (txn.type != TransactionType.EXPENSE) {
                0.0
            } else {
                val splits = splitsMap[txn.id].orEmpty()
                val hasSubFilter = !subCategoryFilter.isNullOrBlank()
                val hasSubExclusions = excludedSubCategories.isNotEmpty()
                if (splits.isNotEmpty() && (hasSubFilter || hasSubExclusions)) {
                    val lowerExcluded = excludedSubCategories.map { it.trim().lowercase() }.toSet()
                    splits.filter { split ->
                        val name = split.subCategoryName?.trim().orEmpty()
                        val normalized = name.lowercase().ifBlank { SUBCATEGORY_OTHER.lowercase() }
                        val matchesInclude = if (!hasSubFilter) {
                            true
                        } else if (subCategoryFilter.equals(SUBCATEGORY_OTHER, ignoreCase = true)) {
                            name.isBlank() || name.equals(SUBCATEGORY_OTHER, ignoreCase = true)
                        } else {
                            name.equals(subCategoryFilter, ignoreCase = true)
                        }
                        val notExcluded = !hasSubExclusions || normalized !in lowerExcluded
                        matchesInclude && notExcluded
                    }.sumOf { it.amount }
                } else {
                    txn.amount
                }
            }
        }

        return when (period) {
            HistoryPeriod.DAY -> listOf(buildDaySection(baseTransactions, filtered, sortOrder, expenseAmountFor))
            HistoryPeriod.WEEK -> buildDaySections(filtered, sortOrder, expenseAmountFor)
            HistoryPeriod.MONTH -> buildWeekSections(filtered, sortOrder, expenseAmountFor)
            HistoryPeriod.YEAR -> {
                if (monthMode == MonthMode.SALARY && salaryPeriods.isNotEmpty()) {
                    buildSalaryMonthSections(filtered, salaryPeriods, sortOrder, expenseAmountFor)
                } else {
                    buildMonthSections(filtered, sortOrder, expenseAmountFor)
                }
            }
        }
    }

    private fun buildDaySection(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder,
        expenseAmountFor: (Transaction) -> Double
    ): HistorySection {
        val referenceDate = filtered.firstOrNull()?.date ?: allTransactions.first().date
        val expenseTotal = filtered.sumOf(expenseAmountFor)

        val sortedTxns = if (sortOrder == TransactionSortOrder.DESC) {
            filtered.sortedByDescending { it.date }
        } else {
            filtered.sortedBy { it.date }
        }

        return HistorySection(
            key = "day",
            title = DateUtils.formatDate(DateUtils.getStartOfDay(referenceDate)),
            expenseTotal = expenseTotal,
            transactions = sortedTxns
        )
    }

    private fun buildDaySections(
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder,
        expenseAmountFor: (Transaction) -> Double
    ): List<HistorySection> {
        val filteredByDay = filtered.groupBy { DateUtils.getDayKey(it.date) }

        val sortedKeys = if (sortOrder == TransactionSortOrder.DESC) {
            filteredByDay.keys.sortedDescending()
        } else {
            filteredByDay.keys.sorted()
        }

        return sortedKeys.map { dayKey ->
            val sectionTransactions = filteredByDay[dayKey].orEmpty()
            val expenseTotal = sectionTransactions.sumOf(expenseAmountFor)

            val sortedTxns = if (sortOrder == TransactionSortOrder.DESC) {
                sectionTransactions.sortedByDescending { it.date }
            } else {
                sectionTransactions.sortedBy { it.date }
            }

            HistorySection(
                key = "day-$dayKey",
                title = DateUtils.formatDate(Date(dayKey)),
                expenseTotal = expenseTotal,
                transactions = sortedTxns
            )
        }
    }

    private fun buildWeekSections(
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder,
        expenseAmountFor: (Transaction) -> Double
    ): List<HistorySection> {
        val filteredByWeek = filtered.groupBy { DateUtils.getWeekKey(it.date) }

        val sortedKeys = if (sortOrder == TransactionSortOrder.DESC) {
            filteredByWeek.keys.sortedDescending()
        } else {
            filteredByWeek.keys.sorted()
        }

        return sortedKeys.map { weekKey ->
            val sectionTransactions = filteredByWeek[weekKey].orEmpty()
            val expenseTotal = sectionTransactions.sumOf(expenseAmountFor)

            val sortedTxns = if (sortOrder == TransactionSortOrder.DESC) {
                sectionTransactions.sortedByDescending { it.date }
            } else {
                sectionTransactions.sortedBy { it.date }
            }

            HistorySection(
                key = "week-$weekKey",
                title = DateUtils.formatWeekRange(Date(weekKey)),
                expenseTotal = expenseTotal,
                transactions = sortedTxns
            )
        }
    }

    private fun buildMonthSections(
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder,
        expenseAmountFor: (Transaction) -> Double
    ): List<HistorySection> {
        val filteredByMonth = filtered.groupBy { DateUtils.getMonthKey(it.date) }

        val sortedKeys = if (sortOrder == TransactionSortOrder.DESC) {
            filteredByMonth.keys.sortedDescending()
        } else {
            filteredByMonth.keys.sorted()
        }

        return sortedKeys.map { monthKey ->
            val sectionTransactions = filteredByMonth[monthKey].orEmpty()
            val expenseTotal = sectionTransactions.sumOf(expenseAmountFor)

            val sortedTxns = if (sortOrder == TransactionSortOrder.DESC) {
                sectionTransactions.sortedByDescending { it.date }
            } else {
                sectionTransactions.sortedBy { it.date }
            }

            HistorySection(
                key = "month-$monthKey",
                title = DateUtils.formatMonthYear(DateUtils.dateFromMonthKey(monthKey)),
                expenseTotal = expenseTotal,
                transactions = sortedTxns
            )
        }
    }

    private fun buildSalaryMonthSections(
        filtered: List<Transaction>,
        salaryPeriods: List<SalaryWalletPeriodSummary>,
        sortOrder: TransactionSortOrder,
        expenseAmountFor: (Transaction) -> Double
    ): List<HistorySection> {
        val sortedPeriods = if (sortOrder == TransactionSortOrder.DESC) {
            salaryPeriods.sortedByDescending { it.start }
        } else {
            salaryPeriods.sortedBy { it.start }
        }

        val sections = mutableListOf<HistorySection>()

        sortedPeriods.forEachIndexed { index, period ->
            val matchingFiltered = filtered.filter { txn ->
                txn.date >= period.start && txn.date <= period.end
            }
            if (matchingFiltered.isNotEmpty()) {
                val expenseTotal = matchingFiltered.sumOf(expenseAmountFor)

                val sortedTxns = if (sortOrder == TransactionSortOrder.DESC) {
                    matchingFiltered.sortedByDescending { it.date }
                } else {
                    matchingFiltered.sortedBy { it.date }
                }

                sections.add(
                    HistorySection(
                        key = "salary-period-$index",
                        title = period.label,
                        expenseTotal = expenseTotal,
                        transactions = sortedTxns
                    )
                )
            }
        }

        // Catch any transactions outside defined salary periods
        val outsideFiltered = filtered.filter { txn ->
            sortedPeriods.none { period -> txn.date >= period.start && txn.date <= period.end }
        }
        if (outsideFiltered.isNotEmpty()) {
            val outsideByMonth = outsideFiltered.groupBy { DateUtils.getMonthKey(it.date) }
            val sortedMonthKeys = if (sortOrder == TransactionSortOrder.DESC) {
                outsideByMonth.keys.sortedDescending()
            } else {
                outsideByMonth.keys.sorted()
            }
            sortedMonthKeys.forEach { monthKey ->
                val txns = outsideByMonth[monthKey].orEmpty()
                val expenseTotal = txns.sumOf(expenseAmountFor)

                val sortedTxns = if (sortOrder == TransactionSortOrder.DESC) {
                    txns.sortedByDescending { it.date }
                } else {
                    txns.sortedBy { it.date }
                }

                sections.add(
                    HistorySection(
                        key = "salary-outside-$monthKey",
                        title = DateUtils.formatMonthYear(DateUtils.dateFromMonthKey(monthKey)),
                        expenseTotal = expenseTotal,
                        transactions = sortedTxns
                    )
                )
            }
        }

        return sections
    }
}
