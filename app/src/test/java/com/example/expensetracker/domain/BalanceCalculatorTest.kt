package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.ITransactionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

class BalanceCalculatorTest {

    private lateinit var transactionRepository: ITransactionRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var balanceCalculator: BalanceCalculator

    @Before
    fun setup() {
        transactionRepository = Mockito.mock(ITransactionRepository::class.java)
        userPreferences = Mockito.mock(UserPreferences::class.java)
        balanceCalculator = BalanceCalculator(transactionRepository, userPreferences)
    }

    @Test
    fun testComputeMonthFinancialSummary_incomeAndExpenses() {
        val now = Date()
        val currentBalances = AccountBalances(cash = 200.0, bank = 800.0)

        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 1000.0,
                type = TransactionType.INCOME,
                categoryId = null,
                accountType = AccountType.BANK,
                date = now,
                description = "Salary"
            ),
            Transaction(
                id = 2L,
                amount = 150.0,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                accountType = AccountType.CASH,
                date = now,
                description = "Groceries"
            ),
            Transaction(
                id = 3L,
                amount = 50.0,
                type = TransactionType.EXPENSE,
                categoryId = 2L,
                accountType = AccountType.BANK,
                date = now,
                description = "Internet"
            )
        )

        val summary = balanceCalculator.computeMonthFinancialSummary(txns, currentBalances)

        assertEquals(1000.0, summary.periodIncome, 0.001)
        assertEquals(200.0, summary.periodExpense, 0.001)
        assertEquals(800.0, summary.periodNet, 0.001)
        assertEquals(-150.0, summary.incomeExpenseByAccount.cash, 0.001)
        assertEquals(950.0, summary.incomeExpenseByAccount.bank, 0.001)
        assertFalse(summary.hasTransfers)
    }

    @Test
    fun testComputeMonthFinancialSummary_transferNeutrality() {
        val now = Date()
        val currentBalances = AccountBalances(cash = 500.0, bank = 1500.0)

        // Transfer 200 from Bank to Cash
        val transferTxn = Transaction(
            id = 1L,
            amount = 200.0,
            type = TransactionType.TRANSFER,
            categoryId = null,
            accountType = AccountType.BANK,
            toAccountType = AccountType.CASH,
            date = now,
            description = "ATM Withdrawal"
        )

        val summary = balanceCalculator.computeMonthFinancialSummary(listOf(transferTxn), currentBalances)

        // Transfers do not affect period income, expense, or net
        assertEquals(0.0, summary.periodIncome, 0.001)
        assertEquals(0.0, summary.periodExpense, 0.001)
        assertEquals(0.0, summary.periodNet, 0.001)

        // Transfer impact balances to zero overall
        assertEquals(200.0, summary.transferImpact.cash, 0.001)
        assertEquals(-200.0, summary.transferImpact.bank, 0.001)
        assertEquals(0.0, summary.transferImpact.cash + summary.transferImpact.bank, 0.001)
        assertTrue(summary.hasTransfers)

        // Period change reflects the transfer
        assertEquals(200.0, summary.periodChangeByAccount.cash, 0.001)
        assertEquals(-200.0, summary.periodChangeByAccount.bank, 0.001)
    }

    @Test
    fun testComputeMonthFinancialSummary_walletMove() {
        val now = Date()
        val currentBalances = AccountBalances(cash = 300.0, bank = 1000.0)

        // Move 100 from Cash to Wallet
        val walletTxn = Transaction(
            id = 1L,
            amount = 100.0,
            type = TransactionType.WALLET_MOVE,
            categoryId = null,
            accountType = AccountType.CASH,
            toAccountType = AccountType.WALLET,
            date = now,
            description = "Pocket Money"
        )

        val summary = balanceCalculator.computeMonthFinancialSummary(listOf(walletTxn), currentBalances)

        assertEquals(-100.0, summary.transferImpact.cash, 0.001)
        assertEquals(-100.0, summary.periodChangeByAccount.cash, 0.001)
    }

    @Test
    fun testComputeMonthFinancialSummary_loanDeductedFromIncome() {
        val now = Date()
        val currentBalances = AccountBalances(cash = 0.0, bank = 700.0)

        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 1000.0,
                type = TransactionType.INCOME,
                categoryId = null,
                accountType = AccountType.BANK,
                date = now,
                description = "Salary"
            ),
            Transaction(
                id = 2L,
                amount = 300.0,
                type = TransactionType.EXPENSE,
                categoryId = 10L,
                accountType = AccountType.BANK,
                date = now,
                description = "Loan payment: Car"
            )
        )

        val paidLoan = com.example.expensetracker.data.database.dao.PaidLoanInfo(
            paymentId = 1L,
            loanConfigId = 1L,
            loanName = "Car Loan",
            amount = 300.0,
            accountType = AccountType.BANK,
            paidDate = now,
            transactionId = 2L,
            deductFromIncome = true
        )

        val summary = balanceCalculator.computeMonthFinancialSummary(
            periodTransactions = txns,
            currentBalances = currentBalances,
            paidLoans = listOf(paidLoan)
        )

        // As requested: Income is 1000 - 300 = 700, Expense is 0, Net is 700
        assertEquals(700.0, summary.periodIncome, 0.001)
        assertEquals(0.0, summary.periodExpense, 0.001)
        assertEquals(700.0, summary.periodNet, 0.001)
        assertEquals(300.0, summary.periodLoansDeducted, 0.001)
        assertEquals(300.0, summary.totalPaidLoans, 0.001)
        assertEquals(1, summary.paidLoans.size)
        assertEquals(700.0, summary.incomeExpenseByAccount.bank, 0.001)
    }

    @Test
    fun computeMonthFinancialSummary_paidLoanCalculatedAsMonthlyExpense() {
        val now = Date()
        val currentBalances = AccountBalances(cash = 500.0, bank = 1000.0)
        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 1000.0,
                type = TransactionType.INCOME,
                categoryId = 1L,
                accountType = AccountType.BANK,
                date = now,
                description = "Salary"
            ),
            Transaction(
                id = 2L,
                amount = 300.0,
                type = TransactionType.EXPENSE,
                categoryId = 10L,
                accountType = AccountType.BANK,
                date = now,
                description = "Loan payment: Car"
            )
        )

        val paidLoan = com.example.expensetracker.data.database.dao.PaidLoanInfo(
            paymentId = 1L,
            loanConfigId = 1L,
            loanName = "Car Loan",
            amount = 300.0,
            accountType = AccountType.BANK,
            paidDate = now,
            transactionId = 2L,
            deductFromIncome = false
        )

        val summary = balanceCalculator.computeMonthFinancialSummary(
            periodTransactions = txns,
            currentBalances = currentBalances,
            paidLoans = listOf(paidLoan)
        )

        // Calculated as monthly expense: Income remains 1000, Expense is 300, Net is 700, periodLoansDeducted is 0, totalPaidLoans is 300
        assertEquals(1000.0, summary.periodIncome, 0.001)
        assertEquals(300.0, summary.periodExpense, 0.001)
        assertEquals(700.0, summary.periodNet, 0.001)
        assertEquals(0.0, summary.periodLoansDeducted, 0.001)
        assertEquals(300.0, summary.totalPaidLoans, 0.001)
        assertEquals(1, summary.paidLoans.size)
        assertEquals(700.0, summary.incomeExpenseByAccount.bank, 0.001)
    }
}
