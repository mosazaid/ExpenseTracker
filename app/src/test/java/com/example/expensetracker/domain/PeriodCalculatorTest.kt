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
}
