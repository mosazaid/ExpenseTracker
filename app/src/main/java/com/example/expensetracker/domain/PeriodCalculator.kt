package com.example.expensetracker.domain

import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.core.time.DateUtils
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class PeriodBounds(
    val start: Date,
    val end: Date,
    val label: String,
    val isSalaryFallback: Boolean = false,
    val hint: String? = null
)

@Singleton
class PeriodCalculator @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    suspend fun getBounds(
        period: HistoryPeriod,
        referenceDate: Date = Date(),
        monthMode: MonthMode
    ): PeriodBounds {
        return when (period) {
            HistoryPeriod.DAY -> {
                val start = DateUtils.getStartOfDay(referenceDate)
                val end = DateUtils.getEndOfDay(referenceDate)
                PeriodBounds(
                    start = start,
                    end = end,
                    label = DateUtils.formatDate(start)
                )
            }

            HistoryPeriod.WEEK -> {
                val start = DateUtils.getStartOfWeek(referenceDate)
                val end = DateUtils.getEndOfWeek(referenceDate)
                PeriodBounds(
                    start = start,
                    end = end,
                    label = DateUtils.formatWeekRange(start)
                )
            }

            HistoryPeriod.YEAR -> {
                val start = DateUtils.getStartOfYear(referenceDate)
                val end = DateUtils.getEndOfYear(referenceDate)
                val year = Calendar.getInstance().apply { time = start }.get(Calendar.YEAR)
                PeriodBounds(
                    start = start,
                    end = end,
                    label = year.toString()
                )
            }

            HistoryPeriod.MONTH -> {
                if (monthMode == MonthMode.CALENDAR) {
                    val start = DateUtils.getStartOfMonth(referenceDate)
                    val end = DateUtils.getEndOfMonth(referenceDate)
                    PeriodBounds(
                        start = start,
                        end = end,
                        label = DateUtils.formatMonthYear(start)
                    )
                } else {
                    getSalaryMonthBounds(referenceDate)
                }
            }
        }
    }

    /**
     * Returns the salary transaction that anchors the current salary month for [referenceDate].
     */
    suspend fun getCurrentPeriodAnchor(referenceDate: Date): Transaction? {
        val salaryCategory = categoryDao.getCategoryByName(
            CategorySystemKey.SALARY.displayName,
            CategorySystemKey.SALARY.type
        ) ?: return null

        val referenceEnd = DateUtils.getEndOfDay(referenceDate)
        val anchors = transactionDao.getSalaryPeriodAnchors(salaryCategory.id)
        if (anchors.isEmpty()) return null

        val anchorIndex = anchors.indexOfLast { it.date <= referenceEnd }
        return anchors.getOrNull(anchorIndex)
    }

    /**
     * Bounds for the salary month that ends the day before [newSalaryDate].
     * Used to calculate how much was saved in the previous month for carry-forward.
     */
    suspend fun getPreviousSalaryPeriodBounds(newSalaryDate: Date): PeriodBounds? {
        val calendar = Calendar.getInstance()
        calendar.time = DateUtils.getStartOfDay(newSalaryDate)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val bounds = getBounds(HistoryPeriod.MONTH, calendar.time, MonthMode.SALARY)
        return bounds.takeUnless { it.isSalaryFallback }
    }

    private suspend fun getSalaryMonthBounds(referenceDate: Date): PeriodBounds {
        val salaryCategory = categoryDao.getCategoryByName(
            CategorySystemKey.SALARY.displayName,
            CategorySystemKey.SALARY.type
        )
        if (salaryCategory == null) {
            return calendarMonthFallback(referenceDate, "Salary category not found")
        }

        val markedAnchors = transactionDao.getSalaryPeriodAnchors(salaryCategory.id)
        val referenceEnd = DateUtils.getEndOfDay(referenceDate)
        val anchors = if (markedAnchors.isNotEmpty()) {
            markedAnchors
        } else {
            transactionDao.getLatestSalaryIncomeBefore(referenceEnd, salaryCategory.id)
                ?.let { listOf(it) }
                .orEmpty()
        }

        if (anchors.isEmpty()) {
            return calendarMonthFallback(
                referenceDate,
                "Add a Salary income and mark Start new month"
            )
        }

        val anchorIndex = anchors.indexOfLast { it.date <= referenceEnd }

        if (anchorIndex < 0) {
            return calendarMonthFallback(
                referenceDate,
                "Add a Salary income and mark Start new month"
            )
        }

        val anchor = anchors[anchorIndex]
        val periodStart = DateUtils.getStartOfDay(anchor.date)
        val nextAnchor = anchors.getOrNull(anchorIndex + 1)

        val naturalEnd = if (nextAnchor != null) {
            dayBefore(nextAnchor.date)
        } else {
            referenceEnd
        }

        val periodEnd = if (naturalEnd.after(referenceEnd)) referenceEnd else naturalEnd

        return PeriodBounds(
            start = periodStart,
            end = periodEnd,
            label = "${DateUtils.formatDate(periodStart)} – ${DateUtils.formatDate(periodEnd)}",
            isSalaryFallback = false
        )
    }

    private fun dayBefore(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = DateUtils.getStartOfDay(date)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        return DateUtils.getEndOfDay(calendar.time)
    }

    private fun calendarMonthFallback(referenceDate: Date, hint: String): PeriodBounds {
        val start = DateUtils.getStartOfMonth(referenceDate)
        val end = DateUtils.getEndOfMonth(referenceDate)
        return PeriodBounds(
            start = start,
            end = end,
            label = DateUtils.formatMonthYear(start),
            isSalaryFallback = true,
            hint = hint
        )
    }
}
