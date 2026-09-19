package com.example.expensetracker.presentation.screens

import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.SalaryWalletPeriodSummary
import com.example.expensetracker.presentation.theme.DateUtils
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

const val SUBCATEGORY_OTHER = "Other"

object HistoryGrouping {
    const val SUBCATEGORY_OTHER = "Other"

    fun group(
        transactions: List<Transaction>,
        period: HistoryPeriod,
        typeFilter: TransactionTypeFilter,
        categoryIdFilter: Long? = null,
        subCategoryFilter: String? = null,
        sortOrder: TransactionSortOrder = TransactionSortOrder.DESC,
        reimbursedExpenseIds: Set<Long> = emptySet(),
        monthMode: MonthMode = MonthMode.CALENDAR,
        salaryPeriods: List<SalaryWalletPeriodSummary> = emptyList()
    ): List<HistorySection> {
        var filtered = when (typeFilter) {
            TransactionTypeFilter.ALL -> transactions
            TransactionTypeFilter.INCOME -> transactions.filter { it.type == TransactionType.INCOME }
            TransactionTypeFilter.EXPENSE -> transactions.filter { it.type == TransactionType.EXPENSE }
            TransactionTypeFilter.WALLET -> transactions.filter { it.type == TransactionType.WALLET_MOVE }
            TransactionTypeFilter.OWED -> transactions.filter {
                it.type == TransactionType.EXPENSE &&
                    it.awaitingReimbursement &&
                    it.id !in reimbursedExpenseIds
            }
        }

        if (categoryIdFilter != null) {
            filtered = filtered.filter { it.categoryId == categoryIdFilter }
        }

        if (!subCategoryFilter.isNullOrBlank()) {
            filtered = if (subCategoryFilter.equals(SUBCATEGORY_OTHER, ignoreCase = true)) {
                filtered.filter { it.subDescription.isNullOrBlank() || it.subDescription.equals(SUBCATEGORY_OTHER, ignoreCase = true) }
            } else {
                filtered.filter { it.subDescription.equals(subCategoryFilter, ignoreCase = true) }
            }
        }

        if (filtered.isEmpty()) return emptyList()

        return when (period) {
            HistoryPeriod.DAY -> listOf(buildDaySection(transactions, filtered, sortOrder))
            HistoryPeriod.WEEK -> buildDaySections(transactions, filtered, sortOrder)
            HistoryPeriod.MONTH -> buildWeekSections(transactions, filtered, sortOrder)
            HistoryPeriod.YEAR -> {
                if (monthMode == MonthMode.SALARY && salaryPeriods.isNotEmpty()) {
                    buildSalaryMonthSections(transactions, filtered, salaryPeriods, sortOrder)
                } else {
                    buildMonthSections(transactions, filtered, sortOrder)
                }
            }
        }
    }

    private fun buildDaySection(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder
    ): HistorySection {
        val referenceDate = filtered.firstOrNull()?.date ?: allTransactions.first().date
        val expenseTotal = allTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

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
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder
    ): List<HistorySection> {
        val filteredByDay = filtered.groupBy { DateUtils.getDayKey(it.date) }
        val allByDay = allTransactions.groupBy { DateUtils.getDayKey(it.date) }

        val sortedKeys = if (sortOrder == TransactionSortOrder.DESC) {
            filteredByDay.keys.sortedDescending()
        } else {
            filteredByDay.keys.sorted()
        }

        return sortedKeys.map { dayKey ->
            val sectionTransactions = filteredByDay[dayKey].orEmpty()
            val expenseTotal = allByDay[dayKey]
                .orEmpty()
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

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
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder
    ): List<HistorySection> {
        val filteredByWeek = filtered.groupBy { DateUtils.getWeekKey(it.date) }
        val allByWeek = allTransactions.groupBy { DateUtils.getWeekKey(it.date) }

        val sortedKeys = if (sortOrder == TransactionSortOrder.DESC) {
            filteredByWeek.keys.sortedDescending()
        } else {
            filteredByWeek.keys.sorted()
        }

        return sortedKeys.map { weekKey ->
            val sectionTransactions = filteredByWeek[weekKey].orEmpty()
            val expenseTotal = allByWeek[weekKey]
                .orEmpty()
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

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
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        sortOrder: TransactionSortOrder
    ): List<HistorySection> {
        val filteredByMonth = filtered.groupBy { DateUtils.getMonthKey(it.date) }
        val allByMonth = allTransactions.groupBy { DateUtils.getMonthKey(it.date) }

        val sortedKeys = if (sortOrder == TransactionSortOrder.DESC) {
            filteredByMonth.keys.sortedDescending()
        } else {
            filteredByMonth.keys.sorted()
        }

        return sortedKeys.map { monthKey ->
            val sectionTransactions = filteredByMonth[monthKey].orEmpty()
            val expenseTotal = allByMonth[monthKey]
                .orEmpty()
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

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
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        salaryPeriods: List<SalaryWalletPeriodSummary>,
        sortOrder: TransactionSortOrder
    ): List<HistorySection> {
        val sortedPeriods = if (sortOrder == TransactionSortOrder.DESC) {
            salaryPeriods.sortedByDescending { it.start }
        } else {
            salaryPeriods.sortedBy { it.start }
        }
        val allExpense = allTransactions.filter { it.type == TransactionType.EXPENSE }

        val sections = mutableListOf<HistorySection>()

        sortedPeriods.forEachIndexed { index, period ->
            val matchingFiltered = filtered.filter { txn ->
                txn.date >= period.start && txn.date <= period.end
            }
            if (matchingFiltered.isNotEmpty()) {
                val expenseTotal = allExpense
                    .filter { txn -> txn.date >= period.start && txn.date <= period.end }
                    .sumOf { it.amount }

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
                val expenseTotal = allExpense
                    .filter { txn -> txns.any { it.id == txn.id } }
                    .sumOf { it.amount }

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
