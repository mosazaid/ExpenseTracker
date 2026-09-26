package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.MonthlyLoanPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyLoanPaymentDao {

    @Query("SELECT * FROM monthly_loan_payments WHERE monthKey = :monthKey")
    fun getPaymentsForMonth(monthKey: String): Flow<List<MonthlyLoanPayment>>

    @Query("SELECT * FROM monthly_loan_payments WHERE monthKey = :monthKey")
    suspend fun getPaymentsForMonthSnapshot(monthKey: String): List<MonthlyLoanPayment>

    @Query("SELECT * FROM monthly_loan_payments WHERE monthKey = :monthKey AND isPaid = 0")
    suspend fun getUnpaidPaymentsForMonth(monthKey: String): List<MonthlyLoanPayment>

    @Query("SELECT * FROM monthly_loan_payments WHERE monthKey = :monthKey AND isPaid = 0")
    fun getUnpaidPaymentsForMonthFlow(monthKey: String): Flow<List<MonthlyLoanPayment>>

    @Query("SELECT * FROM monthly_loan_payments WHERE loanConfigId = :loanConfigId AND monthKey = :monthKey LIMIT 1")
    suspend fun getPaymentForLoanAndMonth(loanConfigId: Long, monthKey: String): MonthlyLoanPayment?

    @Query("SELECT * FROM monthly_loan_payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): MonthlyLoanPayment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: MonthlyLoanPayment): Long

    @Update
    suspend fun updatePayment(payment: MonthlyLoanPayment)

    @Delete
    suspend fun deletePayment(payment: MonthlyLoanPayment)

    @Query("UPDATE monthly_loan_payments SET isPaid = 1, paidDate = :paidDate, transactionId = :txnId WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidDate: java.util.Date, txnId: Long)

    @Query("UPDATE monthly_loan_payments SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissPayment(id: Long)

    @Query("SELECT * FROM monthly_loan_payments WHERE transactionId = :txnId LIMIT 1")
    suspend fun getPaymentByTransactionId(txnId: Long): MonthlyLoanPayment?

    @Query("UPDATE monthly_loan_payments SET isPaid = 0, paidDate = NULL, transactionId = NULL WHERE transactionId = :txnId")
    suspend fun unmarkPaymentByTransactionId(txnId: Long)

    @Query("""
        SELECT 
            mlp.id AS paymentId,
            mlp.loanConfigId AS loanConfigId,
            cl.name AS loanName,
            mlp.amount AS amount,
            mlp.accountType AS accountType,
            COALESCE(t.date, mlp.paidDate) AS paidDate,
            mlp.transactionId AS transactionId,
            cl.deductFromIncome AS deductFromIncome
        FROM monthly_loan_payments mlp
        INNER JOIN configured_loans cl ON mlp.loanConfigId = cl.id
        LEFT JOIN transactions t ON mlp.transactionId = t.id
        WHERE mlp.isPaid = 1 
          AND (
              (t.date IS NOT NULL AND t.date >= :startDate AND t.date <= :endDate)
              OR (t.date IS NULL AND mlp.paidDate IS NOT NULL AND mlp.paidDate >= :startDate AND mlp.paidDate <= :endDate)
          )
    """)
    suspend fun getPaidLoansBetweenDates(
        startDate: java.util.Date,
        endDate: java.util.Date
    ): List<PaidLoanInfo>

    @Query("""
        SELECT COALESCE(SUM(mlp.amount), 0.0)
        FROM monthly_loan_payments mlp
        INNER JOIN configured_loans cl ON mlp.loanConfigId = cl.id
        LEFT JOIN transactions t ON mlp.transactionId = t.id
        WHERE mlp.isPaid = 1 
          AND cl.deductFromIncome = 1
          AND (
              (t.date IS NOT NULL AND t.date >= :startDate AND t.date <= :endDate)
              OR (t.date IS NULL AND mlp.paidDate IS NOT NULL AND mlp.paidDate >= :startDate AND mlp.paidDate <= :endDate)
          )
    """)
    suspend fun getTotalPaidLoansDeductedFromIncomeBetweenDates(
        startDate: java.util.Date,
        endDate: java.util.Date
    ): Double

    @Query("""
        SELECT 
            mlp.id AS paymentId,
            mlp.loanConfigId AS loanConfigId,
            mlp.monthKey AS monthKey,
            mlp.amount AS amount,
            mlp.accountType AS accountType,
            mlp.isPaid AS isPaid,
            COALESCE(t.date, mlp.paidDate) AS paidDate,
            mlp.transactionId AS transactionId
        FROM monthly_loan_payments mlp
        LEFT JOIN transactions t ON mlp.transactionId = t.id
        WHERE mlp.loanConfigId = :loanConfigId
        ORDER BY mlp.monthKey DESC, COALESCE(t.date, mlp.paidDate) DESC
    """)
    fun getPaymentHistoryForLoanFlow(loanConfigId: Long): Flow<List<LoanPaymentHistoryItem>>
}

data class PaidLoanInfo(
    val paymentId: Long,
    val loanConfigId: Long,
    val loanName: String,
    val amount: Double,
    val accountType: com.example.expensetracker.data.database.entities.AccountType,
    val paidDate: java.util.Date?,
    val transactionId: Long?,
    val deductFromIncome: Boolean
)

data class LoanPaymentHistoryItem(
    val paymentId: Long,
    val loanConfigId: Long,
    val monthKey: String,
    val amount: Double,
    val accountType: com.example.expensetracker.data.database.entities.AccountType,
    val isPaid: Boolean,
    val paidDate: java.util.Date?,
    val transactionId: Long?
)
