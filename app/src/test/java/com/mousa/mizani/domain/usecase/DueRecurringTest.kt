package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.DepositCalculator
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.util.NotificationHelper
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class DueRecurringTest {

    private val balanceCalculator = mock(BalanceCalculator::class.java)
    private val accountBalance = RecurringAccountBalance(
        balanceCalculator, mock(WalletCalculator::class.java), mock(DepositCalculator::class.java)
    )
    private val now = dateOf(2026, 3, 20)

    private suspend fun bankHolds(amount: Double) {
        doReturn(amount).`when`(balanceCalculator).getAvailableBalance(AccountType.BANK)
    }

    private suspend fun attention(repository: FakeRecurringRepository) =
        DueRecurringAttentionUseCase(repository, accountBalance)(now)

    private fun skipUseCase(repository: FakeRecurringRepository) =
        SkipRecurringOccurrenceUseCase(repository, RecurringScheduleLock())

    @Test
    fun `reminder-only items are listed`() = runTest {
        bankHolds(1_000.0)
        val repository = FakeRecurringRepository(
            recurringItem(1, nextDueDate = dateOf(2026, 3, 15)),
            recurringItem(2, type = TransactionType.INCOME, nextDueDate = dateOf(2026, 3, 1)),
            recurringItem(3, nextDueDate = dateOf(2026, 4, 1))
        )

        val listed = attention(repository)

        assertEquals("oldest due first, future items left out", listOf(2L, 1L), listed.map { it.recurring.id })
        assertTrue(listed.none { it.isBlocked })
    }

    @Test
    fun `affordable auto item is not listed`() = runTest {
        bankHolds(10.0)

        assertEquals(emptyList<DueRecurringItem>(), attention(FakeRecurringRepository(recurringItem(1, autoAdd = true))))
    }

    @Test
    fun `unaffordable auto expense is listed as blocked`() = runTest {
        bankHolds(9.0)
        val item = recurringItem(1, autoAdd = true)

        assertEquals(listOf(DueRecurringItem(item, isBlocked = true)), attention(FakeRecurringRepository(item)))
    }

    @Test
    fun `auto item that can record an older period is not listed`() = runTest {
        bankHolds(15.0)
        val twoPeriodsDue = recurringItem(1, autoAdd = true, nextDueDate = dateOf(2026, 2, 10))

        assertEquals(emptyList<DueRecurringItem>(), attention(FakeRecurringRepository(twoPeriodsDue)))
    }

    @Test
    fun `auto income is not listed`() = runTest {
        bankHolds(0.0)
        val income = recurringItem(1, autoAdd = true, type = TransactionType.INCOME)

        assertEquals(emptyList<DueRecurringItem>(), attention(FakeRecurringRepository(income)))
    }

    @Test
    fun `recurring notification switch off does not change the list`() = runTest {
        val userPreferences = mock(UserPreferences::class.java)
        `when`(userPreferences.salaryAlertsEnabled).thenReturn(flowOf(false))
        val notificationHelper = mock(NotificationHelper::class.java)
        val repository = FakeRecurringRepository(recurringItem(1))

        RecurringReminderUseCase(repository, userPreferences, notificationHelper)(now)

        verifyNoInteractions(notificationHelper)
        assertEquals(listOf(1L), attention(repository).map { it.recurring.id })
    }

    @Test
    fun `skip advances one period and inserts no transaction`() = runTest {
        // Two periods are due; one skip moves past only the older one. The use case has no
        // access to the transaction table at all.
        val item = recurringItem(1, nextDueDate = dateOf(2026, 2, 10))
        val repository = FakeRecurringRepository(item)

        val skipped = skipUseCase(repository)(1, item.nextDueDate.time)

        assertTrue(skipped)
        assertEquals(dateOf(2026, 3, 10), repository.items.getValue(1).nextDueDate)
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `second call with the same expected date returns false`() = runTest {
        val item = recurringItem(1, nextDueDate = dateOf(2026, 2, 10))
        val repository = FakeRecurringRepository(item)
        val skip = skipUseCase(repository)

        skip(1, item.nextDueDate.time)

        assertFalse(skip(1, item.nextDueDate.time))
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `missing or inactive item writes nothing`() = runTest {
        val paused = recurringItem(2, isActive = false)
        val repository = FakeRecurringRepository(paused)
        val skip = skipUseCase(repository)

        assertFalse(skip(1, paused.nextDueDate.time))
        assertFalse(skip(2, paused.nextDueDate.time))
        assertEquals(emptyList<Any>(), repository.writes)
    }
}
