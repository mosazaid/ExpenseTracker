package com.example.expensetracker.domain.runway

import kotlin.math.max

enum class RunwayVulnerability {
    CRITICAL,    // < 1 Month: extreme vulnerability
    VULNERABLE,  // 1–3 Months: fragile buffer
    STABLE,      // 3–6 Months: solid baseline
    FORTIFIED    // 6+ Months: rock-solid financial cushion
}

data class FinancialRunwayResult(
    val liquidWealth: Double,
    val monthlyEssentialBurn: Double,
    val runwayMonths: Double,
    val vulnerability: RunwayVulnerability,
    val daysOfSurvival: Int,
    val recommendedSafetyBuffer: Double // 3 months of essential burn
)

object FinancialRunwayCalculator {

    fun calculate(
        liquidWealth: Double,
        monthlyEssentialBurn: Double
    ): FinancialRunwayResult {
        val safeBurn = max(1.0, monthlyEssentialBurn)
        val safeLiquid = max(0.0, liquidWealth)
        val months = safeLiquid / safeBurn
        val days = (months * 30.0).toInt()

        val vulnerability = when {
            months < 1.0 -> RunwayVulnerability.CRITICAL
            months < 3.0 -> RunwayVulnerability.VULNERABLE
            months < 6.0 -> RunwayVulnerability.STABLE
            else -> RunwayVulnerability.FORTIFIED
        }

        val recommendedBuffer = safeBurn * 3.0

        return FinancialRunwayResult(
            liquidWealth = safeLiquid,
            monthlyEssentialBurn = monthlyEssentialBurn,
            runwayMonths = months,
            vulnerability = vulnerability,
            daysOfSurvival = days,
            recommendedSafetyBuffer = recommendedBuffer
        )
    }

    /**
     * Simulates what happens to the runway if a sudden emergency expense occurs or income drops.
     */
    fun simulate(
        currentLiquidWealth: Double,
        monthlyBurn: Double,
        emergencyExpense: Double,
        incomeLossPercent: Double = 1.0 // 1.0 = 100% loss of salary
    ): FinancialRunwayResult {
        val adjustedLiquid = max(0.0, currentLiquidWealth - emergencyExpense)
        return calculate(adjustedLiquid, monthlyBurn)
    }
}

