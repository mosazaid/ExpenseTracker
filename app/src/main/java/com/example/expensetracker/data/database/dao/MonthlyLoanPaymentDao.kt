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
}
