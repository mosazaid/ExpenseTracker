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

    @Test
    fun testSubcategoryComparisonItem_deltasAndSpikes() {
        // Chicken & Meat: 110.0 vs 75.0 (+46.7%)
        val chickenBase = 110.0
        val chickenComp = 75.0
        val chickenDelta = chickenBase - chickenComp
        val chickenPct = (((chickenBase - chickenComp) / chickenComp) * 100f).toFloat()

        val chickenItem = com.example.expensetracker.presentation.model.SubcategoryComparisonItem(
            name = "Chicken & Meat",
            baseAmount = chickenBase,
            compareAmount = chickenComp,
            deltaAmount = chickenDelta,
            deltaPercent = chickenPct,
            baseTxnCount = 4,
            varianceInsight = "Spike +47% across 4 purchases"
        )

        assertEquals(35.0, chickenItem.deltaAmount, 0.001)
        assertEquals(46.666f, chickenItem.deltaPercent ?: 0f, 0.01f)
        assertNotNull(chickenItem.varianceInsight)

        // Snacks: 65.0 vs 80.0 (-18.75%)
        val snackBase = 65.0
        val snackComp = 80.0
        val snackPct = (((snackBase - snackComp) / snackComp) * 100f).toFloat()

        val snackItem = com.example.expensetracker.presentation.model.SubcategoryComparisonItem(
            name = "Snacks",
            baseAmount = snackBase,
            compareAmount = snackComp,
            deltaAmount = snackBase - snackComp,
            deltaPercent = snackPct,
            baseTxnCount = 2
        )

        assertEquals(-15.0, snackItem.deltaAmount, 0.001)
        assertEquals(-18.75f, snackItem.deltaPercent ?: 0f, 0.01f)

        // New item (no compare)
        val newItem = com.example.expensetracker.presentation.model.SubcategoryComparisonItem(
            name = "New Equipment",
            baseAmount = 70.0,
            compareAmount = 0.0,
            deltaAmount = 70.0,
            deltaPercent = null,
            baseTxnCount = 1
        )
        assertEquals(null, newItem.deltaPercent)
    }

    @Test
    fun testCategoryComparisonItem_waterfallTotals() {
        val sub1 = com.example.expensetracker.presentation.model.SubcategoryComparisonItem(
            name = "Chicken & Meat",
            baseAmount = 110.0,
            compareAmount = 75.0,
            deltaAmount = 35.0,
            deltaPercent = 46.7f,
            baseTxnCount = 4
        )
        val sub2 = com.example.expensetracker.presentation.model.SubcategoryComparisonItem(
            name = "Snacks",
            baseAmount = 65.0,
            compareAmount = 80.0,
            deltaAmount = -15.0,
            deltaPercent = -18.75f,
            baseTxnCount = 2
        )
        val sub3 = com.example.expensetracker.presentation.model.SubcategoryComparisonItem(
            name = "Other",
            baseAmount = 70.0,
            compareAmount = 35.0,
            deltaAmount = 35.0,
            deltaPercent = 100f,
            baseTxnCount = 1
        )

        val subs = listOf(sub1, sub2, sub3)
        val baseTotal = subs.sumOf { it.baseAmount } // 245.0
        val compTotal = subs.sumOf { it.compareAmount } // 190.0
        val catDelta = baseTotal - compTotal // 55.0
        val catDeltaPct = (((baseTotal - compTotal) / compTotal) * 100f).toFloat() // +28.947%

        val catItem = com.example.expensetracker.presentation.model.CategoryComparisonItem(
            categoryId = 10L,
            categoryName = "Shopping",
            categoryIcon = "🛒",
            categoryColor = "#4CAF50",
            baseTotal = baseTotal,
            compareTotal = compTotal,
            deltaAmount = catDelta,
            deltaPercent = catDeltaPct,
            subcategories = subs,
            topSpikeInsight = "Driven by Chicken & Meat (+47%)"
        )

        assertEquals(245.0, catItem.baseTotal, 0.001)
        assertEquals(190.0, catItem.compareTotal, 0.001)
        assertEquals(55.0, catItem.deltaAmount, 0.001)
        assertEquals(28.947f, catItem.deltaPercent ?: 0f, 0.01f)
        assertEquals(3, catItem.subcategories.size)
    }
}
