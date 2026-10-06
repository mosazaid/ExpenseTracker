package com.example.expensetracker

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.core.locale.LocaleHelper
import com.example.expensetracker.data.worker.DailyExpenseReminderWorker
import com.example.expensetracker.data.worker.LoanReminderWorker
import com.example.expensetracker.data.worker.SalaryReminderWorker
import com.example.expensetracker.util.AlarmScheduler
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.example.expensetracker.core.location.LocationHelper
import com.example.expensetracker.domain.PrayerCalculationMethod
import com.example.expensetracker.domain.PrayerTimesCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var userPreferences: com.example.expensetracker.data.preferences.UserPreferences

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        alarmScheduler.scheduleDailyExpenseAlarm(21, 0)
        scheduleSalaryReminderWorker()
        scheduleDailyExpenseReminderWorker()
        scheduleLoanReminderWorker()
        syncPrayerAlarms()
    }

    private fun syncPrayerAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (userPreferences.prayerAlertsEnabled.first()) {
                    val locationHelper = LocationHelper(this@App)
                    val detected = locationHelper.detectLocationInfo()
                    val city = detected?.nearestPresetCity ?: PrayerTimesCalculator.PRESET_CITIES.first()
                    val methodKey = userPreferences.prayerCalculationMethod.first()
                    val method = if (methodKey != null) {
                        try {
                            PrayerCalculationMethod.valueOf(methodKey)
                        } catch (_: Exception) {
                            PrayerCalculationMethod.autoDetect(detected?.countryCode)
                        }
                    } else {
                        PrayerCalculationMethod.autoDetect(detected?.countryCode)
                    }
                    val schedule = PrayerTimesCalculator.calculateDaySchedule(city, Date(), method, true)

                    if (userPreferences.fajrAlertEnabled.first()) alarmScheduler.schedulePrayerAlarm("Fajr", schedule.fajr24)
                    if (userPreferences.dhuhrAlertEnabled.first()) alarmScheduler.schedulePrayerAlarm("Dhuhr", schedule.dhuhr24)
                    if (userPreferences.asrAlertEnabled.first()) alarmScheduler.schedulePrayerAlarm("Asr", schedule.asr24)
                    if (userPreferences.maghribAlertEnabled.first()) alarmScheduler.schedulePrayerAlarm("Maghrib", schedule.maghrib24)
                    if (userPreferences.ishaAlertEnabled.first()) alarmScheduler.schedulePrayerAlarm("Isha", schedule.isha24)
                }
            } catch (_: Exception) {}
        }
    }

    private fun scheduleSalaryReminderWorker() {
        val request = PeriodicWorkRequestBuilder<SalaryReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "salary_reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleDailyExpenseReminderWorker() {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<DailyExpenseReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_expense_reminder_9pm",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleLoanReminderWorker() {
        val request = PeriodicWorkRequestBuilder<LoanReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "loan_reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
