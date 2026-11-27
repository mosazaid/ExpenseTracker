package com.mousa.mizani.presentation

import android.util.Log
import com.mousa.mizani.core.startup.OpeningPromptSession
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.DepositCalculator
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.domain.calculator.advanced
import com.mousa.mizani.domain.usecase.DueRecurringAttentionUseCase
import com.mousa.mizani.domain.usecase.FakeRecurringRepository
import com.mousa.mizani.domain.usecase.RecurringAccountBalance
import com.mousa.mizani.domain.usecase.RecurringScheduleLock
import com.mousa.mizani.domain.usecase.SkipRecurringOccurrenceUseCase
import com.mousa.mizani.domain.usecase.recurringItem
import com.mousa.mizani.presentation.feature.recurring.due.DueRecurringMessage
import com.mousa.mizani.presentation.feature.recurring.due.DueRecurringViewModel
import com.mousa.mizani.presentation.feature.recurring.due.RecurringFormRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DueRecurringViewModelTest {

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

    private val due = recurringItem(1)
    private val repository = FakeRecurringRepository(due)
    private val session = OpeningPromptSession()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        log = mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        log.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(): DueRecurringViewModel {
        val balance = RecurringAccountBalance(
            mock(BalanceCalculator::class.java),
            mock(WalletCalculator::class.java),
            mock(DepositCalculator::class.java)
        )
        return DueRecurringViewModel(
            DueRecurringAttentionUseCase(repository, balance),
            SkipRecurringOccurrenceUseCase(repository, RecurringScheduleLock()),
            repository,
            session
        )
    }

    private fun loaded() = viewModel().also { it.load() }

    private fun DueRecurringViewModel.listed() = uiState.value.items.map { it.recurring.id }

    @Test
    fun `load lists due items and clear removes them`() {
        val viewModel = loaded()
        assertEquals(listOf(1L), viewModel.listed())

        viewModel.clear()

        assertEquals(emptyList<Long>(), viewModel.listed())
    }

    @Test
    fun `later keeps items due`() {
        val viewModel = loaded()

        viewModel.later()

        assertTrue(session.recurringDismissed.value)
        assertEquals(emptyList<Any>(), repository.writes)
    }

    @Test
    fun `add opens the form without advancing and hides the list`() {
        val viewModel = loaded()

        viewModel.add(viewModel.uiState.value.items.single())

        assertEquals(
            "the form is asked for with the shown due date",
            RecurringFormRequest(recurringId = 1L, dueMillis = due.nextDueDate.time),
            viewModel.uiState.value.openForm
        )
        assertEquals(emptyList<Long>(), viewModel.listed())
        assertEquals(emptyList<Any>(), repository.writes)
        viewModel.onFormOpened()
        assertNull(viewModel.uiState.value.openForm)
    }

    @Test
    fun `a second add while the first is checking is ignored`() {
        val viewModel = loaded()
        val item = viewModel.uiState.value.items.single()
        val gate = CompletableDeferred<Unit>()
        repository.readGate = gate

        viewModel.add(item)
        viewModel.add(item)
        assertTrue(viewModel.uiState.value.isBusy)
        gate.complete(Unit)
        viewModel.onFormOpened()

        assertNull("the form is asked for only once", viewModel.uiState.value.openForm)
    }

    @Test
    fun `add on an occurrence that already moved on opens nothing`() {
        val viewModel = loaded()
        val stale = viewModel.uiState.value.items.single()
        // Recorded in the background, for example by auto-add, after the list was read.
        repository.items[1] = due.advanced().copy(nextDueDate = java.util.Date(Long.MAX_VALUE))

        viewModel.add(stale)

        assertNull(viewModel.uiState.value.openForm)
        assertEquals(DueRecurringMessage.ITEM_CHANGED, viewModel.uiState.value.message)
        assertEquals(emptyList<Long>(), viewModel.listed())
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `skip advances once and reloads the list`() {
        val viewModel = loaded()
        val item = viewModel.uiState.value.items.single()

        viewModel.skip(item)
        viewModel.skip(item)

        assertEquals(due.advanced().nextDueDate, repository.items.getValue(1).nextDueDate)
        assertEquals(1, repository.writes.size)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `a failed skip shows a message and leaves the item due`() {
        val viewModel = loaded()
        val item = viewModel.uiState.value.items.single()
        repository.readFailure = IOException("db")

        viewModel.skip(item)

        assertEquals(DueRecurringMessage.SKIP_FAILED, viewModel.uiState.value.message)
        assertEquals(emptyList<Any>(), repository.writes)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `a failed load shows a message and the banner can retry`() {
        repository.readFailure = IOException("db")

        val viewModel = loaded()

        assertEquals(DueRecurringMessage.LOAD_FAILED, viewModel.uiState.value.message)
        assertTrue("dismissed, so reopening reloads", session.recurringDismissed.value)
        assertEquals(emptyList<Any>(), repository.writes)
        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
    }
}
