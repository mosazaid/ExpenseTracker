package com.example.expensetracker.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class SpendingPacingCalculatorTest {

    @Test
    fun testPacingAnalysis_SuperSaver() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        }
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 1000.0,
            totalExpense = 100.0,
            currentDate = cal.time
        )

        assertEquals(15, analysis.dayOfPeriod)
        assertEquals(31, analysis.totalPeriodDays)
        assertEquals(16, analysis.daysRemaining)
        assertTrue(analysis.safeDailySpendRemaining > 50.0)
        assertEquals(PacingStatus.SUPER_SAVER, analysis.status)
        assertTrue(analysis.projectedDifference > 0)
    }

    @Test
    fun testPacingAnalysis_HighBurnRate() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        }
        // Consumed 50% of income by day 10 of 31 (velocity ~1.55x)
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 1000.0,
            totalExpense = 500.0,
            currentDate = cal.time
        )

        assertEquals(10, analysis.dayOfPeriod)
        assertTrue(analysis.velocityRatio > 1.35f)
        assertEquals(PacingStatus.HIGH_BURN, analysis.status)
        assertTrue(analysis.safeDailySpendRemaining < 25.0)
    }

    @Test
    fun testPacingAnalysis_DeficitCritical() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 20, 12, 0, 0)
        }
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 500.0,
            totalExpense = 650.0,
            currentDate = cal.time
        )

        assertEquals(PacingStatus.DEFICIT_CRITICAL, analysis.status)
        assertEquals(0.0, analysis.safeDailySpendRemaining, 0.001)
    }

    @Test
    fun testPacingAnalysis_ZeroIncomeFallback() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 5, 12, 0, 0)
        }
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 0.0,
            totalExpense = 50.0,
            currentDate = cal.time
        )

        assertNotNull(analysis)
        assertTrue(analysis.dailySpendRate > 0)
    }
}
