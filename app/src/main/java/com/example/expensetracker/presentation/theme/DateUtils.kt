package com.example.expensetracker.presentation.theme

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    const val PATTERN_DEFAULT = "MMM dd, yyyy"
    const val PATTERN_FULL_DATE = "dd MMMM yyyy"
    const val PATTERN_SHORT_DATE = "dd MMM yyyy"
    const val PATTERN_MONTH_DAY = "MMM dd"
    const val PATTERN_MONTH_YEAR = "MMMM yyyy"
    const val PATTERN_TIME_24H = "HH:mm"
    const val PATTERN_TIME_12H = "hh:mm a"
    const val PATTERN_DATE_TIME = "MMM dd, yyyy HH:mm"
    const val PATTERN_ISO = "yyyy-MM-dd HH:mm:ss"

    private val dateFormat = SimpleDateFormat(PATTERN_DEFAULT, Locale.getDefault())
    private val timeFormat = SimpleDateFormat(PATTERN_TIME_24H, Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat(PATTERN_DATE_TIME, Locale.getDefault())

    fun formatDate(date: Date): String = dateFormat.format(date)
    fun formatTime(date: Date): String = timeFormat.format(date)
    fun formatDateTime(date: Date): String = dateTimeFormat.format(date)

    /**
     * Formats a [Date] with any custom [pattern] and optional [locale].
     */
    fun format(date: Date, pattern: String, locale: Locale = Locale.getDefault()): String {
        return try {
            SimpleDateFormat(pattern, locale).format(date)
        } catch (e: Exception) {
            formatDate(date)
        }
    }

    /**
     * Parses a date string using the specified [pattern].
     */
    fun parse(dateString: String, pattern: String, locale: Locale = Locale.getDefault()): Date? {
        return try {
            SimpleDateFormat(pattern, locale).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatWeekRange(weekStart: Date): String {
        val weekEnd = getEndOfWeek(weekStart)
        val shortFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        return "${shortFormat.format(weekStart)} – ${shortFormat.format(weekEnd)}"
    }

    fun formatMonthYear(date: Date): String {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return monthFormat.format(date)
    }

    fun getDayKey(date: Date): Long = getStartOfDay(date).time

    fun getWeekKey(date: Date): Long = getStartOfWeek(date).time

    fun getMonthKey(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
    }

    fun dateFromMonthKey(key: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(key / 100, key % 100, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    fun getStartOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.SATURDAY
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        if (calendar.time.after(date)) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }
        return getStartOfDay(calendar.time)
    }

    fun getEndOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = getStartOfWeek(date)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        return getEndOfDay(calendar.time)
    }

    fun getStartOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(calendar.time)
    }

    fun getEndOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return getEndOfDay(calendar.time)
    }

    fun getStartOfYear(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        return getStartOfDay(calendar.time)
    }

    fun getEndOfYear(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
        return getEndOfDay(calendar.time)
    }
}

object CurrencyUtils {
    private val decimalFormat = DecimalFormat("#,##0.000", DecimalFormatSymbols(Locale.US))

    fun formatAmountOnly(amount: Double): String {
        return decimalFormat.format(amount)
    }

    fun formatCurrency(amount: Double): String {
        return "${decimalFormat.format(amount)} JOD"
    }

    fun formatAmount(amount: Double, includeCurrency: Boolean = true): String {
        return if (includeCurrency) formatCurrency(amount) else formatAmountOnly(amount)
    }

    /**
     * Sanitizes amount input by keeping only digits and at most one decimal point.
     */
    fun cleanDecimalInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val firstDotIndex = filtered.indexOf('.')
        return if (firstDotIndex == -1) {
            filtered
        } else {
            filtered.substring(0, firstDotIndex + 1) +
                filtered.substring(firstDotIndex + 1).filter { it.isDigit() }
        }
    }
}

/**
 * Extension functions for Double to format amounts with 3 decimal places and thousands grouping.
 */
fun Double.formatCurrency(): String = CurrencyUtils.formatCurrency(this)
fun Double.formatAmount(includeCurrency: Boolean = true): String = CurrencyUtils.formatAmount(this, includeCurrency)

/**
 * Extension functions for Date to format easily in any format needed.
 */
fun Date.format(pattern: String = DateUtils.PATTERN_DEFAULT, locale: Locale = Locale.getDefault()): String =
    DateUtils.format(this, pattern, locale)

fun Date.formatDate(): String = DateUtils.formatDate(this)
fun Date.formatTime(): String = DateUtils.formatTime(this)
fun Date.formatDateTime(): String = DateUtils.formatDateTime(this)
fun Date.formatMonthYear(): String = DateUtils.formatMonthYear(this)
