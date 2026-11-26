package com.mousa.mizani.domain.calculator

import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import java.util.Calendar
import java.util.Date

/** Most periods one run will catch up; anything left is picked up by the next run. */
const val MAX_CATCH_UP_OCCURRENCES = 400

private const val MONEY_TOLERANCE = 0.000001

fun dayOfMonthOf(date: Date): Int = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_MONTH)

private val RecurrenceFrequency.usesAnchorDay: Boolean
    get() = this == RecurrenceFrequency.MONTHLY || this == RecurrenceFrequency.YEARLY

/**
 * The due date after [from]. Monthly and yearly items aim for [anchorDay] every time, so an item
 * due on the 31st falls on Feb 28 and then returns to Mar 31 instead of staying on the 28th.
 */
fun nextDueDateAfter(from: Date, frequency: RecurrenceFrequency, anchorDay: Int?): Date {
    val calendar = Calendar.getInstance().apply { time = from }
    when (frequency) {
        RecurrenceFrequency.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
        RecurrenceFrequency.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
        RecurrenceFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
        RecurrenceFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
    }
    if (frequency.usesAnchorDay) {
        val wanted = anchorDay ?: dayOfMonthOf(from)
        calendar.set(Calendar.DAY_OF_MONTH, wanted.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }
    return calendar.time
}

/** This item moved on by one period. */
fun RecurringTransaction.advanced(): RecurringTransaction {
    val anchor = if (frequency.usesAnchorDay) anchorDay ?: dayOfMonthOf(nextDueDate) else null
    return copy(nextDueDate = nextDueDateAfter(nextDueDate, frequency, anchor), anchorDay = anchor)
}

/** One entry per period due at or before [now], oldest first; each keeps its own due date. */
fun RecurringTransaction.dueOccurrences(
    now: Date,
    limit: Int = MAX_CATCH_UP_OCCURRENCES
): List<RecurringTransaction> =
    generateSequence(this) { it.advanced() }
        .takeWhile { !it.nextDueDate.after(now) }
        .take(limit)
        .toList()

/** Moves the due date forward past periods before [startOfToday]. A period due today is kept. */
fun RecurringTransaction.skippingMissedBefore(startOfToday: Date): RecurringTransaction =
    generateSequence(this) { it.advanced() }.first { !it.nextDueDate.before(startOfToday) }

/** What an automatic run should record, and the first period it had to hold back. */
data class AutoAddPlan(
    val toRecord: List<RecurringTransaction>,
    val blocked: RecurringTransaction?
)

/**
 * Automatic recording cannot ask "allow a negative balance?", so expenses stop at the first
 * period [availableBalance] cannot cover. That period stays due for the user to record.
 */
fun planAutoAdd(item: RecurringTransaction, now: Date, availableBalance: Double): AutoAddPlan {
    val due = item.dueOccurrences(now)
    if (item.type != TransactionType.EXPENSE) return AutoAddPlan(due, null)
    var remaining = availableBalance
    val affordable = due.takeWhile { occurrence ->
        val fits = occurrence.amount <= remaining + MONEY_TOLERANCE
        if (fits) remaining -= occurrence.amount
        fits
    }
    return AutoAddPlan(affordable, due.getOrNull(affordable.size))
}

/** The anchor to save: kept while date and frequency are unchanged, otherwise taken from the new date. */
fun anchorDayFor(existing: RecurringTransaction?, nextDueDate: Date, frequency: RecurrenceFrequency): Int? {
    if (!frequency.usesAnchorDay) return null
    val unchanged = existing != null && existing.nextDueDate == nextDueDate && existing.frequency == frequency
    return (if (unchanged) existing?.anchorDay else null) ?: dayOfMonthOf(nextDueDate)
}
