package com.example.expensetracker

import com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.database.entities.MonthlyLoanPayment
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.TransactionRepositoryImpl
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.model.MonthlyLoanItem
import com.example.expensetracker.domain.repository.ILoanRepository
import com.example.expensetracker.presentation.viewModel.LoansViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class LoanAndSalaryImprovementsTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var transactionDao: TransactionDao
    private lateinit var monthlyLoanPaymentDao: MonthlyLoanPaymentDao
    private lateinit var transactionRepository: TransactionRepositoryImpl

    private lateinit var fakeLoanRepository: FakeLoanRepository
    private lateinit var userPreferences: UserPreferences
    private lateinit var balanceCalculator: BalanceCalculator

    class FakeLoanRepository : ILoanRepository {
        var paymentById: MonthlyLoanPayment? = null
        var loanById: ConfiguredLoan? = null
        var deductedPaymentId: Long? = null
        var deductedAccountType: AccountType? = null
        var updatedAmount: Double? = null

        override fun getAllConfiguredLoans(): Flow<List<ConfiguredLoan>> = flowOf(emptyList())
        override suspend fun getActiveLoans(): List<ConfiguredLoan> = emptyList()
        override suspend fun saveConfiguredLoan(loan: ConfiguredLoan): Long = 1L
        override suspend fun deleteConfiguredLoan(id: Long) {}
        override fun getPaymentsForMonth(monthKey: String): Flow<List<MonthlyLoanPayment>> = flowOf(emptyList())
        override suspend fun getUnpaidPaymentsForMonth(monthKey: String): List<MonthlyLoanPayment> = emptyList()
        override fun getUnpaidPaymentsForMonthFlow(monthKey: String): Flow<List<MonthlyLoanPayment>> = flowOf(emptyList())
        override suspend fun ensureMonthlyPaymentsCreated(monthKey: String): List<MonthlyLoanPayment> = emptyList()
        override suspend fun getMonthlyLoanItems(monthKey: String): List<MonthlyLoanItem> = emptyList()
        override suspend fun getPreservedLoanAmountForMonth(monthKey: String): Double = 0.0

        override suspend fun updateMonthlyPaymentAmount(paymentId: Long, newAmount: Double) {
            updatedAmount = newAmount
        }

        override suspend fun getPaymentById(id: Long): MonthlyLoanPayment? = paymentById
        override suspend fun getLoanById(id: Long): ConfiguredLoan? = loanById

        override suspend fun deductAndPayLoan(
            paymentId: Long,
            accountType: AccountType,
            customDate: Date,
            customDescription: String?
        ): Transaction? {
            deductedPaymentId = paymentId
            deductedAccountType = accountType
            return Transaction(
                id = 999L,
                amount = paymentById?.amount ?: 0.0,
                description = "Loan paid",
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                accountType = accountType,
                date = customDate
            )
        }

        override suspend fun dismissPayment(paymentId: Long) {}
        override fun getPaymentHistoryForLoanFlow(loanConfigId: Long): Flow<List<com.example.expensetracker.data.database.dao.LoanPaymentHistoryItem>> = flowOf(emptyList())
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        transactionDao = Mockito.mock(TransactionDao::class.java)
        monthlyLoanPaymentDao = Mockito.mock(MonthlyLoanPaymentDao::class.java)
        transactionRepository = TransactionRepositoryImpl(transactionDao, monthlyLoanPaymentDao)

        fakeLoanRepository = FakeLoanRepository()
        userPreferences = Mockito.mock(UserPreferences::class.java)
        balanceCalculator = Mockito.mock(BalanceCalculator::class.java)

        Mockito.`when`(userPreferences.lastLoanSheetDate).thenReturn(flowOf(null))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deleteTransaction_unmarksMonthlyLoanPayment() = runTest {
        val txn = Transaction(
            id = 42L,
            amount = 350.0,
            description = "Loan payment",
            type = TransactionType.EXPENSE,
            categoryId = 5L,
            accountType = AccountType.BANK,
            date = Date()
        )

        transactionRepository.deleteTransaction(txn)

        Mockito.verify(monthlyLoanPaymentDao).unmarkPaymentByTransactionId(42L)
        Mockito.verify(transactionDao).deleteTransaction(txn)
    }

    @Test
    fun deleteTransactionById_unmarksMonthlyLoanPayment() = runTest {
        transactionRepository.deleteTransactionById(99L)

        Mockito.verify(monthlyLoanPaymentDao).unmarkPaymentByTransactionId(99L)
        Mockito.verify(transactionDao).deleteTransactionById(99L)
    }

    @Test
    fun deductAndPayLoan_failsWhenInsufficientBalance() = runTest {
        val viewModel = LoansViewModel(fakeLoanRepository, userPreferences, balanceCalculator)

        fakeLoanRepository.paymentById = MonthlyLoanPayment(
            id = 10L,
            loanConfigId = 1L,
            monthKey = "2026-09",
            amount = 500.0,
            accountType = AccountType.BANK,
            isPaid = false
        )
        fakeLoanRepository.loanById = ConfiguredLoan(
            id = 1L,
            name = "Car Loan",
            defaultAmount = 500.0,
            accountType = AccountType.BANK,
            isActive = true
        )

        Mockito.`when`(balanceCalculator.getAvailableBalance(AccountType.BANK)).thenReturn(200.0)

        var callbackSuccess: Boolean? = null
        var callbackError: String? = null

        viewModel.deductAndPayLoan(10L, customAmount = 500.0) { success, errorMsg ->
            callbackSuccess = success
            callbackError = errorMsg
        }

        testScheduler.advanceUntilIdle()

        assertFalse(callbackSuccess == true)
        assertNotNull(callbackError)
        assertTrue(callbackError!!.contains("Insufficient Bank balance"))
        assertNull(fakeLoanRepository.deductedPaymentId)
    }

    @Test
    fun deductAndPayLoan_succeedsWhenBalanceIsSufficient() = runTest {
        val viewModel = LoansViewModel(fakeLoanRepository, userPreferences, balanceCalculator)

        fakeLoanRepository.paymentById = MonthlyLoanPayment(
            id = 10L,
            loanConfigId = 1L,
            monthKey = "2026-09",
            amount = 500.0,
            accountType = AccountType.BANK,
            isPaid = false
        )
        fakeLoanRepository.loanById = ConfiguredLoan(
            id = 1L,
            name = "Car Loan",
            defaultAmount = 500.0,
            accountType = AccountType.BANK,
            isActive = true
        )

        Mockito.`when`(balanceCalculator.getAvailableBalance(AccountType.BANK)).thenReturn(1000.0)

        var callbackSuccess: Boolean? = null
        var callbackError: String? = null

        viewModel.deductAndPayLoan(10L, customAmount = 500.0) { success, errorMsg ->
            callbackSuccess = success
            callbackError = errorMsg
        }

        testScheduler.advanceUntilIdle()

        assertTrue(callbackSuccess == true)
        assertNull(callbackError)
        assertEquals(10L, fakeLoanRepository.deductedPaymentId)
        assertEquals(AccountType.BANK, fakeLoanRepository.deductedAccountType)
    }
}
