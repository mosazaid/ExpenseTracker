package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.util.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

/** Posts a phone notification for each due recurring item that the user has to record. */
class RecurringReminderUseCase @Inject constructor(
    private val recurringRepository: IRecurringRepository,
    private val userPreferences: UserPreferences,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke(now: Date = Date()) {
        if (!userPreferences.salaryAlertsEnabled.first()) return
        // Auto-add items are recorded and notified by AutoDeductRecurringWorker.
        recurringRepository.getDueReminders(now).itemsToRemind().forEach { reminder ->
            notificationHelper.showSalaryReminderNotification(reminder)
        }
    }
}
