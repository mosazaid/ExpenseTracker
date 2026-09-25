package com.example.expensetracker.domain

import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class SalaryWalletPeriodSummary(
    val label: String,
    val start: Date,
    val end: Date,
    val walletRemaining: Double,
    val isClosed: Boolean,
    val isCurrent: Boolean
)

@Singleton
class WalletCalculator @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val periodCalculator: PeriodCalculator
) {

    suspend fun getWalletBalance(startDate: Date, endDate: Date, excludeTransactionId: Long? = null): Double {
        val transactions = transactionRepository.getTransactionsBetweenDatesSnapshot(startDate, endDate)
        return computeWalletBalanceFromMoves(
            transactions.filter { it.type == TransactionType.WALLET_MOVE },
            excludeTransactionId
        )
    }

    private fun computeWalletBalanceFromMoves(
        moves: List<Transaction>,
        excludeTransactionId: Long? = null
    ): Double {
        var walletIn = 0.0
        var walletOut = 0.0
        for (move in moves) {
            if (move.id == excludeTransactionId) continue
            if (move.toAccountType == AccountType.WALLET) walletIn += move.amount
            if (move.accountType == AccountType.WALLET) walletOut += move.amount
        }
        return walletIn - walletOut
    }

    suspend fun getOpenSalaryPeriodBounds(now: Date = Date()): PeriodBounds? {
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, now, MonthMode.SALARY)
        return bounds.takeUnless { it.isSalaryFallback }
    }

    suspend fun isDateInOpenSalaryPeriod(date: Date, now: Date = Date()): Boolean {
        val openBounds = getOpenSalaryPeriodBounds(now) ?: return false
        val dayStart = DateUtils.getStartOfDay(date)
        return !dayStart.before(openBounds.start) && !dayStart.after(openBounds.end)
    }

    suspend fun isPeriodClosedForDate(date: Date, now: Date = Date()): Boolean {
        return !isDateInOpenSalaryPeriod(date, now)
    }

    suspend fun getSalaryPeriodSummariesForYear(year: Int): List<SalaryWalletPeriodSummary> {
        val salaryCategory = categoryRepository.getCategoryByName(
            CategorySystemKey.SALARY.displayName,
            CategorySystemKey.SALARY.type
        ) ?: return emptyList()

        val anchors = transactionRepository.getSalaryPeriodAnchors(salaryCategory.id)
        if (anchors.isEmpty()) return emptyList()

        val yearStart = Calendar.getInstance().apply {
            set(year, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val yearEnd = DateUtils.getEndOfYear(yearStart)
        val now = Date()
        val openAnchor = periodCalculator.getCurrentPeriodAnchor(now)

        return anchors.mapIndexed { index, anchor ->
            val periodStart = DateUtils.getStartOfDay(anchor.date)
            val nextAnchor = anchors.getOrNull(index + 1)
            val naturalEnd = if (nextAnchor != null) {
                dayBefore(nextAnchor.date)
            } else {
                DateUtils.getEndOfDay(now)
            }
            val isCurrent = openAnchor?.id == anchor.id
            val openBounds = if (isCurrent) getOpenSalaryPeriodBounds(now) else null
            val periodEnd = if (isCurrent) {
                openBounds?.end ?: naturalEnd
            } else {
                naturalEnd
            }
            val isClosed = nextAnchor != null
            val walletRemaining = getWalletBalance(periodStart, periodEnd)
            SalaryWalletPeriodSummary(
                label = "${DateUtils.formatDate(periodStart)} – ${DateUtils.formatDate(periodEnd)}",
                start = periodStart,
                end = periodEnd,
                walletRemaining = walletRemaining,
                isClosed = isClosed,
                isCurrent = isCurrent
            )
        }.filter { summary ->
            summary.start <= yearEnd && summary.end >= yearStart
        }
    }

    /** Validates WALLET_MOVE: exactly one side is WALLET, the other is CASH or BANK. */
    fun validateWalletMove(from: AccountType, to: AccountType?): Boolean {
        if (to == null) return false
        val involvesWallet = from == AccountType.WALLET || to == AccountType.WALLET
        if (!involvesWallet) return false
        if (from == AccountType.WALLET && to == AccountType.WALLET) return false
        val other = if (from == AccountType.WALLET) to else from
        return other == AccountType.CASH || other == AccountType.BANK
    }

    private fun dayBefore(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = DateUtils.getStartOfDay(date)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return DateUtils.getEndOfDay(calendar.time)
    }
}
