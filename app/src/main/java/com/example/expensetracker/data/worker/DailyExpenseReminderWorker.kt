package com.example.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.repository.IAlertRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@HiltWorker
class DailyExpenseReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: ITransactionRepository,
    private val alertRepository: IAlertRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.time

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.time

            val expenseCount = transactionRepository.getExpenseCountBetweenDates(startOfDay, endOfDay)
            val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(startOfDay)

            if (expenseCount == 0) {
                notificationHelper.showDailyExpenseReminderNotification()
                alertRepository.postDailyExpenseAlert(dayKey)
            } else {
                alertRepository.dismissDailyAlertIfExpenseRecorded(dayKey)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
