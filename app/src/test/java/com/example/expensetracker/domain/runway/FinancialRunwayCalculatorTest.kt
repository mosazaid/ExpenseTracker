package com.example.expensetracker.domain.runway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialRunwayCalculatorTest {

    @Test
    fun testRunwayCalculation_Critical() {
        val liquidWealth = 200.0
        val monthlyBurn = 400.0 // 0.5 months of burn

        val result = FinancialRunwayCalculator.calculate(liquidWealth, monthlyBurn)

        assertEquals(0.5, result.runwayMonths, 0.01)
        assertEquals(15, result.daysOfSurvival)
        assertEquals(RunwayVulnerability.CRITICAL, result.vulnerability)
        assertEquals(1200.0, result.recommendedSafetyBuffer, 0.01)
    }

    @Test
    fun testRunwayCalculation_StableAndFortified() {
        val liquidWealth = 2400.0
        val monthlyBurn = 500.0 // 4.8 months of burn

        val stableResult = FinancialRunwayCalculator.calculate(liquidWealth, monthlyBurn)
        assertEquals(4.8, stableResult.runwayMonths, 0.01)
        assertEquals(RunwayVulnerability.STABLE, stableResult.vulnerability)

        val fortifiedResult = FinancialRunwayCalculator.calculate(4000.0, 500.0) // 8.0 months
        assertEquals(8.0, fortifiedResult.runwayMonths, 0.01)
        assertEquals(RunwayVulnerability.FORTIFIED, fortifiedResult.vulnerability)
    }

    @Test
    fun testSimulationWithEmergencyExpense() {
        val currentLiquid = 1500.0
        val monthlyBurn = 500.0 // Baseline: 3.0 months
        val shock = 600.0 // Post-shock liquid: 900.0 -> 1.8 months

        val simulatedResult = FinancialRunwayCalculator.simulate(
            currentLiquidWealth = currentLiquid,
            monthlyBurn = monthlyBurn,
            emergencyExpense = shock
        )

        assertEquals(1.8, simulatedResult.runwayMonths, 0.01)
        assertEquals(RunwayVulnerability.VULNERABLE, simulatedResult.vulnerability)
    }
}

