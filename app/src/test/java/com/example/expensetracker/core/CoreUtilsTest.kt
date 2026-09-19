package com.example.expensetracker.core

import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.theme.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CoreUtilsTest {

    @Test
    fun testCurrencyUtils_formatting() {
        assertEquals("0.00 JOD", CurrencyUtils.formatCurrency(0.0))
        assertEquals("50.80 JOD", CurrencyUtils.formatCurrency(50.8))
        assertEquals("1,250.50 JOD", CurrencyUtils.formatCurrency(1250.5))
        assertEquals("-25.00 JOD", CurrencyUtils.formatCurrency(-25.0))
    }

    @Test
    fun testDateUtils_startAndEndOfDay() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 19, 15, 45, 30)
            set(Calendar.MILLISECOND, 500)
        }
        val date = cal.time

        val startOfDay = DateUtils.getStartOfDay(date)
        val endOfDay = DateUtils.getEndOfDay(date)

        val calStart = Calendar.getInstance().apply { time = startOfDay }
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(0, calStart.get(Calendar.MILLISECOND))

        val calEnd = Calendar.getInstance().apply { time = endOfDay }
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(999, calEnd.get(Calendar.MILLISECOND))

        assertTrue(endOfDay.after(startOfDay))
    }

    @Test
    fun testDateUtils_startAndEndOfMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 15)
        }
        val date = cal.time

        val startOfMonth = DateUtils.getStartOfMonth(date)
        val endOfMonth = DateUtils.getEndOfMonth(date)

        val calStart = Calendar.getInstance().apply { time = startOfMonth }
        assertEquals(1, calStart.get(Calendar.DAY_OF_MONTH))

        val calEnd = Calendar.getInstance().apply { time = endOfMonth }
        assertEquals(28, calEnd.get(Calendar.DAY_OF_MONTH)) // 2026 is not a leap year
    }
}
