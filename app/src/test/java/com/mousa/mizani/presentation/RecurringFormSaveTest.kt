package com.mousa.mizani.presentation

import android.util.Log
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.domain.usecase.FakeTransactionRunner
import com.mousa.mizani.presentation.feature.add_transaction.AddEditUiEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.TimeFormatPreference
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.advanced
import com.mousa.mizani.domain.repository.ITransactionRepository
import com.mousa.mizani.domain.repository.ITripRepository
import com.mousa.mizani.domain.usecase.FakeRecurringRepository
import com.mousa.mizani.domain.usecase.RecordRecurringOccurrenceUseCase
import com.mousa.mizani.domain.usecase.RecurringScheduleLock
import com.mousa.mizani.domain.usecase.recurringItem
import com.mousa.mizani.presentation.feature.add_transaction.AddEditTransactionViewModel
import com.mousa.mizani.presentation.feature.add_transaction.SaveTransactionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.withSettings
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer
import java.util.Date

/** Saving the prefilled form of a recurring item. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecurringFormSaveTest {

    private val due = recurringItem(1)
    private val repository = FakeRecurringRepository(due)

    // Every insert succeeds with id 5.
    private val transactions: ITransactionRepository = mock(
        ITransactionRepository::class.java,
        withSettings().defaultAnswer(Answer { if (it.method.name.startsWith("insert")) 5L else null })
    )

    private var rolledBack = 0

    // A rollback also undoes the inserted transaction, counted here.
    private val runner = FakeTransactionRunner(repository) { rolledBack++ }

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

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

    private fun viewModel(): AddEditTransactionViewModel {
        val preferences = mock(UserPreferences::class.java)
        `when`(preferences.monthMode).thenReturn(flowOf(MonthMode.CALENDAR))
        `when`(preferences.timeFormat).thenReturn(flowOf(TimeFormatPreference.H12))
        `when`(preferences.sadaqahRoundUpEnabled).thenReturn(flowOf(false))
        val trips = mock(ITripRepository::class.java)
        `when`(trips.getAllTrips()).thenReturn(flowOf(emptyList()))
        `when`(trips.getActiveTrip()).thenReturn(flowOf(null))
        return AddEditTransactionViewModel(
            transactions, stub(), repository, stub(), trips, stub(), preferences, stub(), stub(),
            stub(), stub(), stub(), stub(), stub(), stub(),
            RecordRecurringOccurrenceUseCase(repository, RecurringScheduleLock(), runner)
        )
    }

    /** The screen's events, collected from before the next call on. */
    private fun TestScope.eventsOf(viewModel: AddEditTransactionViewModel): List<AddEditUiEvent> {
        val events = mutableListOf<AddEditUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }
        return events
    }

    private inline fun <reified T> stub(): T = mock(T::class.java)

    private val form = Transaction(
        amount = 10.0, description = "rent", date = Date(), type = TransactionType.INCOME,
        categoryId = null, accountType = due.accountType
    )

    private fun insertCount() =
        mockingDetails(transactions).invocations.count { it.method.name.startsWith("insert") }

    @Test
    fun `saving records once and advances once`() = runTest {
        val viewModel = viewModel()
        viewModel.prefillRecurring(1)

        val result = viewModel.saveTransaction(form)

        assertEquals(SaveTransactionResult.Saved(5L), result)
        assertEquals(1, insertCount())
        assertEquals(due.advanced().nextDueDate, repository.items.getValue(1).nextDueDate)
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `a matching expected date prefills and saves that period`() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.prefillRecurring(1, due.nextDueDate.time))

        assertEquals(1L, viewModel.uiState.value.pendingRecurringId)
        assertEquals(due.nextDueDate.time, viewModel.uiState.value.pendingRecurringDueMillis)
        assertEquals(SaveTransactionResult.Saved(5L), viewModel.saveTransaction(form))
    }

    @Test
    fun `without an expected date the current period is prefilled`() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.prefillRecurring(1, null))

        assertEquals(due.nextDueDate.time, viewModel.uiState.value.pendingRecurringDueMillis)
    }

    @Test
    fun `a stale expected date prefills nothing and says so`() = runTest {
        // For example an old notification, after its period was recorded.
        repository.items[1] = due.advanced()
        val viewModel = viewModel()
        val events = eventsOf(viewModel)

        assertFalse(viewModel.prefillRecurring(1, due.nextDueDate.time))

        assertNull(viewModel.uiState.value.pendingRecurringId)
        assertEquals("", viewModel.uiState.value.amount)
        assertEquals("", viewModel.uiState.value.description)
        assertEquals(listOf<AddEditUiEvent>(AddEditUiEvent.RecurringAlreadyHandled), events)
        assertEquals(0, insertCount())
        assertEquals(emptyList<Any>(), repository.writes)
    }

    @Test
    fun `a rotation does not repeat the stale message or clear what was typed`() = runTest {
        repository.items[1] = due.advanced()
        val viewModel = viewModel()
        val events = eventsOf(viewModel)
        viewModel.prefillRecurring(1, due.nextDueDate.time)
        viewModel.updateAmount("3")

        viewModel.prefillRecurring(1, due.nextDueDate.time)

        assertEquals("3", viewModel.uiState.value.amount)
        assertEquals(1, events.size)
    }

    @Test
    fun `a second tap shows no already-handled message`() = runTest {
        val viewModel = viewModel()
        val events = eventsOf(viewModel)
        viewModel.prefillRecurring(1)
        val gate = CompletableDeferred<Unit>()
        repository.readGate = gate

        val first = async(Dispatchers.Main) { viewModel.saveTransaction(form) }
        viewModel.saveTransaction(form)
        gate.complete(Unit)
        first.await()
        viewModel.saveTransaction(form)

        assertEquals(emptyList<AddEditUiEvent>(), events)
    }

    @Test
    fun `a failed advance records nothing and leaves Save usable`() = runTest {
        val viewModel = viewModel()
        val events = eventsOf(viewModel)
        viewModel.prefillRecurring(1)
        repository.advanceFailure = java.io.IOException("disk")

        assertEquals(SaveTransactionResult.Failed, viewModel.saveTransaction(form))

        assertEquals("the insert was rolled back", 1, rolledBack)
        assertEquals(listOf<AddEditUiEvent>(AddEditUiEvent.RecurringSaveFailed), events)
        assertEquals(due, repository.items.getValue(1))

        repository.advanceFailure = null
        assertEquals(SaveTransactionResult.Saved(5L), viewModel.saveTransaction(form))
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `a double tap on save records the occurrence once`() = runTest {
        val viewModel = viewModel()
        viewModel.prefillRecurring(1)
        val gate = CompletableDeferred<Unit>()
        repository.readGate = gate

        val first = async(Dispatchers.Main) { viewModel.saveTransaction(form) }
        val second = viewModel.saveTransaction(form)
        gate.complete(Unit)

        assertEquals(SaveTransactionResult.Ignored, second)
        assertEquals(SaveTransactionResult.Saved(5L), first.await())
        assertEquals(1, insertCount())
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `a late second save after the form reset records nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.prefillRecurring(1)
        viewModel.saveTransaction(form)
        viewModel.resetForm()

        assertEquals(SaveTransactionResult.Ignored, viewModel.saveTransaction(form))
        assertEquals(1, insertCount())
    }

    @Test
    fun `saving an occurrence handled elsewhere records nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.prefillRecurring(1)
        repository.items[1] = due.advanced()

        val result = viewModel.saveTransaction(form)

        assertEquals(SaveTransactionResult.AlreadyHandled, result)
        assertEquals(0, insertCount())
        assertEquals(emptyList<Any>(), repository.writes)
    }

    @Test
    fun `recreating the screen does not prefill the next period`() = runTest {
        val viewModel = viewModel()
        viewModel.prefillRecurring(1)
        viewModel.saveTransaction(form)

        // A rotation runs the screen's prefill again on the same ViewModel.
        viewModel.prefillRecurring(1)

        assertEquals(SaveTransactionResult.Ignored, viewModel.saveTransaction(form))
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `a failed save can be tried again`() = runTest {
        val viewModel = viewModel()
        viewModel.prefillRecurring(1)
        repository.readFailure = java.io.IOException("db")
        assertEquals(SaveTransactionResult.Failed, viewModel.saveTransaction(form))
        repository.readFailure = null

        assertEquals(SaveTransactionResult.Saved(5L), viewModel.saveTransaction(form))
        assertEquals(1, insertCount())
    }
}
