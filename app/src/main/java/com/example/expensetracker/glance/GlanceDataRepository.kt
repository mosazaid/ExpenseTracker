package com.example.expensetracker.glance

import android.content.Context
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PacingStatus
import com.example.expensetracker.domain.SpendingPacingCalculator
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.Date

object GlanceDataRepository {

    suspend fun getPaydayPacingData(context: Context): PaydayPacingWidgetData {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                GlanceWidgetEntryPoint::class.java
            )

            val balanceCalculator = entryPoint.balanceCalculator()
            val periodCalculator = entryPoint.periodCalculator()
            val walletCalculator = entryPoint.walletCalculator()
            val userPreferences = entryPoint.userPreferences()
            val transactionRepository = entryPoint.transactionRepository()

            val monthMode = userPreferences.monthMode.first()
            val now = Date()

            // Available liquid cash (Cash + Bank)
            val allTimeBalances = balanceCalculator.getAllTimeBalances()
            val liquidCash = allTimeBalances.total

            // Period bounds
            val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, now, monthMode)

            // Month summary financials
            val summary = balanceCalculator.buildMonthFinancialSummary(bounds.start, bounds.end)
            val walletBalance = if (monthMode == MonthMode.SALARY) {
                walletCalculator.getWalletBalance(bounds.start, bounds.end)
            } else 0.0

            // Carried forward savings
            val anchor = if (monthMode == MonthMode.SALARY) {
                periodCalculator.getCurrentPeriodAnchor(now)
            } else null
            val broughtForward = anchor?.carriedForwardBalance ?: 0.0

            // Pacing analysis
            val pacing = SpendingPacingCalculator.analyze(
                totalIncome = summary.periodIncome + broughtForward,
                totalExpense = summary.periodExpense,
                walletReserved = walletBalance,
                currentDate = now,
                periodStartDate = bounds.start,
                periodEndDate = bounds.end,
                cycleLabel = bounds.label
            )

            // Today's spending
            val startOfToday = DateUtils.getStartOfDay(now)
            val endOfToday = DateUtils.getEndOfDay(now)
            val todayTxns = transactionRepository.getTransactionsBetweenDatesSnapshot(startOfToday, endOfToday)
            val todaySpend = todayTxns.filter {
                it.type == TransactionType.EXPENSE &&
                it.accountType != AccountType.WALLET &&
                it.accountType != AccountType.DEPOSIT &&
                !it.excludeFromMonthExpenses
            }.sumOf { it.amount }

            val statusLabel = when (pacing.status) {
                PacingStatus.SUPER_SAVER -> "Super Saver"
                PacingStatus.OPTIMAL -> "On Track"
                PacingStatus.MODERATE_BURN -> "Moderate Burn"
                PacingStatus.HIGH_BURN -> "High Burn"
                PacingStatus.DEFICIT_CRITICAL -> "Deficit Alert"
            }

            PaydayPacingWidgetData(
                liquidCash = liquidCash,
                daysToPayday = pacing.daysRemaining,
                safeDailySpend = pacing.safeDailySpendRemaining,
                todaySpend = todaySpend,
                pacingStatus = pacing.status,
                pacingLabel = statusLabel,
                cycleLabel = bounds.label
            )
        } catch (_: Exception) {
            PaydayPacingWidgetData(
                liquidCash = 0.0,
                daysToPayday = 30,
                safeDailySpend = 0.0,
                todaySpend = 0.0,
                pacingStatus = PacingStatus.OPTIMAL,
                pacingLabel = "On Track",
                cycleLabel = ""
            )
        }
    }

    suspend fun getQuickAddData(context: Context): QuickAddWidgetData {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                GlanceWidgetEntryPoint::class.java
            )

            val balanceCalculator = entryPoint.balanceCalculator()
            val transactionRepository = entryPoint.transactionRepository()
            val now = Date()

            val allTimeBalances = balanceCalculator.getAllTimeBalances()
            val liquidCash = allTimeBalances.total

            val startOfToday = DateUtils.getStartOfDay(now)
            val endOfToday = DateUtils.getEndOfDay(now)
            val todayTxns = transactionRepository.getTransactionsBetweenDatesSnapshot(startOfToday, endOfToday)
            val todaySpend = todayTxns.filter {
                it.type == TransactionType.EXPENSE &&
                it.accountType != AccountType.WALLET &&
                it.accountType != AccountType.DEPOSIT &&
                !it.excludeFromMonthExpenses
            }.sumOf { it.amount }

            QuickAddWidgetData(
                todaySpend = todaySpend,
                availableLiquid = liquidCash
            )
        } catch (_: Exception) {
            QuickAddWidgetData(
                todaySpend = 0.0,
                availableLiquid = 0.0
            )
        }
    }
}

