package com.example.expensetracker.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.usecase.ExecuteAutoRecurringUseCase
import com.example.expensetracker.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Date

@HiltWorker
class AutoDeductRecurringWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val executeAutoRecurringUseCase: ExecuteAutoRecurringUseCase,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = executeAutoRecurringUseCase(Date())
            if (result.executedCount > 0) {
                result.executedTransactions.forEach { tx ->
                    notificationHelper.showRecurringAutoDeductNotification(
                        description = tx.description,
                        amount = tx.amount,
                        txId = tx.id
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

