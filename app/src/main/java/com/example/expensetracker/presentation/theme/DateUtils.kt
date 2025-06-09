package com.example.expensetracker.presentation.theme

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
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
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
    private val numberFormat = NumberFormat.getCurrencyInstance()

    fun formatCurrency(amount: Double): String {
        return numberFormat.format(amount)
    }
}