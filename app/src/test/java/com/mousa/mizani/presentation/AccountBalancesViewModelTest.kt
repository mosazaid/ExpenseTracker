package com.mousa.mizani.presentation

import android.util.Log
import com.mousa.mizani.R
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.BalanceCorrection
import com.mousa.mizani.presentation.feature.settings.accounts.AccountBalancesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer
import java.io.IOException
import kotlin.coroutines.Continuation

@OptIn(ExperimentalCoroutinesApi::class)
class AccountBalancesViewModelTest {

    private val balanceCalculator = mock(BalanceCalculator::class.java)
    private val dispatcher = UnconfinedTestDispatcher()
    private val change = BalanceCorrection.Change(old = 5.0, new = 20.0, difference = 15.0)

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        log = mockStatic(Log::class.java)
        stubBalances(cash = 5.0, bank = 100.0)
    }

    @After
    fun tearDown() {
        log.close()
        Dispatchers.resetMain()
    }

    private fun stubBalances(cash: Double, bank: Double) = runBlocking<Unit> {
        `when`(balanceCalculator.getAvailableBalance(AccountType.CASH)).thenReturn(cash)
        `when`(balanceCalculator.getAvailableBalance(AccountType.BANK)).thenReturn(bank)
    }

    private fun viewModel() = AccountBalancesViewModel(balanceCalculator)

    private fun writes(): List<List<Any?>> = mockingDetails(balanceCalculator).invocations
        .filter { it.method.name == "setCurrentBalance" }
        .map { it.rawArguments.take(2) }

    private fun <T> TestScope.collect(flow: SharedFlow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { values.add(it) }
        }
        return values
    }

    @Test
    fun `loads cash and bank balances on creation`() = runTest(dispatcher) {
        assertEquals(
            mapOf(AccountType.CASH to 5.0, AccountType.BANK to 100.0),
            viewModel().balances.value
        )
    }

    @Test
    fun `refresh picks up new balances`() = runTest(dispatcher) {
        val viewModel = viewModel()
        stubBalances(cash = 8.0, bank = 90.0)

        viewModel.refresh()

        assertEquals(
            mapOf(AccountType.CASH to 8.0, AccountType.BANK to 90.0),
            viewModel.balances.value
        )
    }

    @Test
    fun `apply for cash calls setCurrentBalance for CASH only`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.apply(AccountType.CASH, change)

        assertEquals(listOf(listOf<Any?>(AccountType.CASH, 20.0)), writes())
    }

    @Test
    fun `apply reloads the balances after writing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        `when`(balanceCalculator.setCurrentBalance(AccountType.BANK, 20.0)).thenAnswer {
            stubBalances(cash = 5.0, bank = 20.0)
        }

        viewModel.apply(AccountType.BANK, change)

        assertEquals(20.0, viewModel.balances.value[AccountType.BANK])
    }

    @Test
    fun `apply never writes a non-finite target`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.apply(AccountType.CASH, change.copy(new = Double.POSITIVE_INFINITY))
        viewModel.apply(AccountType.CASH, change.copy(new = Double.NaN))

        assertEquals(emptyList<List<Any?>>(), writes())
        assertEquals(List(2) { R.string.balance_correction_failed }, errors)
    }

    @Test
    fun `apply does not write when the stored balance is non-finite`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)
        stubBalances(cash = Double.NaN, bank = 100.0)

        viewModel.apply(AccountType.CASH, change)

        assertEquals(emptyList<List<Any?>>(), writes())
        assertEquals(listOf(R.string.balance_correction_failed), errors)
    }

    @Test
    fun `apply does nothing for accounts without an opening balance`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.apply(AccountType.WALLET, change)
        viewModel.apply(AccountType.DEPOSIT, change)

        assertEquals(emptyList<List<Any?>>(), writes())
    }

    @Test
    fun `a failed write emits an error and keeps the app running`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)
        `when`(balanceCalculator.setCurrentBalance(AccountType.CASH, 20.0))
            .thenAnswer { throw IOException("disk full") }

        viewModel.apply(AccountType.CASH, change)

        assertEquals(listOf(R.string.balance_correction_failed), errors)
        assertEquals(5.0, viewModel.balances.value[AccountType.CASH])
    }

    @Test
    fun `a cancelled write is not reported as an error`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)
        `when`(balanceCalculator.setCurrentBalance(AccountType.CASH, 20.0))
            .thenAnswer { throw CancellationException() }

        viewModel.apply(AccountType.CASH, change)

        assertEquals(emptyList<Int>(), errors)
    }

    @Test
    fun `a failed refresh drops the stale balances and emits an error`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)
        `when`(balanceCalculator.getAvailableBalance(AccountType.BANK))
            .thenAnswer { throw IOException("database closed") }

        viewModel.refresh()

        assertEquals(emptyMap<AccountType, Double>(), viewModel.balances.value)
        assertEquals(listOf(R.string.balance_load_failed), errors)
    }

    @Test
    fun `a cancelled refresh is not reported as an error`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)
        `when`(balanceCalculator.getAvailableBalance(AccountType.BANK))
            .thenAnswer { throw CancellationException() }

        viewModel.refresh()

        assertEquals(emptyList<Int>(), errors)
    }

    @Test
    fun `a second correction waits for the first to finish`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        `when`(balanceCalculator.setCurrentBalance(AccountType.CASH, 20.0))
            .thenAnswer(suspendingAnswer { gate.await() })
        val viewModel = viewModel()

        viewModel.apply(AccountType.CASH, change)
        viewModel.apply(AccountType.BANK, change)
        assertEquals(1, writes().size)

        gate.complete(Unit)

        assertEquals(2, writes().size)
    }

    @Test
    fun `a slow error collector does not block later corrections`() = runTest(dispatcher) {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            // Never finishes handling the first error, so the next emit has to wait.
            viewModel.errors.collect { awaitCancellation() }
        }
        `when`(balanceCalculator.setCurrentBalance(AccountType.CASH, 20.0))
            .thenAnswer { throw IOException("disk full") }

        viewModel.apply(AccountType.CASH, change)
        viewModel.apply(AccountType.CASH, change)
        viewModel.apply(AccountType.BANK, change)

        assertEquals(listOf<Any?>(AccountType.BANK, 20.0), writes().last())
    }

    /** Makes a mocked suspend function really suspend inside [block]. */
    @Suppress("UNCHECKED_CAST")
    private fun suspendingAnswer(block: suspend () -> Unit) = Answer<Any?> { invocation ->
        val continuation = invocation.rawArguments.last() as Continuation<Unit>
        (block as (Continuation<Unit>) -> Any?).invoke(continuation)
    }
}
