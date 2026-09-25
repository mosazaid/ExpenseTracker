package com.example.expensetracker.core

import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.format.formatAmount
import com.example.expensetracker.core.format.formatCurrency
import com.example.expensetracker.core.time.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

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
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 19)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 45)
            set(Calendar.MILLISECOND, 500)
        }
        val date = cal.time

        val start = DateUtils.getStartOfDay(date)
        val startCal = Calendar.getInstance().apply { time = start }
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))

        val end = DateUtils.getEndOfDay(date)
        val endCal = Calendar.getInstance().apply { time = end }
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testDateUtils_startAndEndOfWeek() {
        // Sep 19, 2026 is a Saturday (in our app Saturday is start of week)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 21) // Monday
        }
        val date = cal.time

        val startOfWeek = DateUtils.getStartOfWeek(date)
        val startCal = Calendar.getInstance().apply { time = startOfWeek }
        assertEquals(Calendar.SATURDAY, startCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(19, startCal.get(Calendar.DAY_OF_MONTH))

        val endOfWeek = DateUtils.getEndOfWeek(date)
        val endCal = Calendar.getInstance().apply { time = endOfWeek }
        assertEquals(Calendar.FRIDAY, endCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(25, endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testDateUtils_formattingAndParsing() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 19)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val date = cal.time

        // Test custom format pattern with US locale for deterministic assertions
        val formattedFull = DateUtils.format(
            date,
            DateUtils.PATTERN_FULL_DATE,
            Locale.US
        )
        assertEquals("19 September 2026", formattedFull)

        val formattedIso = DateUtils.format(
            date,
            DateUtils.PATTERN_ISO,
            Locale.US
        )
        assertEquals("2026-09-19 14:30:00", formattedIso)

        // Test parse
        val parsed = DateUtils.parse(
            "2026-09-19 14:30:00",
            DateUtils.PATTERN_ISO,
            Locale.US
        )
        assertEquals(date.time, parsed?.time)

        // Test invalid parse returns null safely
        val invalidParsed = DateUtils.parse(
            "invalid-date",
            DateUtils.PATTERN_ISO,
            Locale.US
        )
        org.junit.Assert.assertNull(invalidParsed)
    }

    @Test
    fun testDateUtils_monthAndYearBoundaries() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.FEBRUARY)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val date = cal.time

        val startOfMonth = DateUtils.getStartOfMonth(date)
        val startMonthCal = Calendar.getInstance().apply { time = startOfMonth }
        assertEquals(1, startMonthCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.FEBRUARY, startMonthCal.get(Calendar.MONTH))

        val endOfMonth = DateUtils.getEndOfMonth(date)
        val endMonthCal = Calendar.getInstance().apply { time = endOfMonth }
        assertEquals(28, endMonthCal.get(Calendar.DAY_OF_MONTH)) // 2026 is non-leap year

        val startOfYear = DateUtils.getStartOfYear(date)
        val startYearCal = Calendar.getInstance().apply { time = startOfYear }
        assertEquals(1, startYearCal.get(Calendar.DAY_OF_YEAR))

        val endOfYear = DateUtils.getEndOfYear(date)
        val endYearCal = Calendar.getInstance().apply { time = endOfYear }
        assertEquals(365, endYearCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testDateUtils_monthKeyConversions() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 25)
        }
        val date = cal.time

        val monthKey = DateUtils.getMonthKey(date)
        assertEquals(202608, monthKey) // 0-based month September = 8

        val reconstructedDate = DateUtils.dateFromMonthKey(monthKey)
        val reconCal = Calendar.getInstance().apply { time = reconstructedDate }
        assertEquals(2026, reconCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, reconCal.get(Calendar.MONTH))
        assertEquals(1, reconCal.get(Calendar.DAY_OF_MONTH))
    }
}
