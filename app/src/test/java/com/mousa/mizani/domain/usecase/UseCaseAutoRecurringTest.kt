package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class UseCaseAutoRecurringTest {

    private fun item(id: Long, autoAdd: Boolean, type: TransactionType = TransactionType.EXPENSE) =
        RecurringTransaction(
            id = id, amount = 10.0, description = "item$id", type = type, categoryId = null,
            accountType = AccountType.BANK, frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = Date(0), autoAdd = autoAdd
        )

    @Test
    fun `only items set to auto add are selected for recording`() {
        val due = listOf(item(1, autoAdd = false), item(2, autoAdd = true), item(3, false, TransactionType.INCOME))

        assertEquals(listOf(2L), due.itemsToAutoAdd().map { it.id })
        assertEquals(listOf(1L, 3L), due.itemsToRemind().map { it.id })
    }

    @Test
    fun `new recurring items default to alert only`() {
        val created = RecurringTransaction(
            amount = 1.0, description = "x", type = TransactionType.EXPENSE, categoryId = null,
            accountType = AccountType.CASH, frequency = RecurrenceFrequency.MONTHLY, nextDueDate = Date(0)
        )

        assertEquals(false, created.autoAdd)
    }
}
