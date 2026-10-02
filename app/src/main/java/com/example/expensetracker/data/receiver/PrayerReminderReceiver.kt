package com.example.expensetracker.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.PrayerCalculationMethod
import com.example.expensetracker.domain.PrayerTimesCalculator
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
class PrayerReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        if (action == ACTION_PRAYER_ALARM) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alertsEnabled = userPreferences.prayerAlertsEnabled.first()
                    if (alertsEnabled) {
                        val isPrayerEnabled = when (prayerName.lowercase(java.util.Locale.US)) {
                            "fajr" -> userPreferences.fajrAlertEnabled.first()
                            "dhuhr" -> userPreferences.dhuhrAlertEnabled.first()
                            "asr" -> userPreferences.asrAlertEnabled.first()
                            "maghrib" -> userPreferences.maghribAlertEnabled.first()
                            "isha" -> userPreferences.ishaAlertEnabled.first()
                            else -> false
                        }
                        if (isPrayerEnabled) {
                            notificationHelper.showPrayerReminderNotification(prayerName, prayerTime)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.expensetracker.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }
}
