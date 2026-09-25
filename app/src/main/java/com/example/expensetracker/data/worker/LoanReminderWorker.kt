package com.example.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.repository.IAlertRepository
import com.example.expensetracker.domain.repository.ILoanRepository
import com.example.expensetracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class LoanReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val loanRepository: ILoanRepository,
    private val alertRepository: IAlertRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val loanItems = loanRepository.getMonthlyLoanItems(monthKey)
            val unpaidItems = loanItems.filter { !it.payment.isPaid }

            if (unpaidItems.isEmpty()) {
                // All loans deducted/paid for this month: Battery saving, no reminders sent!
                return Result.success()
            }

            unpaidItems.forEach { item ->
                notificationHelper.showLoanReminderNotification(
                    loanName = item.loanConfig.name,
                    amount = item.payment.amount,
                    loanId = item.payment.id
                )
                alertRepository.postLoanAlert(
                    paymentId = item.payment.id,
                    loanName = item.loanConfig.name,
                    amount = item.payment.amount,
                    periodKey = monthKey
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
