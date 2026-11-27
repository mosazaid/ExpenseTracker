package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.domain.calculator.advanced
import com.mousa.mizani.domain.repository.IRecurringRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar
import java.util.Date

fun dateOf(year: Int, month: Int, day: Int): Date = Calendar.getInstance().apply {
    clear()
    set(year, month - 1, day, 9, 0)
}.time

fun recurringItem(
    id: Long,
    autoAdd: Boolean = false,
    type: TransactionType = TransactionType.EXPENSE,
    amount: Double = 10.0,
    nextDueDate: Date = dateOf(2026, 3, 10),
    isActive: Boolean = true
) = RecurringTransaction(
    id = id, amount = amount, description = "item$id", type = type, categoryId = null,
    accountType = AccountType.BANK, frequency = RecurrenceFrequency.MONTHLY,
    nextDueDate = nextDueDate, isActive = isActive, autoAdd = autoAdd
)

/** In-memory recurring table. */
class FakeRecurringRepository(vararg initial: RecurringTransaction) : IRecurringRepository {
    val items = initial.associateBy { it.id }.toMutableMap()
    val writes = mutableListOf<RecurringTransaction>()

    /** When set, reads by id wait for it, so a test can overlap two callers. */
    var readGate: CompletableDeferred<Unit>? = null

    /** When set, reading throws it. */
    var readFailure: Throwable? = null

    override suspend fun getDueReminders(date: Date): List<RecurringTransaction> {
        readFailure?.let { throw it }
        return items.values.filter { it.isActive && !it.nextDueDate.after(date) }
    }

    override suspend fun getRecurringById(id: Long): RecurringTransaction? {
        readGate?.await()
        readFailure?.let { throw it }
        return items[id]
    }

    /** When set, advancing throws it instead of writing. */
    var advanceFailure: Throwable? = null

    /** Runs just before an advance is written. */
    var onAdvance: () -> Unit = {}

    override suspend fun advanceRecurringDueDate(recurring: RecurringTransaction) {
        onAdvance()
        advanceFailure?.let { throw it }
        updateRecurring(recurring.advanced())
    }

    override suspend fun updateRecurring(recurring: RecurringTransaction) {
        items[recurring.id] = recurring
        writes += recurring
    }

    override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> =
        flowOf(items.values.filter { it.isActive })

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(items.values.toList())

    override suspend fun createSalaryReminder(
        amount: Double,
        accountType: AccountType,
        salaryCategoryId: Long,
        dayOfMonth: Int,
        description: String
    ): Long = error("not used")

    override suspend fun deactivateRecurring(id: Long) = error("not used")
    override suspend fun setRecurringActive(id: Long, isActive: Boolean) = error("not used")
    override suspend fun deleteRecurringById(id: Long) = error("not used")
    override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long = error("not used")
}
