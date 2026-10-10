package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.database.entities.*
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class UseCaseTest {

    private open class BaseFakeTransactionRepo : ITransactionRepository {
        val insertedTransactions = mutableListOf<Transaction>()
        val updatedTransactions = mutableListOf<Transaction>()

        override suspend fun insertTransaction(transaction: Transaction): Long {
            val id = if (transaction.id == 0L) (insertedTransactions.size + 1).toLong() else transaction.id
            insertedTransactions.add(transaction.copy(id = id))
            return id
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            updatedTransactions.add(transaction)
        }

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getAllTransactionsSnapshot(): List<Transaction> = emptyList()
        override suspend fun getTransactionById(id: Long): Transaction? = insertedTransactions.find { it.id == id }
        override fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getTotalAmountByCategoryAndDateRange(categoryId: Long, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun getUnreimbursedExpenses(startDate: Date, endDate: Date): List<Transaction> = emptyList()
        override suspend fun hasLinkedDeptIncome(expenseId: Long): Boolean = false
        override suspend fun getDeptIncomesLinkedTo(expenseId: Long): List<Transaction> = emptyList()
        override suspend fun getTotalReimbursedAmountForExpense(expenseId: Long): Double = 0.0
        override fun getTotalReimbursedAmountForExpenseFlow(expenseId: Long): Flow<Double> = flowOf(0.0)
        override suspend fun getExpenseCountBetweenDates(startDate: Date, endDate: Date): Int = 0
        override fun getOutstandingLentDebtsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getOutstandingBorrowedDebtsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getAllOutstandingDebtsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getAllOutstandingDebtsSnapshot(): List<Transaction> = emptyList()
        override suspend fun getTransactionsBetweenDatesSnapshot(startDate: Date, endDate: Date): List<Transaction> = emptyList()
        override suspend fun getTotalIncomeForAccount(account: AccountType): Double = 0.0
        override suspend fun getTotalExpenseForAccount(account: AccountType): Double = 0.0
        override suspend fun getTotalTransferOut(account: AccountType): Double = 0.0
        override suspend fun getTotalTransferIn(account: AccountType): Double = 0.0
        override suspend fun getTotalWalletMoveOut(account: AccountType): Double = 0.0
        override suspend fun getTotalWalletMoveIn(account: AccountType): Double = 0.0
        override suspend fun getTotalIncomeForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double = 0.0
        override suspend fun getTotalExpenseForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double = 0.0
        override suspend fun getTotalTransferOutExcluding(account: AccountType, excludeTransactionId: Long): Double = 0.0
        override suspend fun getTotalTransferInExcluding(account: AccountType, excludeTransactionId: Long): Double = 0.0
        override suspend fun getTotalWalletMoveOutExcluding(account: AccountType, excludeTransactionId: Long): Double = 0.0
        override suspend fun getTotalWalletMoveInExcluding(account: AccountType, excludeTransactionId: Long): Double = 0.0
        override suspend fun getPeriodIncomeForAccount(account: AccountType, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getPeriodExpenseForAccount(account: AccountType, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getPeriodTransferOut(account: AccountType, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getPeriodTransferIn(account: AccountType, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getPeriodWalletIn(startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getPeriodWalletOut(startDate: Date, endDate: Date): Double = 0.0
        override suspend fun getSalaryPeriodAnchors(salaryCategoryId: Long): List<Transaction> = emptyList()
        override suspend fun getLatestSalaryIncomeBefore(beforeDate: Date, salaryCategoryId: Long): Transaction? = null
        override suspend fun setDebtSettled(id: Long, settled: Boolean) {}
        override fun getTransactionsForRecurringFlow(recurringId: Long, description: String, type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSplitsForTransaction(transactionId: Long): Flow<List<TransactionSplit>> = flowOf(emptyList())
        override suspend fun getSplitsForTransactionSnapshot(transactionId: Long): List<TransactionSplit> = emptyList()
        override suspend fun insertTransactionWithSplits(transaction: Transaction, splits: List<TransactionSplit>): Long = insertTransaction(transaction)
        override suspend fun updateTransactionWithSplits(transaction: Transaction, splits: List<TransactionSplit>) = updateTransaction(transaction)
        override suspend fun setDebtSplitSettled(splitId: Long, settled: Boolean) {}
        override fun getUnsettledDebtSplitsFlow(): Flow<List<TransactionSplit>> = flowOf(emptyList())
        override fun getAllSplitsFlow(): Flow<List<TransactionSplit>> = flowOf(emptyList())
        override suspend fun getAllSplitsSnapshot(): List<TransactionSplit> = emptyList()
    }

    private open class BaseFakeRecurringRepo : IRecurringRepository {
        var advancedRecurring: RecurringTransaction? = null

        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(emptyList())
        override suspend fun getDueReminders(date: Date): List<RecurringTransaction> = emptyList()
        override suspend fun getRecurringById(id: Long): RecurringTransaction? = null
        override suspend fun createSalaryReminder(amount: Double, accountType: AccountType, salaryCategoryId: Long, dayOfMonth: Int, description: String): Long = 1L
        override suspend fun advanceRecurringDueDate(recurring: RecurringTransaction) {
            advancedRecurring = recurring
        }
        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(emptyList())
        override suspend fun deactivateRecurring(id: Long) {}
        override suspend fun setRecurringActive(id: Long, isActive: Boolean) {}
        override suspend fun deleteRecurringById(id: Long) {}
        override suspend fun updateRecurring(recurring: RecurringTransaction) {}
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction): Long = 1L
    }

    @Test
    fun addTransactionUseCase_validatesAmountAndInserts() = runBlocking {
        val repo = BaseFakeTransactionRepo()
        val useCase = AddTransactionUseCase(repo)
        val txn = Transaction(
            amount = 50.0,
            description = "Groceries",
            date = Date(),
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            accountType = AccountType.CASH
        )

        val id = useCase(txn)
        assertEquals(1L, id)
        assertEquals(1, repo.insertedTransactions.size)
        assertEquals(50.0, repo.insertedTransactions.first().amount, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTransactionUseCase_rejectsZeroAmount() {
        runBlocking {
            val repo = BaseFakeTransactionRepo()
            val useCase = AddTransactionUseCase(repo)
            val txn = Transaction(
                amount = 0.0,
                description = "Invalid",
                date = Date(),
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                accountType = AccountType.CASH
            )
            useCase(txn)
        }
    }

    @Test
    fun settleDebtUseCase_marksSettledAndCreatesCounterTransaction() = runBlocking {
        val repo = BaseFakeTransactionRepo()
        val useCase = SettleDebtUseCase(repo)
        val originalDebt = Transaction(
            id = 55L,
            amount = 120.0,
            description = "Lent to Ahmad",
            date = Date(),
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            accountType = AccountType.CASH,
            debtType = "LENT",
            debtorNote = "Ahmad"
        )

        val counterId = useCase(originalDebt, AccountType.BANK, createCounterTransaction = true)
        assertEquals(1L, counterId)
        assertTrue(repo.updatedTransactions.any { it.id == 55L && it.isDebtSettled })
        assertTrue(repo.insertedTransactions.any { it.type == TransactionType.INCOME && it.amount == 120.0 })
    }

    @Test
    fun executeRecurringTransactionUseCase_insertsTransactionAndAdvancesDate() = runBlocking {
        val txnRepo = BaseFakeTransactionRepo()
        val recurringRepo = BaseFakeRecurringRepo()
        val useCase = ExecuteRecurringTransactionUseCase(txnRepo, recurringRepo)
        val recurring = RecurringTransaction(
            id = 10L,
            amount = 8.99,
            description = "Streaming",
            type = TransactionType.EXPENSE,
            categoryId = 3L,
            accountType = AccountType.BANK,
            frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = Date()
        )

        val txnId = useCase(recurring)
        assertEquals(1L, txnId)
        assertEquals(10L, txnRepo.insertedTransactions.first().recurringId)
        assertEquals(10L, recurringRepo.advancedRecurring?.id)
    }
}
