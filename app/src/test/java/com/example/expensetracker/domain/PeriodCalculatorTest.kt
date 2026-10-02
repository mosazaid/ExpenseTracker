package com.example.expensetracker.domain

import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Calendar
import java.util.Date

class PeriodCalculatorTest {

    private lateinit var transactionRepository: ITransactionRepository
    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var periodCalculator: PeriodCalculator

    @Before
    fun setup() {
        transactionRepository = Mockito.mock(ITransactionRepository::class.java)
        categoryRepository = Mockito.mock(ICategoryRepository::class.java)
        periodCalculator = PeriodCalculator(transactionRepository, categoryRepository)
    }

    @Test
    fun testDayBounds() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 14, 30, 0)
        }
        val refDate = cal.time

        val bounds = periodCalculator.getBounds(HistoryPeriod.DAY, refDate, MonthMode.CALENDAR)

        assertEquals(DateUtils.getStartOfDay(refDate), bounds.start)
        assertEquals(DateUtils.getEndOfDay(refDate), bounds.end)
        assertEquals(DateUtils.formatDate(bounds.start), bounds.label)
        assertFalse(bounds.isSalaryFallback)
    }

    @Test
    fun testWeekBounds() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 14, 30, 0)
        }
        val refDate = cal.time

        val bounds = periodCalculator.getBounds(HistoryPeriod.WEEK, refDate, MonthMode.CALENDAR)

        assertEquals(DateUtils.getStartOfWeek(refDate), bounds.start)
        assertEquals(DateUtils.getEndOfWeek(refDate), bounds.end)
        assertEquals(DateUtils.formatWeekRange(bounds.start), bounds.label)
        assertFalse(bounds.isSalaryFallback)
    }

    @Test
    fun testCalendarMonthBounds() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 14, 30, 0)
        }
        val refDate = cal.time

        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, refDate, MonthMode.CALENDAR)

        assertEquals(DateUtils.getStartOfMonth(refDate), bounds.start)
        assertEquals(DateUtils.getEndOfMonth(refDate), bounds.end)
        assertEquals(DateUtils.formatMonthYear(bounds.start), bounds.label)
        assertFalse(bounds.isSalaryFallback)
    }

    @Test
    fun testYearBounds() = runTest {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 15, 14, 30, 0)
        }
        val refDate = cal.time

        val bounds = periodCalculator.getBounds(HistoryPeriod.YEAR, refDate, MonthMode.CALENDAR)

        assertEquals(DateUtils.getStartOfYear(refDate), bounds.start)
        assertEquals(DateUtils.getEndOfYear(refDate), bounds.end)
        assertEquals("2026", bounds.label)
        assertFalse(bounds.isSalaryFallback)
    }

    @Test
    fun testSalaryMonthFallbackWhenNoSalaryCategory() = runTest {
        val refDate = Date()
        Mockito.`when`(categoryRepository.getCategoryByName(CategorySystemKey.SALARY.displayName, CategorySystemKey.SALARY.type)).thenReturn(null)

        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, refDate, MonthMode.SALARY)

        assertTrue(bounds.isSalaryFallback)
        assertEquals(DateUtils.getStartOfMonth(refDate), bounds.start)
        assertEquals(DateUtils.getEndOfMonth(refDate), bounds.end)
    }

    @Test
    fun testSalaryMonthBoundsProjectsFullCycleWhenNoNextAnchor() = runTest {
        val salaryCategory = com.example.expensetracker.data.database.entities.Category(
            id = 1L,
            name = CategorySystemKey.SALARY.displayName,
            icon = "💵",
            color = "#4CAF50",
            type = CategorySystemKey.SALARY.type
        )
        Mockito.`when`(categoryRepository.getCategoryByName(CategorySystemKey.SALARY.displayName, CategorySystemKey.SALARY.type))
            .thenReturn(salaryCategory)

        // Salary received on Sep 24
        val anchorCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 24, 9, 0, 0)
        }
        val anchorTxn = com.example.expensetracker.data.database.entities.Transaction(
            id = 10L,
            amount = 1700.0,
            description = "Monthly Salary",
            categoryId = salaryCategory.id,
            type = com.example.expensetracker.data.database.entities.TransactionType.INCOME,
            accountType = com.example.expensetracker.data.database.entities.AccountType.BANK,
            date = anchorCal.time,
            startsNewPeriod = true
        )
        Mockito.`when`(transactionRepository.getSalaryPeriodAnchors(salaryCategory.id))
            .thenReturn(listOf(anchorTxn))

        // Reference date is Oct 2 (8 days later)
        val refCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 2, 14, 0, 0)
        }
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, refCal.time, MonthMode.SALARY)

        assertFalse(bounds.isSalaryFallback)
        assertEquals(DateUtils.getStartOfDay(anchorCal.time), bounds.start)

        // Expected end: 1 month after Sep 24 minus 1 day -> Oct 23 23:59:59
        val expectedEndCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 23, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val actualEndCal = Calendar.getInstance().apply { time = bounds.end }
        assertEquals(Calendar.OCTOBER, actualEndCal.get(Calendar.MONTH))
        assertEquals(23, actualEndCal.get(Calendar.DAY_OF_MONTH))
    }
}
