package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.AppAlertDao
import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.ConfiguredLoanDao
import com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.AppAlert
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.database.entities.MonthlyLoanPayment
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.CategorySystemKey
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class MonthlyLoanItem(
    val payment: MonthlyLoanPayment,
    val loanConfig: ConfiguredLoan
)

@Singleton
class LoanRepository @Inject constructor(
    private val configuredLoanDao: ConfiguredLoanDao,
    private val monthlyLoanPaymentDao: MonthlyLoanPaymentDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val appAlertDao: AppAlertDao
) {

    fun getAllConfiguredLoans(): Flow<List<ConfiguredLoan>> =
        configuredLoanDao.getAllLoans()

    suspend fun getActiveLoans(): List<ConfiguredLoan> =
        configuredLoanDao.getActiveLoans()

    suspend fun saveConfiguredLoan(loan: ConfiguredLoan): Long {
        return if (loan.id == 0L) {
            configuredLoanDao.insertLoan(loan)
        } else {
            configuredLoanDao.updateLoan(loan)
            loan.id
        }
    }

    suspend fun deleteConfiguredLoan(id: Long) {
        configuredLoanDao.deleteLoanById(id)
    }

    fun getPaymentsForMonth(monthKey: String): Flow<List<MonthlyLoanPayment>> =
        monthlyLoanPaymentDao.getPaymentsForMonth(monthKey)

    suspend fun getUnpaidPaymentsForMonth(monthKey: String): List<MonthlyLoanPayment> =
        monthlyLoanPaymentDao.getUnpaidPaymentsForMonth(monthKey)

    fun getUnpaidPaymentsForMonthFlow(monthKey: String): Flow<List<MonthlyLoanPayment>> =
        monthlyLoanPaymentDao.getUnpaidPaymentsForMonthFlow(monthKey)

    suspend fun ensureMonthlyPaymentsCreated(monthKey: String): List<MonthlyLoanPayment> {
        val activeLoans = configuredLoanDao.getActiveLoans()
        activeLoans.forEach { loan ->
            val existing = monthlyLoanPaymentDao.getPaymentForLoanAndMonth(loan.id, monthKey)
            if (existing == null) {
                monthlyLoanPaymentDao.insertPayment(
                    MonthlyLoanPayment(
                        loanConfigId = loan.id,
                        monthKey = monthKey,
                        amount = loan.defaultAmount,
                        accountType = loan.accountType,
                        isPaid = false
                    )
                )
            }
        }
        return monthlyLoanPaymentDao.getPaymentsForMonthSnapshot(monthKey)
    }

    suspend fun getMonthlyLoanItems(monthKey: String): List<MonthlyLoanItem> {
        ensureMonthlyPaymentsCreated(monthKey)
        val payments = monthlyLoanPaymentDao.getPaymentsForMonthSnapshot(monthKey)
        val loans = configuredLoanDao.getAllLoans()
        return payments.mapNotNull { payment ->
            val config = configuredLoanDao.getLoanById(payment.loanConfigId)
            config?.let { MonthlyLoanItem(payment = payment, loanConfig = it) }
        }
    }

    suspend fun getPreservedLoanAmountForMonth(monthKey: String): Double {
        ensureMonthlyPaymentsCreated(monthKey)
        val unpaid = monthlyLoanPaymentDao.getUnpaidPaymentsForMonth(monthKey)
        return unpaid.sumOf { it.amount }
    }

    suspend fun updateMonthlyPaymentAmount(paymentId: Long, newAmount: Double) {
        val payment = monthlyLoanPaymentDao.getPaymentById(paymentId) ?: return
        monthlyLoanPaymentDao.updatePayment(payment.copy(amount = newAmount))
    }

    suspend fun deductAndPayLoan(
        paymentId: Long,
        accountType: AccountType,
        customDate: Date = Date(),
        customDescription: String? = null
    ): Transaction? {
        val payment = monthlyLoanPaymentDao.getPaymentById(paymentId) ?: return null
        val loanConfig = configuredLoanDao.getLoanById(payment.loanConfigId) ?: return null

        // Find or create 'Loan' category
        var loanCategory = categoryDao.getCategoryByName("Loan", TransactionType.EXPENSE)
        if (loanCategory == null) {
            val newCatId = categoryDao.insertCategory(
                Category(
                    name = "Loan",
                    icon = "🏦",
                    color = "#3F51B5",
                    type = TransactionType.EXPENSE,
                    isDefault = true
                )
            )
            loanCategory = categoryDao.getCategoryById(newCatId)
        }

        val description = customDescription?.ifBlank { null }
            ?: "Loan payment: ${loanConfig.name}"

        val transaction = Transaction(
            amount = payment.amount,
            description = description,
            subDescription = loanConfig.name,
            date = customDate,
            type = TransactionType.EXPENSE,
            categoryId = loanCategory?.id,
            accountType = accountType,
            startsNewPeriod = false
        )

        val txnId = transactionDao.insertTransaction(transaction)

        // Mark payment as paid
        monthlyLoanPaymentDao.markAsPaid(paymentId, customDate, txnId)

        // Soft delete / dismiss alert for this loan
        appAlertDao.dismissAlertsByTypeAndRelatedId(AppAlert.TYPE_LOAN, paymentId)

        return transaction.copy(id = txnId)
    }

    suspend fun dismissPayment(paymentId: Long) {
        monthlyLoanPaymentDao.dismissPayment(paymentId)
        appAlertDao.dismissAlertsByTypeAndRelatedId(AppAlert.TYPE_LOAN, paymentId)
    }
}
