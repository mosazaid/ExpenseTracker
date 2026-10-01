package com.example.expensetracker.domain

import java.util.Calendar
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PacingAnalysis(
    val dayOfPeriod: Int,
    val totalPeriodDays: Int,
    val daysRemaining: Int,
    val timeElapsedRatio: Float,
    val budgetConsumedRatio: Float,
    val velocityRatio: Float, // > 1.0 means spending faster than time elapsed
    val dailySpendRate: Double,
    val safeDailySpendRemaining: Double,
    val projectedCycleSpend: Double,
    val projectedDifference: Double, // positive = surplus, negative = deficit
    val status: PacingStatus,
    val adviceHeadline: String,
    val adviceMessage: String,
    val baselineBudget: Double,
    val isEarlyCycle: Boolean = false,
    val cycleLabel: String? = null
)

enum class PacingStatus {
    SUPER_SAVER,
    OPTIMAL,
    MODERATE_BURN,
    HIGH_BURN,
    DEFICIT_CRITICAL
}

object SpendingPacingCalculator {

    fun analyze(
        totalIncome: Double,
        totalExpense: Double,
        currentDate: Date = Date(),
        periodStartDate: Date? = null,
        periodEndDate: Date? = null,
        cycleLabel: String? = null
    ): PacingAnalysis {
        // 1. Calculate Period Boundaries (Salary or Calendar)
        val (totalPeriodDays, dayOfPeriod, daysRemaining) = if (periodStartDate != null && periodEndDate != null) {
            val startMs = periodStartDate.time
            val endMs = periodEndDate.time
            val currentMs = currentDate.time.coerceIn(startMs, endMs)

            val totalDays = max(1, ((endMs - startMs) / (1000L * 60 * 60 * 24)).toInt() + 1)
            val elapsed = max(1, ((currentMs - startMs) / (1000L * 60 * 60 * 24)).toInt() + 1).coerceIn(1, totalDays)
            val remaining = max(1, totalDays - elapsed)
            Triple(totalDays, elapsed, remaining)
        } else {
            val cal = Calendar.getInstance().apply { time = currentDate }
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDays)
            val remaining = max(1, maxDays - currentDay)
            Triple(maxDays, currentDay, remaining)
        }

        val timeElapsedRatio = (dayOfPeriod.toFloat() / totalPeriodDays.toFloat()).coerceIn(0.01f, 1f)

        // Baseline budget is total income, or fallback to sensible floor if income not yet entered
        val baselineBudget = if (totalIncome > 0) totalIncome else (totalExpense * 1.15).coerceAtLeast(100.0)
        val budgetConsumedRatio = if (baselineBudget > 0) (totalExpense / baselineBudget).toFloat() else 0f
        val velocityRatio = if (timeElapsedRatio > 0) (budgetConsumedRatio / timeElapsedRatio) else 1f

        val dailySpendRate = totalExpense / max(1, dayOfPeriod)
        val remainingBudget = max(0.0, baselineBudget - totalExpense)
        val safeDailySpendRemaining = remainingBudget / daysRemaining

        val isEarlyCycle = dayOfPeriod <= 5

        // Realistic Projected Spend calculation:
        // In early cycle (first 5 days), lump sums (rent, tuition, monthly grocery stock) are normal.
        // Linearly multiplying day 1 or 2 by 30 produces absurd projections (e.g. $700 on day 1 -> $21,000 spend).
        // Instead, we project remaining days based on remaining safe allowance, or blend if early cycle.
        val projectedCycleSpend: Double
        val projectedDifference: Double

        if (totalExpense >= baselineBudget) {
            projectedCycleSpend = totalExpense + (dailySpendRate * daysRemaining)
            projectedDifference = baselineBudget - projectedCycleSpend
        } else if (isEarlyCycle) {
            // Early cycle damping: assume remaining days stabilize toward the safe daily allowance
            val blendedDailyFuture = (dailySpendRate * 0.2) + (safeDailySpendRemaining * 0.8)
            projectedCycleSpend = min(baselineBudget * 1.5, totalExpense + (blendedDailyFuture * daysRemaining))
            projectedDifference = baselineBudget - projectedCycleSpend
        } else {
            // Mid to late cycle: standard run-rate extrapolation
            projectedCycleSpend = totalExpense + (dailySpendRate * daysRemaining)
            projectedDifference = baselineBudget - projectedCycleSpend
        }

        val status = when {
            totalExpense >= baselineBudget -> PacingStatus.DEFICIT_CRITICAL
            isEarlyCycle && velocityRatio > 1.25f -> PacingStatus.MODERATE_BURN // Keep non-critical for early lump sums
            velocityRatio > 1.35f -> PacingStatus.HIGH_BURN
            velocityRatio > 1.10f -> PacingStatus.MODERATE_BURN
            velocityRatio in 0.50f..1.10f -> PacingStatus.OPTIMAL
            else -> PacingStatus.SUPER_SAVER
        }

        val adviceHeadline = when {
            status == PacingStatus.DEFICIT_CRITICAL -> "Deficit Warning Alert"
            isEarlyCycle && budgetConsumedRatio > 0.4f -> "Initial Upfront Outlays (${(budgetConsumedRatio * 100).roundToInt()}% of Budget)"
            status == PacingStatus.HIGH_BURN -> "High Spending Velocity (${String.format(java.util.Locale.US, "%.1fx", velocityRatio)})"
            status == PacingStatus.MODERATE_BURN -> "Pacing Slightly Elevated"
            status == PacingStatus.OPTIMAL -> "Healthy Spending Rhythm"
            else -> "Super Saver Pace"
        }

        val adviceMessage = when {
            status == PacingStatus.DEFICIT_CRITICAL ->
                "Expenses have reached ${(budgetConsumedRatio * 100).roundToInt()}% of income. Limit further outlays to essentials for the remaining $daysRemaining days."
            isEarlyCycle && budgetConsumedRatio > 0.4f ->
                "Major upfront payments (rent, bills, groceries) are normal at cycle start (Day $dayOfPeriod of $totalPeriodDays). Target spending around ${String.format(java.util.Locale.US, "%.2f", safeDailySpendRemaining)}/day for the remaining $daysRemaining days to stay on track."
            status == PacingStatus.HIGH_BURN ->
                "You've consumed ${(budgetConsumedRatio * 100).roundToInt()}% of your budget in ${(timeElapsedRatio * 100).roundToInt()}% of the cycle. Recommended safe daily spend: ${String.format(java.util.Locale.US, "%.2f", safeDailySpendRemaining)}."
            status == PacingStatus.MODERATE_BURN ->
                "Spending is slightly ahead of schedule. Keeping daily spending near ${String.format(java.util.Locale.US, "%.2f", safeDailySpendRemaining)} will ensure a comfortable month-end surplus."
            status == PacingStatus.OPTIMAL ->
                "Balanced pacing. You are on track with $daysRemaining days remaining in the cycle."
            else ->
                "Excellent financial restraint! Projected monthly surplus is on track to reach ${String.format(java.util.Locale.US, "%.2f", max(0.0, projectedDifference))}."
        }

        return PacingAnalysis(
            dayOfPeriod = dayOfPeriod,
            totalPeriodDays = totalPeriodDays,
            daysRemaining = daysRemaining,
            timeElapsedRatio = timeElapsedRatio,
            budgetConsumedRatio = budgetConsumedRatio.coerceIn(0f, 2f),
            velocityRatio = velocityRatio,
            dailySpendRate = dailySpendRate,
            safeDailySpendRemaining = safeDailySpendRemaining,
            projectedCycleSpend = projectedCycleSpend,
            projectedDifference = projectedDifference,
            status = status,
            adviceHeadline = adviceHeadline,
            adviceMessage = adviceMessage,
            baselineBudget = baselineBudget,
            isEarlyCycle = isEarlyCycle,
            cycleLabel = cycleLabel
        )
    }
}
