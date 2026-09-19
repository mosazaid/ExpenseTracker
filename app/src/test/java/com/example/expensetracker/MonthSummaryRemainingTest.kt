package com.example.expensetracker

import com.example.expensetracker.domain.AccountBalances
import com.example.expensetracker.presentation.viewModel.HistoryViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthSummaryRemainingTest {

    @Test
    fun testRemainingBalanceBreakdown_sumsToTotalRemaining() {
        // Simulating the user's data:
        // Cash: 50.8, Bank: 4.22 -> Total: 55.02
        // Current month remaining income: 38.98
        // Previous month remaining: 55.02 - 38.98 = 16.04
        val totalCurrentBalances = 55.02
        val currentRemainingIncome = 38.98
        val previousMonthRemaining = totalCurrentBalances - currentRemainingIncome

        val state = HistoryViewModel.MonthSummaryUiState(
            monthIncome = 100.0,
            monthExpense = 61.02,
            monthNet = 38.98,
            walletBalance = 0.0,
            previousMonthRemaining = previousMonthRemaining,
            monthRemaining = currentRemainingIncome,
            totalRemaining = totalCurrentBalances,
            currentBalances = AccountBalances(cash = 50.8, bank = 4.22)
        )

        assertEquals(38.98, state.remainingIncome, 0.001)
        assertEquals(16.04, state.previousMonthRemaining, 0.001)
        assertEquals(55.02, state.totalRemaining, 0.001)
        assertEquals(state.totalRemaining, state.previousMonthRemaining + state.remainingIncome, 0.001)
        assertEquals(55.02, state.currentBalances.total, 0.001)
    }
}
