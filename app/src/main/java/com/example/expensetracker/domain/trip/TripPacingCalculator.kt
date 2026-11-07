package com.example.expensetracker.domain.trip

import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.database.entities.Trip
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max

enum class TripPacingStatus {
    ON_TRACK,
    CAUTION,
    OVERSPENDING,
    EXCEEDED
}

data class TripPacingResult(
    val trip: Trip,
    val totalDays: Int,
    val daysPassed: Int,
    val daysRemaining: Int,
    val totalBudget: Double,
    val totalSpentTripCurrency: Double,
    val totalSpentHomeCurrency: Double,
    val remainingBudget: Double,
    val dailyBudgetTotal: Double,
    val safeDailySpendRemaining: Double,
    val spendPercentage: Double,
    val pacingStatus: TripPacingStatus,
    val expenseTransactionCount: Int
)

object TripPacingCalculator {

    fun calculate(
        trip: Trip,
        transactions: List<Transaction>,
        currentDate: Date = Date()
    ): TripPacingResult {
        val startMillis = trip.startDate.time
        val endMillis = trip.endDate.time
        val currentMillis = currentDate.time

        val rawTotalDays = TimeUnit.MILLISECONDS.toDays(max(0L, endMillis - startMillis)).toInt() + 1
        val totalDays = max(1, rawTotalDays)

        val daysFromStart = TimeUnit.MILLISECONDS.toDays(max(0L, currentMillis - startMillis)).toInt() + 1
        val daysPassed = daysFromStart.coerceIn(1, totalDays)

        val daysRemaining = max(0, totalDays - daysPassed)

        val tripExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && !it.excludeFromMonthExpenses
        }

        var totalSpentTrip = 0.0
        var totalSpentHome = 0.0

        for (tx in tripExpenses) {
            totalSpentHome += tx.amount
            val original = tx.originalAmount
            if (original != null && original > 0) {
                totalSpentTrip += original
            } else {
                // If original amount was not recorded separately, use home amount
                totalSpentTrip += tx.amount
            }
        }

        val remainingBudget = max(0.0, trip.totalBudget - totalSpentTrip)
        val dailyBudgetTotal = trip.totalBudget / totalDays
        val safeDailySpendRemaining = if (daysRemaining > 0) {
            remainingBudget / daysRemaining
        } else {
            0.0
        }

        val spendPercentage = if (trip.totalBudget > 0) {
            (totalSpentTrip / trip.totalBudget) * 100.0
        } else {
            0.0
        }

        val pacingStatus = when {
            totalSpentTrip > trip.totalBudget -> TripPacingStatus.EXCEEDED
            safeDailySpendRemaining < dailyBudgetTotal * 0.70 -> TripPacingStatus.OVERSPENDING
            safeDailySpendRemaining < dailyBudgetTotal * 0.90 -> TripPacingStatus.CAUTION
            else -> TripPacingStatus.ON_TRACK
        }

        return TripPacingResult(
            trip = trip,
            totalDays = totalDays,
            daysPassed = daysPassed,
            daysRemaining = daysRemaining,
            totalBudget = trip.totalBudget,
            totalSpentTripCurrency = totalSpentTrip,
            totalSpentHomeCurrency = totalSpentHome,
            remainingBudget = remainingBudget,
            dailyBudgetTotal = dailyBudgetTotal,
            safeDailySpendRemaining = safeDailySpendRemaining,
            spendPercentage = spendPercentage,
            pacingStatus = pacingStatus,
            expenseTransactionCount = tripExpenses.size
        )
    }
}

