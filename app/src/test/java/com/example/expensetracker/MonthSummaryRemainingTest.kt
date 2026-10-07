package com.example.expensetracker

import com.example.expensetracker.domain.AccountBalances
import com.example.expensetracker.presentation.model.MonthSummaryUiState
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

        val state = MonthSummaryUiState(
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

    @Test
    fun testHeroAssetDistribution_withCumulativeWallet() {
        val cashBal = 200.0
        val bankBal = 800.0
        val walletBal = 500.0
        val state = MonthSummaryUiState(
            currentBalances = AccountBalances(cash = cashBal, bank = bankBal),
            cumulativeWalletBalance = walletBal
        )

        // Total available strictly liquid: Cash + Bank (excludes wallet)
        val totalAvailable = state.currentBalances.cash + state.currentBalances.bank
        assertEquals(1000.0, totalAvailable, 0.001)

        // Proportional asset distribution: Cash, Bank, and Wallet
        val totalAssets = state.currentBalances.cash + state.currentBalances.bank + state.cumulativeWalletBalance
        assertEquals(1500.0, totalAssets, 0.001)

        val cashRatio = state.currentBalances.cash / totalAssets
        val bankRatio = state.currentBalances.bank / totalAssets
        val walletRatio = state.cumulativeWalletBalance / totalAssets

        assertEquals(200.0 / 1500.0, cashRatio, 0.001)
        assertEquals(800.0 / 1500.0, bankRatio, 0.001)
        assertEquals(500.0 / 1500.0, walletRatio, 0.001)
    }
}
