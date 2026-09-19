package com.example.expensetracker.presentation

import com.example.expensetracker.presentation.components.parseCategoryColor
import com.example.expensetracker.presentation.viewModel.CategoryExpenseShare
import com.example.expensetracker.presentation.viewModel.MonthlyBreakdown
import com.example.expensetracker.presentation.viewModel.ThreeMonthStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Date

class StatisticsCalculationsTest {

    @Test
    fun testMonthlyBreakdown_netBalanceCalculation() {
        val breakdown = MonthlyBreakdown(
            monthLabel = "Jul 2026",
            startDate = Date(1000L),
            endDate = Date(2000L),
            income = 2500.0,
            expense = 1200.0,
            wallet = 300.0,
            netBalance = 2500.0 - 1200.0 - 300.0,
            categoryBreakdown = emptyList()
        )

        assertEquals(1000.0, breakdown.netBalance, 0.001)
    }

    @Test
    fun testThreeMonthStats_aggregations() {
        val m1 = MonthlyBreakdown(
            monthLabel = "May 2026",
            startDate = Date(1000L),
            endDate = Date(2000L),
            income = 2000.0,
            expense = 1000.0,
            wallet = 100.0,
            netBalance = 900.0,
            categoryBreakdown = emptyList()
        )
        val m2 = MonthlyBreakdown(
            monthLabel = "Jun 2026",
            startDate = Date(3000L),
            endDate = Date(4000L),
            income = 2000.0,
            expense = 1200.0,
            wallet = 150.0,
            netBalance = 650.0,
            categoryBreakdown = emptyList()
        )
        val m3 = MonthlyBreakdown(
            monthLabel = "Jul 2026",
            startDate = Date(5000L),
            endDate = Date(6000L),
            income = 2400.0,
            expense = 800.0,
            wallet = 200.0,
            netBalance = 1400.0,
            categoryBreakdown = emptyList()
        )

        val months = listOf(m1, m2, m3)
        val totalInc = months.sumOf { it.income }
        val totalExp = months.sumOf { it.expense }
        val totalWal = months.sumOf { it.wallet }
        val avgExp = totalExp / months.size

        val stats = ThreeMonthStats(
            months = months,
            totalIncome = totalInc,
            totalExpense = totalExp,
            totalWallet = totalWal,
            averageMonthlyExpense = avgExp,
            combinedCategoryBreakdown = emptyList()
        )

        assertEquals(6400.0, stats.totalIncome, 0.001)
        assertEquals(3000.0, stats.totalExpense, 0.001)
        assertEquals(450.0, stats.totalWallet, 0.001)
        assertEquals(1000.0, stats.averageMonthlyExpense, 0.001)

        // Month-over-month calculation between m2 and m3 (800 vs 1200)
        val momChange = ((m3.expense - m2.expense) / m2.expense) * 100.0
        assertEquals(-33.333, momChange, 0.01)
    }

    @Test
    fun testCategoryExpenseShare_percentageDistribution() {
        val totalExpense = 500.0
        val cat1Amount = 250.0
        val cat2Amount = 150.0
        val cat3Amount = 100.0

        val share1 = CategoryExpenseShare(1L, "Food", "🍔", "#FF5722", cat1Amount, (cat1Amount / totalExpense).toFloat() * 100f)
        val share2 = CategoryExpenseShare(2L, "Transport", "🚗", "#2196F3", cat2Amount, (cat2Amount / totalExpense).toFloat() * 100f)
        val share3 = CategoryExpenseShare(3L, "Utilities", "💡", "#FFEB3B", cat3Amount, (cat3Amount / totalExpense).toFloat() * 100f)

        assertEquals(50.0f, share1.percentage, 0.001f)
        assertEquals(30.0f, share2.percentage, 0.001f)
        assertEquals(20.0f, share3.percentage, 0.001f)
        assertEquals(100.0f, share1.percentage + share2.percentage + share3.percentage, 0.001f)
    }
}
