package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Date

class DepositAndWalletTest {

    private class FakeTransactionRepo : ITransactionRepository {
        val txns = mutableListOf<Transaction>()

        override suspend fun insertTransaction(transaction: Transaction): Long {
            val id = (txns.size + 1).toLong()
            txns.add(transaction.copy(id = id))
            return id
        }

        override suspend fun updateTransaction(transaction: Transaction) {
            val index = txns.indexOfFirst { it.id == transaction.id }
            if (index != -1) txns[index] = transaction
        }

        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txns)
        override suspend fun getAllTransactionsSnapshot(): List<Transaction> = txns.toList()
        override suspend fun getTransactionById(id: Long): Transaction? = txns.find { it.id == id }
        override fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>> = flowOf(txns)
        override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = flowOf(txns.filter { it.type == type })
        override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = flowOf(txns.filter { it.categoryId == categoryId })
        override suspend fun getTotalAmountByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double =
            txns.filter { it.type == type }.sumOf { it.amount }
        override suspend fun getTotalAmountByCategoryAndDateRange(categoryId: Long, startDate: Date, endDate: Date): Double = 0.0
        override suspend fun deleteTransaction(transaction: Transaction) { txns.removeIf { it.id == transaction.id } }
        override suspend fun deleteTransactionById(id: Long) { txns.removeIf { it.id == id } }
        override suspend fun getUnreimbursedExpenses(startDate: Date, endDate: Date): List<Transaction> = emptyList()
        override suspend fun hasLinkedDeptIncome(expenseId: Long): Boolean = false
        override suspend fun getDeptIncomesLinkedTo(expenseId: Long): List<Transaction> = emptyList()
        override suspend fun getTotalReimbursedAmountForExpense(expenseId: Long): Double = 0.0
        override fun getTotalReimbursedAmountForExpenseFlow(expenseId: Long): Flow<Double> = flowOf(0.0)
        override suspend fun getExpenseCountBetweenDates(startDate: Date, endDate: Date): Int = txns.size
        override fun getOutstandingLentDebtsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getOutstandingBorrowedDebtsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getAllOutstandingDebtsFlow(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun getAllOutstandingDebtsSnapshot(): List<Transaction> = emptyList()
        override suspend fun getTransactionsBetweenDatesSnapshot(startDate: Date, endDate: Date): List<Transaction> = txns.toList()

        override suspend fun getTotalIncomeForAccount(account: AccountType): Double =
            txns.filter { it.type == TransactionType.INCOME && it.accountType == account }.sumOf { it.amount }

        override suspend fun getTotalExpenseForAccount(account: AccountType): Double =
            txns.filter { it.type == TransactionType.EXPENSE && it.accountType == account }.sumOf { it.amount }

        override suspend fun getTotalTransferOut(account: AccountType): Double =
            txns.filter { it.type == TransactionType.TRANSFER && it.accountType == account }.sumOf { it.amount }

        override suspend fun getTotalTransferIn(account: AccountType): Double =
            txns.filter { it.type == TransactionType.TRANSFER && it.toAccountType == account }.sumOf { it.amount }

        override suspend fun getTotalWalletMoveOut(account: AccountType): Double =
            txns.filter { it.type == TransactionType.WALLET_MOVE && it.accountType == account }.sumOf { it.amount }

        override suspend fun getTotalWalletMoveIn(account: AccountType): Double =
            txns.filter { it.type == TransactionType.WALLET_MOVE && it.toAccountType == account }.sumOf { it.amount }

        override suspend fun getTotalIncomeForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double =
            txns.filter { it.type == TransactionType.INCOME && it.accountType == account && it.id != excludeTransactionId }.sumOf { it.amount }

        override suspend fun getTotalExpenseForAccountExcluding(account: AccountType, excludeTransactionId: Long): Double =
            txns.filter { it.type == TransactionType.EXPENSE && it.accountType == account && it.id != excludeTransactionId }.sumOf { it.amount }

        override suspend fun getTotalTransferOutExcluding(account: AccountType, excludeTransactionId: Long): Double =
            txns.filter { it.type == TransactionType.TRANSFER && it.accountType == account && it.id != excludeTransactionId }.sumOf { it.amount }

        override suspend fun getTotalTransferInExcluding(account: AccountType, excludeTransactionId: Long): Double =
            txns.filter { it.type == TransactionType.TRANSFER && it.toAccountType == account && it.id != excludeTransactionId }.sumOf { it.amount }

        override suspend fun getTotalWalletMoveOutExcluding(account: AccountType, excludeTransactionId: Long): Double =
            txns.filter { it.type == TransactionType.WALLET_MOVE && it.accountType == account && it.id != excludeTransactionId }.sumOf { it.amount }

        override suspend fun getTotalWalletMoveInExcluding(account: AccountType, excludeTransactionId: Long): Double =
            txns.filter { it.type == TransactionType.WALLET_MOVE && it.toAccountType == account && it.id != excludeTransactionId }.sumOf { it.amount }

        override suspend fun getPeriodIncomeForAccount(account: AccountType, startDate: Date, endDate: Date): Double =
            getTotalIncomeForAccount(account)

        override suspend fun getPeriodExpenseForAccount(account: AccountType, startDate: Date, endDate: Date): Double =
            getTotalExpenseForAccount(account)

        override suspend fun getPeriodTransferOut(account: AccountType, startDate: Date, endDate: Date): Double =
            getTotalTransferOut(account)

        override suspend fun getPeriodTransferIn(account: AccountType, startDate: Date, endDate: Date): Double =
            getTotalTransferIn(account)

        override suspend fun getPeriodWalletIn(startDate: Date, endDate: Date): Double =
            getTotalWalletMoveIn(AccountType.WALLET)

        override suspend fun getPeriodWalletOut(startDate: Date, endDate: Date): Double =
            getTotalWalletMoveOut(AccountType.WALLET)

        override suspend fun getSalaryPeriodAnchors(salaryCategoryId: Long): List<Transaction> = emptyList()
        override suspend fun getLatestSalaryIncomeBefore(beforeDate: Date, salaryCategoryId: Long): Transaction? = null
        override suspend fun setDebtSettled(id: Long, settled: Boolean) {}
        override fun getTransactionsForRecurringFlow(recurringId: Long, description: String, type: TransactionType): Flow<List<Transaction>> = flowOf(emptyList())
        override fun getSplitsForTransaction(transactionId: Long): Flow<List<com.example.expensetracker.data.database.entities.TransactionSplit>> = flowOf(emptyList())
        override suspend fun getSplitsForTransactionSnapshot(transactionId: Long): List<com.example.expensetracker.data.database.entities.TransactionSplit> = emptyList()
        override suspend fun insertTransactionWithSplits(transaction: Transaction, splits: List<com.example.expensetracker.data.database.entities.TransactionSplit>): Long = insertTransaction(transaction)
        override suspend fun updateTransactionWithSplits(transaction: Transaction, splits: List<com.example.expensetracker.data.database.entities.TransactionSplit>) = updateTransaction(transaction)
        override suspend fun setDebtSplitSettled(splitId: Long, settled: Boolean) {}
        override fun getUnsettledDebtSplitsFlow(): Flow<List<com.example.expensetracker.data.database.entities.TransactionSplit>> = flowOf(emptyList())
        override fun getAllSplitsFlow(): Flow<List<com.example.expensetracker.data.database.entities.TransactionSplit>> = flowOf(emptyList())
        override suspend fun getAllSplitsSnapshot(): List<com.example.expensetracker.data.database.entities.TransactionSplit> = emptyList()
    }

    @Test
    fun depositCalculator_calculatesBalanceAndSummaryAccurately() = runBlocking {
        val repo = FakeTransactionRepo()
        val calculator = DepositCalculator(repo)

        // 1. Initial Deposit inflow: 10,000 JOD
        repo.insertTransaction(
            Transaction(
                amount = 10000.0,
                description = "Master Capital Deposit",
                date = Date(),
                type = TransactionType.INCOME,
                accountType = AccountType.DEPOSIT
            )
        )

        // 2. Buy from Deposit: 2,500 JOD
        repo.insertTransaction(
            Transaction(
                amount = 2500.0,
                description = "Land Investment Share",
                date = Date(),
                type = TransactionType.EXPENSE,
                accountType = AccountType.DEPOSIT,
                excludeFromMonthExpenses = true
            )
        )

        // 3. Transfer from Deposit to Wallet: 500 JOD
        repo.insertTransaction(
            Transaction(
                amount = 500.0,
                description = "Transfer to Pocket Wallet",
                date = Date(),
                type = TransactionType.WALLET_MOVE,
                accountType = AccountType.DEPOSIT,
                toAccountType = AccountType.WALLET,
                excludeFromMonthExpenses = true
            )
        )

        val balance = calculator.getDepositBalance()
        assertEquals(7000.0, balance, 0.001)

        val summary = calculator.getDepositSummary(Date(), Date())
        assertEquals(10000.0, summary.totalDeposited, 0.001)
        assertEquals(2500.0, summary.totalCapitalPurchases, 0.001)
        assertEquals(500.0, summary.totalTransferredOut, 0.001)
        assertEquals(2500.0, summary.yearCapitalPurchases, 0.001)
    }

    @Test
    fun balanceCalculator_excludesDepositAndExcludedExpensesFromMonthSummary() = runBlocking {
        val repo = FakeTransactionRepo()
        val mockPrefs = mock(UserPreferences::class.java)
        val balanceCalculator = BalanceCalculator(repo, mockPrefs)

        val txns = listOf(
            // Regular Bank Salary
            Transaction(
                amount = 1200.0,
                description = "Monthly Salary",
                date = Date(),
                type = TransactionType.INCOME,
                accountType = AccountType.BANK
            ),
            // Regular Cash Expense
            Transaction(
                amount = 300.0,
                description = "Supermarket Grocery",
                date = Date(),
                type = TransactionType.EXPENSE,
                accountType = AccountType.CASH
            ),
            // Deposit Capital Purchase (MUST BE EXCLUDED)
            Transaction(
                amount = 5000.0,
                description = "Car Downpayment from Vault",
                date = Date(),
                type = TransactionType.EXPENSE,
                accountType = AccountType.DEPOSIT,
                excludeFromMonthExpenses = true
            ),
            // Wallet Debt Expense marked excludeFromMonthExpenses (MUST BE EXCLUDED)
            Transaction(
                amount = 150.0,
                description = "Lent to colleague from pocket wallet",
                date = Date(),
                type = TransactionType.EXPENSE,
                accountType = AccountType.WALLET,
                excludeFromMonthExpenses = true
            )
        )

        val currentBalances = AccountBalances(cash = 500.0, bank = 900.0)
        val summary = balanceCalculator.computeMonthFinancialSummary(txns, currentBalances)

        // Month income should only be 1200.0
        assertEquals(1200.0, summary.periodIncome, 0.001)
        // Month expense should ONLY be 300.0 (5000 and 150 excluded!)
        assertEquals(300.0, summary.periodExpense, 0.001)
        // Net should be 1200 - 300 = 900
        assertEquals(900.0, summary.periodNet, 0.001)
    }

    @Test
    fun walletCalculator_deductsDirectExpensesAndAllowsDepositTransfer() = runBlocking {
        val repo = FakeTransactionRepo()
        val mockCategoryRepo = mock(ICategoryRepository::class.java)
        val mockPeriodCalculator = mock(PeriodCalculator::class.java)
        val walletCalculator = WalletCalculator(repo, mockCategoryRepo, mockPeriodCalculator)

        // Transfer into wallet: 400 JOD
        repo.insertTransaction(
            Transaction(
                amount = 400.0,
                description = "Bank to Wallet Move",
                date = Date(),
                type = TransactionType.WALLET_MOVE,
                accountType = AccountType.BANK,
                toAccountType = AccountType.WALLET
            )
        )

        // Direct expense spent from wallet: 70 JOD
        repo.insertTransaction(
            Transaction(
                amount = 70.0,
                description = "Pocket Coffee & Lunch",
                date = Date(),
                type = TransactionType.EXPENSE,
                accountType = AccountType.WALLET,
                excludeFromMonthExpenses = true
            )
        )

        val balance = walletCalculator.getCumulativeWalletBalance()
        assertEquals(330.0, balance, 0.001)

        // Validation permits Deposit transfer
        assertTrue(walletCalculator.validateWalletMove(AccountType.WALLET, AccountType.DEPOSIT))
        assertTrue(walletCalculator.validateWalletMove(AccountType.DEPOSIT, AccountType.WALLET))
    }
}
