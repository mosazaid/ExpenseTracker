package com.example.expensetracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.expensetracker.data.receiver.DailyExpenseReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDailyExpenseAlarm(hour: Int = 21, minute: Int = 0) {
        val intent = Intent(context, DailyExpenseReminderReceiver::class.java).apply {
            action = DailyExpenseReminderReceiver.ACTION_DAILY_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_ALARM_REQUEST_CODE,
            intent,
            flags
        )

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
        } else {
            // Inexact fallback: if exact alarm permission is not granted by user,
            // OS delivers the alarm during the next maintenance window (usually within 5 to 15 minutes)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedulePrayerAlarm(prayerName: String, hour: Int, minute: Int) {
        val reqCode = prayerRequestCode(prayerName)
        val formattedTime = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
        val intent = Intent(context, com.example.expensetracker.data.receiver.PrayerReminderReceiver::class.java).apply {
            action = com.example.expensetracker.data.receiver.PrayerReminderReceiver.ACTION_PRAYER_ALARM
            putExtra(com.example.expensetracker.data.receiver.PrayerReminderReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(com.example.expensetracker.data.receiver.PrayerReminderReceiver.EXTRA_PRAYER_TIME, formattedTime)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, reqCode, intent, flags)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelPrayerAlarm(prayerName: String) {
        val reqCode = prayerRequestCode(prayerName)
        val intent = Intent(context, com.example.expensetracker.data.receiver.PrayerReminderReceiver::class.java).apply {
            action = com.example.expensetracker.data.receiver.PrayerReminderReceiver.ACTION_PRAYER_ALARM
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, reqCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun prayerRequestCode(prayerName: String): Int = when (prayerName.lowercase(java.util.Locale.US)) {
        "fajr" -> 3101
        "dhuhr" -> 3102
        "asr" -> 3103
        "maghrib" -> 3104
        "isha" -> 3105
        else -> 3100
    }

    companion object {
        const val DAILY_ALARM_REQUEST_CODE = 2100
    }
}
