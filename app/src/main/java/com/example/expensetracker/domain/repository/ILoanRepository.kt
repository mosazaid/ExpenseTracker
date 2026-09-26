package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.database.entities.MonthlyLoanPayment
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.domain.model.MonthlyLoanItem
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface ILoanRepository {
    fun getAllConfiguredLoans(): Flow<List<ConfiguredLoan>>
    suspend fun getActiveLoans(): List<ConfiguredLoan>
    suspend fun saveConfiguredLoan(loan: ConfiguredLoan): Long
    suspend fun deleteConfiguredLoan(id: Long)
    fun getPaymentsForMonth(monthKey: String): Flow<List<MonthlyLoanPayment>>
    suspend fun getUnpaidPaymentsForMonth(monthKey: String): List<MonthlyLoanPayment>
    fun getUnpaidPaymentsForMonthFlow(monthKey: String): Flow<List<MonthlyLoanPayment>>
    suspend fun ensureMonthlyPaymentsCreated(monthKey: String): List<MonthlyLoanPayment>
    suspend fun getMonthlyLoanItems(monthKey: String): List<MonthlyLoanItem>
    suspend fun getPreservedLoanAmountForMonth(monthKey: String): Double
    suspend fun updateMonthlyPaymentAmount(paymentId: Long, newAmount: Double)
    suspend fun getPaymentById(id: Long): MonthlyLoanPayment?
    suspend fun getLoanById(id: Long): ConfiguredLoan?
    suspend fun deductAndPayLoan(
        paymentId: Long,
        accountType: AccountType,
        customDate: Date = Date(),
        customDescription: String? = null
    ): Transaction?
    suspend fun dismissPayment(paymentId: Long)
    fun getPaymentHistoryForLoanFlow(loanConfigId: Long): Flow<List<com.example.expensetracker.data.database.dao.LoanPaymentHistoryItem>>
}
