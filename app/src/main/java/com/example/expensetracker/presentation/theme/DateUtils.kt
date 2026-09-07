package com.example.expensetracker.presentation.theme

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun formatDate(date: Date): String = dateFormat.format(date)
    fun formatTime(date: Date): String = timeFormat.format(date)
    fun formatDateTime(date: Date): String = dateTimeFormat.format(date)

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
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun formatCurrency(amount: Double): String {
        return "${decimalFormat.format(amount)} JOD"
    }
}