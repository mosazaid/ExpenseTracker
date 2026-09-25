package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.presentation.theme.DateUtils
import com.example.expensetracker.util.AlarmScheduler
import com.example.expensetracker.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == ACTION_DAILY_ALARM) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (action == ACTION_DAILY_ALARM) {
                        val today = Date()
                        val startOfDay = DateUtils.getStartOfDay(today)
                        val endOfDay = DateUtils.getEndOfDay(today)
                        val count = transactionDao.getExpenseCountBetweenDates(startOfDay, endOfDay)
                        if (count == 0) {
                            notificationHelper.showDailyExpenseReminderNotification()
                        }
                    }
                } finally {
                    // Reschedule for next day 9:00 PM
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
