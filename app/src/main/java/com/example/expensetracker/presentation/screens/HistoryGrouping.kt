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

data class HistorySection(
    val key: String,
    val title: String,
    val expenseTotal: Double,
    val transactions: List<Transaction>
)

object HistoryGrouping {

    fun group(
        transactions: List<Transaction>,
        period: HistoryPeriod,
        typeFilter: TransactionTypeFilter,
        categoryIdFilter: Long? = null,
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

        if (filtered.isEmpty()) return emptyList()

        return when (period) {
            HistoryPeriod.DAY -> listOf(buildDaySection(transactions, filtered))
            HistoryPeriod.WEEK -> buildDaySections(transactions, filtered)
            HistoryPeriod.MONTH -> buildWeekSections(transactions, filtered)
            HistoryPeriod.YEAR -> {
                if (monthMode == MonthMode.SALARY && salaryPeriods.isNotEmpty()) {
                    buildSalaryMonthSections(transactions, filtered, salaryPeriods)
                } else {
                    buildMonthSections(transactions, filtered)
                }
            }
        }
    }

    private fun buildDaySection(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>
    ): HistorySection {
        val referenceDate = filtered.firstOrNull()?.date ?: allTransactions.first().date
        val expenseTotal = allTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return HistorySection(
            key = "day",
            title = DateUtils.formatDate(DateUtils.getStartOfDay(referenceDate)),
            expenseTotal = expenseTotal,
            transactions = filtered.sortedBy { it.date }
        )
    }

    private fun buildDaySections(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>
    ): List<HistorySection> {
        val filteredByDay = filtered.groupBy { DateUtils.getDayKey(it.date) }
        val allByDay = allTransactions.groupBy { DateUtils.getDayKey(it.date) }

        return filteredByDay.keys
            .sorted()
            .map { dayKey ->
                val sectionTransactions = filteredByDay[dayKey].orEmpty()
                val expenseTotal = allByDay[dayKey]
                    .orEmpty()
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                HistorySection(
                    key = "day-$dayKey",
                    title = DateUtils.formatDate(Date(dayKey)),
                    expenseTotal = expenseTotal,
                    transactions = sectionTransactions.sortedBy { it.date }
                )
            }
    }

    private fun buildWeekSections(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>
    ): List<HistorySection> {
        val filteredByWeek = filtered.groupBy { DateUtils.getWeekKey(it.date) }
        val allByWeek = allTransactions.groupBy { DateUtils.getWeekKey(it.date) }

        return filteredByWeek.keys
            .sorted()
            .map { weekKey ->
                val sectionTransactions = filteredByWeek[weekKey].orEmpty()
                val expenseTotal = allByWeek[weekKey]
                    .orEmpty()
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                HistorySection(
                    key = "week-$weekKey",
                    title = DateUtils.formatWeekRange(Date(weekKey)),
                    expenseTotal = expenseTotal,
                    transactions = sectionTransactions.sortedBy { it.date }
                )
            }
    }

    private fun buildMonthSections(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>
    ): List<HistorySection> {
        val filteredByMonth = filtered.groupBy { DateUtils.getMonthKey(it.date) }
        val allByMonth = allTransactions.groupBy { DateUtils.getMonthKey(it.date) }

        return filteredByMonth.keys
            .sorted()
            .map { monthKey ->
                val sectionTransactions = filteredByMonth[monthKey].orEmpty()
                val expenseTotal = allByMonth[monthKey]
                    .orEmpty()
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }

                HistorySection(
                    key = "month-$monthKey",
                    title = DateUtils.formatMonthYear(DateUtils.dateFromMonthKey(monthKey)),
                    expenseTotal = expenseTotal,
                    transactions = sectionTransactions.sortedBy { it.date }
                )
            }
    }

    private fun buildSalaryMonthSections(
        allTransactions: List<Transaction>,
        filtered: List<Transaction>,
        salaryPeriods: List<SalaryWalletPeriodSummary>
    ): List<HistorySection> {
        val sortedPeriods = salaryPeriods.sortedBy { it.start }
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

                sections.add(
                    HistorySection(
                        key = "salary-period-$index",
                        title = period.label,
                        expenseTotal = expenseTotal,
                        transactions = matchingFiltered.sortedBy { it.date }
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
            outsideByMonth.keys.sorted().forEach { monthKey ->
                val txns = outsideByMonth[monthKey].orEmpty()
                val expenseTotal = allExpense
                    .filter { txn -> txns.any { it.id == txn.id } }
                    .sumOf { it.amount }
                sections.add(
                    HistorySection(
                        key = "salary-outside-$monthKey",
                        title = DateUtils.formatMonthYear(DateUtils.dateFromMonthKey(monthKey)),
                        expenseTotal = expenseTotal,
                        transactions = txns.sortedBy { it.date }
                    )
                )
            }
        }

        return sections
    }
}
