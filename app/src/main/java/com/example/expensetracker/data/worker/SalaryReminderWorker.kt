package com.example.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.repository.RecurringRepository
import com.example.expensetracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class SalaryReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recurringRepository: RecurringRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dueReminders = recurringRepository.getDueReminders(Date())
            dueReminders.forEach { reminder ->
                notificationHelper.showSalaryReminderNotification(reminder)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
