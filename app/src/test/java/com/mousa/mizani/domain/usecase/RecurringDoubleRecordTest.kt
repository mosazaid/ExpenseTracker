package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.dao.RecurringTransactionDao
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.DepositCalculator
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.domain.calculator.advanced
import com.mousa.mizani.domain.repository.IAlertRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.withSettings
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer
import java.io.IOException

/** One due period must never be recorded twice, whoever gets to it first. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecurringDoubleRecordTest {

    private val due = recurringItem(1)
    private var inserts = 0

    private fun recordUseCase(
        repository: FakeRecurringRepository,
        lock: RecurringScheduleLock = RecurringScheduleLock(),
        runner: FakeTransactionRunner = FakeTransactionRunner(repository)
    ) = RecordRecurringOccurrenceUseCase(repository, lock, runner)

    @Test
    fun `record and advance run inside one transaction`() = runTest {
        val repository = FakeRecurringRepository(due)
        val runner = FakeTransactionRunner(repository)
        val steps = mutableListOf<String>()
        repository.onAdvance = { steps += "advance inTransaction=${runner.isActive}" }

        recordUseCase(repository, runner = runner)(1, due.nextDueDate.time) {
            steps += "record inTransaction=${runner.isActive}"
            1L
        }

        assertEquals(listOf("record inTransaction=true", "advance inTransaction=true"), steps)
        assertEquals(1, runner.commits)
    }

    @Test
    fun `a failed advance rolls the saved transaction back`() = runTest {
        var rows = 0
        val repository = FakeRecurringRepository(due)
        val runner = FakeTransactionRunner(repository) { rows = 0 }
        repository.advanceFailure = IOException("disk")

        val result = runCatching {
            recordUseCase(repository, runner = runner)(1, due.nextDueDate.time) { rows++; 1L }
        }

        assertEquals(IOException::class.java, result.exceptionOrNull()?.javaClass)
        assertEquals(1, runner.rollbacks)
        assertEquals(0, rows)
        assertEquals(due, repository.items.getValue(1))
    }

    @Test
    fun `an automatic insert and advance run inside one transaction`() = runTest {
        val repository = FakeRecurringRepository(due)
        val runner = FakeTransactionRunner(repository)
        val steps = mutableListOf<String>()
        repository.onAdvance = { steps += "advance inTransaction=${runner.isActive}" }
        val transactions: ITransactionRepository = mock(
            ITransactionRepository::class.java,
            withSettings().defaultAnswer(Answer {
                steps += "insert inTransaction=${runner.isActive}"
                1L
            })
        )

        ExecuteRecurringTransactionUseCase(transactions, repository, runner)(due)

        assertEquals(listOf("insert inTransaction=true", "advance inTransaction=true"), steps)
        assertEquals(1, runner.commits)
    }

    @Test
    fun `recording advances only after the transaction is saved`() = runTest {
        val repository = FakeRecurringRepository(due)

        val savedId = recordUseCase(repository)(1, due.nextDueDate.time) {
            assertEquals("not advanced while saving", emptyList<Any>(), repository.writes)
            42L
        }

        assertEquals(42L, savedId)
        assertEquals(due.advanced().nextDueDate, repository.items.getValue(1).nextDueDate)
    }

    @Test
    fun `a failed save leaves the item due`() = runTest {
        val repository = FakeRecurringRepository(due)

        val failure = runCatching {
            recordUseCase(repository)(1, due.nextDueDate.time) { throw IOException("disk") }
        }.exceptionOrNull()

        assertEquals(IOException::class.java, failure?.javaClass)
        assertEquals(emptyList<Any>(), repository.writes)
    }

    @Test
    fun `an occurrence that already moved on is not recorded again`() = runTest {
        val repository = FakeRecurringRepository(due.advanced())

        val savedId = recordUseCase(repository)(1, due.nextDueDate.time) { inserts++; 42L }

        assertNull(savedId)
        assertEquals(0, inserts)
        assertEquals(emptyList<Any>(), repository.writes)
    }

    @Test
    fun `two overlapping saves of one occurrence record it once`() = runTest {
        val repository = FakeRecurringRepository(due)
        val record = recordUseCase(repository)
        val insert = CompletableDeferred<Unit>()

        val first = async { record(1, due.nextDueDate.time) { insert.await(); inserts++; 1L } }
        val second = async { record(1, due.nextDueDate.time) { inserts++; 2L } }
        runCurrent()
        insert.complete(Unit)

        assertEquals(1L, first.await())
        assertNull(second.await())
        assertEquals(1, inserts)
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `leaving the screen while saving still advances the item`() = runTest {
        val repository = FakeRecurringRepository(due)
        val insert = CompletableDeferred<Unit>()
        val saving = launch {
            recordUseCase(repository)(1, due.nextDueDate.time) { insert.await(); inserts++; 1L }
        }
        runCurrent()

        saving.cancel()
        insert.complete(Unit)
        saving.join()

        assertEquals(1, inserts)
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `a skip waits for a save in progress and then does nothing`() = runTest {
        val repository = FakeRecurringRepository(due)
        val lock = RecurringScheduleLock()
        val insert = CompletableDeferred<Unit>()
        val saving = async { recordUseCase(repository, lock)(1, due.nextDueDate.time) { insert.await(); 1L } }
        val skipping = async { SkipRecurringOccurrenceUseCase(repository, lock)(1, due.nextDueDate.time) }
        runCurrent()
        insert.complete(Unit)

        assertEquals(1L, saving.await())
        assertEquals(false, skipping.await())
        assertEquals(1, repository.writes.size)
    }

    @Test
    fun `auto add rereads the item and does not record one that was handled meanwhile`() = runTest {
        val auto = recurringItem(1, autoAdd = true, type = com.mousa.mizani.data.database.entities.TransactionType.INCOME)
        val dao = mock(RecurringTransactionDao::class.java)
        val now = dateOf(2026, 3, 20)
        // The due list is stale: by the time the item is processed, the user has recorded it.
        `when`(dao.getDueRecurringTransactions(now)).thenReturn(listOf(auto))
        `when`(dao.getRecurringTransactionById(1)).thenReturn(auto.advanced())
        val transactions = mock(ITransactionRepository::class.java)
        val repository = FakeRecurringRepository(auto.advanced())
        val autoAdd = ExecuteAutoRecurringUseCase(
            dao,
            ExecuteRecurringTransactionUseCase(transactions, repository, FakeTransactionRunner(repository)),
            RecurringAccountBalance(
                mock(BalanceCalculator::class.java),
                mock(WalletCalculator::class.java),
                mock(DepositCalculator::class.java)
            ),
            transactions,
            mock(BudgetAlertUseCase::class.java),
            mock(IAlertRepository::class.java),
            RecurringScheduleLock()
        )

        val result = autoAdd(now)

        assertEquals(AutoDeductExecutionResult(), result)
        assertEquals(0, mockingDetails(transactions).invocations.size)
        assertEquals(emptyList<Any>(), repository.writes)
    }
}
