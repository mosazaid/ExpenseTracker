package com.example.expensetracker.domain

import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.preferences.MonthMode
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

    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var periodCalculator: PeriodCalculator

    @Before
    fun setup() {
        transactionDao = Mockito.mock(TransactionDao::class.java)
        categoryDao = Mockito.mock(CategoryDao::class.java)
        periodCalculator = PeriodCalculator(transactionDao, categoryDao)
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
        Mockito.`when`(categoryDao.getCategoryByName(CategorySystemKey.SALARY.displayName, CategorySystemKey.SALARY.type)).thenReturn(null)

        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, refDate, MonthMode.SALARY)

        assertTrue(bounds.isSalaryFallback)
        assertEquals(DateUtils.getStartOfMonth(refDate), bounds.start)
        assertEquals(DateUtils.getEndOfMonth(refDate), bounds.end)
    }
}
