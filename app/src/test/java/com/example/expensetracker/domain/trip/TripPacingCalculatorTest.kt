package com.example.expensetracker.domain.trip

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.database.entities.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TripPacingCalculatorTest {

    private val dayMillis = 24L * 60 * 60 * 1000

    @Test
    fun testPacingOnTrack() {
        val now = Date()
        val start = Date(now.time - 2 * dayMillis)
        val end = Date(now.time + 5 * dayMillis) // total 8 days

        val trip = Trip(
            id = 1L,
            title = "Dubai Trip",
            destination = "UAE",
            baseCurrency = "AED",
            totalBudget = 4000.0,
            startDate = start,
            endDate = end,
            isActive = true
        )

        // 3 days passed, 5 days remaining. Budget: 4000 AED (~500 AED/day).
        // Spent so far: 1000 AED (average ~333/day < 500/day allowance).
        val transactions = listOf(
            Transaction(
                id = 101L,
                amount = 100.0, // Home JOD
                originalAmount = 500.0, // AED
                currency = "AED",
                description = "Hotel breakfast",
                date = start,
                type = TransactionType.EXPENSE,
                accountType = AccountType.BANK,
                tripId = 1L
            ),
            Transaction(
                id = 102L,
                amount = 100.0,
                originalAmount = 500.0,
                currency = "AED",
                description = "Taxi & Metro",
                date = now,
                type = TransactionType.EXPENSE,
                accountType = AccountType.BANK,
                tripId = 1L
            )
        )

        val result = TripPacingCalculator.calculate(trip, transactions, currentDate = now)

        assertEquals(1000.0, result.totalSpentTripCurrency, 0.01)
        assertEquals(3000.0, result.remainingBudget, 0.01)
        assertEquals(TripPacingStatus.ON_TRACK, result.pacingStatus)
        assertTrue("Safe daily spend should be > 500", result.safeDailySpendRemaining >= 500.0)
    }

    @Test
    fun testPacingOverspendingAndExceeded() {
        val now = Date()
        val start = Date(now.time - 4 * dayMillis)
        val end = Date(now.time + 1 * dayMillis)

        val trip = Trip(
            id = 2L,
            title = "Istanbul Trip",
            destination = "Turkey",
            baseCurrency = "USD",
            totalBudget = 1000.0,
            startDate = start,
            endDate = end,
            isActive = true
        )

        // Spent 1200 USD on a 1000 USD budget -> EXCEEDED
        val transactions = listOf(
            Transaction(
                id = 201L,
                amount = 850.0,
                originalAmount = 1200.0,
                currency = "USD",
                description = "Shopping at Grand Bazaar",
                date = now,
                type = TransactionType.EXPENSE,
                accountType = AccountType.BANK,
                tripId = 2L
            )
        )

        val result = TripPacingCalculator.calculate(trip, transactions, currentDate = now)

        assertEquals(1200.0, result.totalSpentTripCurrency, 0.01)
        assertEquals(0.0, result.remainingBudget, 0.01)
        assertEquals(TripPacingStatus.EXCEEDED, result.pacingStatus)
    }
}

