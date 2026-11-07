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

    @Test
    fun testPacingAnalysis_WithWalletReserved() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 12, 0, 0)
        }
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 0, 0, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 23, 23, 59, 59)
        }

        // Total income 1700, wallet 1160, bank+cash spendable = 540, expense = 150
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 1700.0,
            totalExpense = 150.0,
            walletReserved = 1160.0,
            currentDate = cal.time,
            periodStartDate = startCal.time,
            periodEndDate = endCal.time
        )

        assertEquals(540.0, analysis.baselineBudget, 0.001)
        assertEquals(30, analysis.totalPeriodDays) // Sep 24 to Oct 23 is 30 days
        assertEquals(9, analysis.dayOfPeriod)      // Day 9
        assertEquals(21, analysis.daysRemaining)   // 21 days remaining
        // remaining budget is 540 - 150 = 390. safe daily spend = 390 / 21 ≈ 18.57
        assertEquals(390.0 / 21, analysis.safeDailySpendRemaining, 0.01)
        assertTrue("Safe daily spend should be around 18.57, not hundreds", analysis.safeDailySpendRemaining < 30.0)
    }

    @Test
    fun testPacingAnalysis_WithCarriedForwardBalance() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        }
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
        }

        val salaryIncome = 1000.0
        val carriedForward = 250.0
        val walletReserved = 150.0
        val expense = 200.0

        // FinancialInsightsCard computes totalIncome = salaryIncome + carriedForward
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = salaryIncome + carriedForward,
            totalExpense = expense,
            walletReserved = walletReserved,
            currentDate = cal.time,
            periodStartDate = startCal.time,
            periodEndDate = endCal.time
        )

        // Baseline = 1000 + 250 - 150 = 1100.0
        assertEquals(1100.0, analysis.baselineBudget, 0.001)
        assertEquals(31, analysis.totalPeriodDays)
        assertEquals(10, analysis.dayOfPeriod)
        assertEquals(21, analysis.daysRemaining)

        // Remaining = 1100 - 200 = 900.0
        // Safe daily spend = 900 / 21 ≈ 42.857
        assertEquals(900.0 / 21, analysis.safeDailySpendRemaining, 0.01)
        // Velocity = (200 / 1100) / (10 / 31) ≈ 0.56, which is in 0.50..1.10 range -> OPTIMAL
        assertEquals(PacingStatus.OPTIMAL, analysis.status)
    }

    @Test
    fun testPacingAnalysis_WithCurrentAvailableBalance_PreventsFalseDeficit() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 12, 0, 0)
        }
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
        }

        // Scenario: User hasn't recorded new salary yet (income = 0), but has ~500 JOD in real available cash+bank.
        // Expenses are 105 JOD.
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 0.0,
            totalExpense = 105.0,
            currentAvailableBalance = 495.0,
            walletReserved = 0.0,
            currentDate = cal.time,
            periodStartDate = startCal.time,
            periodEndDate = endCal.time
        )

        // Baseline must be 495.0 (real available liquid funds), NOT 100.0 (arbitrary floor)
        assertEquals(495.0, analysis.baselineBudget, 0.001)
        // Consumed ratio should be 105 / 495 ≈ 21.2%, NOT 105%
        assertEquals(105.0 / 495.0, analysis.budgetConsumedRatio.toDouble(), 0.01)
        // Must NOT trigger DEFICIT_CRITICAL alert
        assertNotEquals(PacingStatus.DEFICIT_CRITICAL, analysis.status)
        assertTrue(analysis.projectedDifference > 0 || analysis.status == PacingStatus.OPTIMAL || analysis.status == PacingStatus.SUPER_SAVER)
        assertTrue(analysis.safeDailySpendRemaining > 15.0)
    }

    @Test
    fun testPacingAnalysis_VaultAndWalletSpendingExcluded_ReflectsRealLiquidFunds() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 12, 0, 0)
        }
        val startCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 23, 59, 59)
        }

        // User logged small side income 100 JOD, has 500 JOD liquid funds, and 105 JOD expenses
        val analysis = SpendingPacingCalculator.analyze(
            totalIncome = 100.0,
            totalExpense = 105.0,
            currentAvailableBalance = 500.0,
            walletReserved = 0.0,
            currentDate = cal.time,
            periodStartDate = startCal.time,
            periodEndDate = endCal.time
        )

        // Baseline takes max(100, 500) = 500
        assertEquals(500.0, analysis.baselineBudget, 0.001)
        assertEquals(0.21f, analysis.budgetConsumedRatio, 0.01f)
        assertNotEquals(PacingStatus.DEFICIT_CRITICAL, analysis.status)
    }
}
