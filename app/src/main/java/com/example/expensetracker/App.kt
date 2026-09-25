package com.example.expensetracker

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.core.locale.LocaleHelper
import com.example.expensetracker.worker.SalaryReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var alarmScheduler: com.example.expensetracker.util.AlarmScheduler

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
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 21)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        val initialDelay = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<com.example.expensetracker.worker.DailyExpenseReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_expense_reminder_9pm",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleLoanReminderWorker() {
        val request = PeriodicWorkRequestBuilder<com.example.expensetracker.worker.LoanReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "loan_reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
