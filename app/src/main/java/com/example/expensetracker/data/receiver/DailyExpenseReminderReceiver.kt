package com.example.expensetracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.util.AlarmScheduler
import com.example.expensetracker.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class DailyExpenseReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionDao: TransactionDao

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var userPreferences: com.example.expensetracker.data.preferences.UserPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == ACTION_DAILY_ALARM) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isEnabled = userPreferences.dailyReminderEnabled.first()
                    if (isEnabled && action == ACTION_DAILY_ALARM) {
                        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        // Safety guard: only trigger if currently evening/night (18:00 - 23:59), never in morning
                        if (currentHour >= 18) {
                            val today = Date()
                            val startOfDay = DateUtils.getStartOfDay(today)
                            val endOfDay = DateUtils.getEndOfDay(today)
                            val count = transactionDao.getExpenseCountBetweenDates(startOfDay, endOfDay)
                            if (count == 0) {
                                notificationHelper.showDailyExpenseReminderNotification()
                            }
                        }
                    }
                } finally {
                    // Reschedule for next day 9:00 PM (21:00)
                    alarmScheduler.scheduleDailyExpenseAlarm(21, 0)
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_DAILY_ALARM = "com.example.expensetracker.ACTION_DAILY_ALARM"
    }
}
