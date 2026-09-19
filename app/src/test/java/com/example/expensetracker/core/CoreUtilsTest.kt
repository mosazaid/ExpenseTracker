package com.example.expensetracker.core

import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.theme.formatCurrency
import com.example.expensetracker.presentation.theme.formatAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CoreUtilsTest {

    @Test
    fun testCurrencyUtils_formatting() {
        assertEquals("0.000 JOD", CurrencyUtils.formatCurrency(0.0))
        assertEquals("50.800 JOD", CurrencyUtils.formatCurrency(50.8))
        assertEquals("1,250.500 JOD", CurrencyUtils.formatCurrency(1250.5))
        assertEquals("-25.000 JOD", CurrencyUtils.formatCurrency(-25.0))
        assertEquals("1,234,567.890 JOD", CurrencyUtils.formatCurrency(1234567.89))

        // Test Double extension functions
        assertEquals("1,250.500 JOD", 1250.5.formatCurrency())
        assertEquals("1,250.500", 1250.5.formatAmount(includeCurrency = false))
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

    @Test
    fun testDateUtils_formatAndParse() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 19, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val date = cal.time

        // Test custom format pattern with US locale for deterministic assertions
        val formattedFull = com.example.expensetracker.presentation.theme.DateUtils.format(
            date,
            com.example.expensetracker.presentation.theme.DateUtils.PATTERN_FULL_DATE,
            java.util.Locale.US
        )
        assertEquals("19 September 2026", formattedFull)

        val formattedIso = com.example.expensetracker.presentation.theme.DateUtils.format(
            date,
            com.example.expensetracker.presentation.theme.DateUtils.PATTERN_ISO,
            java.util.Locale.US
        )
        assertEquals("2026-09-19 14:30:00", formattedIso)

        // Test parse
        val parsed = com.example.expensetracker.presentation.theme.DateUtils.parse(
            "2026-09-19 14:30:00",
            com.example.expensetracker.presentation.theme.DateUtils.PATTERN_ISO,
            java.util.Locale.US
        )
        assertEquals(date.time, parsed?.time)

        // Test invalid parse returns null safely
        val invalidParsed = com.example.expensetracker.presentation.theme.DateUtils.parse(
            "invalid-date",
            com.example.expensetracker.presentation.theme.DateUtils.PATTERN_ISO,
            java.util.Locale.US
        )
        org.junit.Assert.assertNull(invalidParsed)
    }
}

