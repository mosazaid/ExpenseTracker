package com.example.expensetracker.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Date/time utility object.
 *
 * All formatters use [DateTimeFormatter], which is **immutable and thread-safe** —
 * safe to share across coroutines without any synchronisation overhead.
 * The old `SimpleDateFormat` shared instances were NOT thread-safe.
 */
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

    // ── Shared, immutable, thread-safe formatters ─────────────────────────────
    private val defaultFormatter    = DateTimeFormatter.ofPattern(PATTERN_DEFAULT,   Locale.getDefault())
    private val timeFormatter       = DateTimeFormatter.ofPattern(PATTERN_TIME_24H,  Locale.getDefault())
    private val dateTimeFormatter   = DateTimeFormatter.ofPattern(PATTERN_DATE_TIME, Locale.getDefault())
    private val weekRangeFormatter  = DateTimeFormatter.ofPattern(PATTERN_MONTH_DAY, Locale.getDefault())
    private val monthYearFormatter  = DateTimeFormatter.ofPattern(PATTERN_MONTH_YEAR,Locale.getDefault())

    // ── Helper to convert java.util.Date → LocalDateTime via system zone ──────
    private fun Date.toLocalDateTime(): LocalDateTime =
        toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun Date.toLocalDate(): LocalDate =
        toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    // ── Core formatters ───────────────────────────────────────────────────────

    fun formatDate(date: Date): String = date.toLocalDate().format(defaultFormatter)

    fun formatTime(date: Date): String = date.toLocalDateTime().format(timeFormatter)

    fun formatDateTime(date: Date): String = date.toLocalDateTime().format(dateTimeFormatter)

    /**
     * Formats a [Date] with any custom [pattern] and optional [locale].
     * The formatter is constructed per-call for custom patterns; use the
     * named helpers (formatDate / formatTime / etc.) for hot paths.
     */
    fun format(date: Date, pattern: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            date.toLocalDateTime().format(formatter)
        } catch (e: Exception) {
            formatDate(date)
        }
    }

    /**
     * Parses a date string using the specified [pattern].
     */
    fun parse(dateString: String, pattern: String, locale: Locale = Locale.getDefault()): Date? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            try {
                val localDateTime = LocalDateTime.parse(dateString, formatter)
                Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
            } catch (e: Exception) {
                val localDate = LocalDate.parse(dateString, formatter)
                Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Returns "MMM dd – MMM dd" formatted week range for the week containing [weekStart]. */
    fun formatWeekRange(weekStart: Date): String {
        val weekEnd = getEndOfWeek(weekStart)
        return "${weekStart.toLocalDate().format(weekRangeFormatter)} – ${weekEnd.toLocalDate().format(weekRangeFormatter)}"
    }

    /** Returns "MMMM yyyy" formatted month+year string. */
    fun formatMonthYear(date: Date): String = date.toLocalDate().format(monthYearFormatter)

    fun getDayKey(date: Date): Long = getStartOfDay(date).time

    fun getWeekKey(date: Date): Long = getStartOfWeek(date).time

    fun getMonthKey(date: Date): Int {
        val ldt = date.toLocalDate()
        return ldt.year * 100 + (ldt.monthValue - 1) // keep 0-based month to match Calendar.MONTH
    }

    fun dateFromMonthKey(key: Int): Date {
        val year = key / 100
        val month = key % 100 + 1 // convert back to 1-based
        val localDate = LocalDate.of(year, month, 1)
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    fun getStartOfDay(date: Date): Date {
        val localDate = date.toLocalDate()
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    fun getEndOfDay(date: Date): Date {
        val localDate = date.toLocalDate()
        return Date.from(
            localDate.atTime(23, 59, 59, 999_000_000)
                .atZone(ZoneId.systemDefault()).toInstant()
        )
    }

    fun getStartOfWeek(date: Date): Date {
        val localDate = date.toLocalDate()
        // Week starts on Saturday (DayOfWeek: Mon=1..Sat=6, Sun=7)
        val daysSinceSaturday = (localDate.dayOfWeek.value - java.time.DayOfWeek.SATURDAY.value).let { if (it < 0) it + 7 else it }
        val startLocalDate = localDate.minusDays(daysSinceSaturday.toLong())
        return getStartOfDay(Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }

    fun getEndOfWeek(date: Date): Date {
        val weekStart = getStartOfWeek(date)
        val localDate = weekStart.toLocalDate().plusDays(6)
        return getEndOfDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }

    fun getStartOfMonth(date: Date): Date {
        val localDate = date.toLocalDate().withDayOfMonth(1)
        return getStartOfDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }

    fun getEndOfMonth(date: Date): Date {
        val localDate = date.toLocalDate().let { it.withDayOfMonth(it.lengthOfMonth()) }
        return getEndOfDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }

    fun getStartOfYear(date: Date): Date {
        val localDate = date.toLocalDate().withDayOfYear(1)
        return getStartOfDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }

    fun getEndOfYear(date: Date): Date {
        val localDate = date.toLocalDate().let { it.withDayOfYear(it.lengthOfYear()) }
        return getEndOfDay(Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }
}

// ── Extension functions ───────────────────────────────────────────────────────

/**
 * Formats a [Date] using any [pattern] string. Uses [DateTimeFormatter] internally.
 */
fun Date.format(pattern: String = DateUtils.PATTERN_DEFAULT, locale: Locale = Locale.getDefault()): String =
    DateUtils.format(this, pattern, locale)

fun Date.formatDate(): String = DateUtils.formatDate(this)
fun Date.formatMonthYear(): String = DateUtils.formatMonthYear(this)
fun Date.formatTime(): String = DateUtils.formatTime(this)
fun Date.formatDateTime(): String = DateUtils.formatDateTime(this)
