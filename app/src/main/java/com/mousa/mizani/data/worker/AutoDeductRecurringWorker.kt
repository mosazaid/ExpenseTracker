package com.mousa.mizani.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mousa.mizani.domain.usecase.ExecuteAutoRecurringUseCase
import com.mousa.mizani.glance.WidgetUpdateHelper
import com.mousa.mizani.util.NotificationHelper
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
            // One notification per item, even when several missed periods were caught up.
            result.recorded.associateBy { it.id }.values.forEach { item ->
                notificationHelper.showRecurringAutoDeductNotification(
                    description = item.description,
                    amount = item.amount,
                    txId = item.id
                )
            }
            result.blocked.forEach { notificationHelper.showRecurringBlockedNotification(it) }
            if (result.recorded.isNotEmpty()) {
                WidgetUpdateHelper.updateAllWidgets(context)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

