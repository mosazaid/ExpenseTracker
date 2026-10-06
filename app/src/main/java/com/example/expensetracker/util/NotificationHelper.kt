package com.example.expensetracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.core.format.CurrencyUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private val appIconBitmap: android.graphics.Bitmap? by lazy {
        try {
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            if (drawable != null) {
                if (drawable is android.graphics.drawable.BitmapDrawable) {
                    drawable.bitmap
                } else {
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        width,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun showSalaryReminderNotification(recurring: RecurringTransaction) {
        if (!hasPermission()) return
        createChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("recurringId", recurring.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            recurring.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SALARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Salary reminder")
            .setContentText("Record ${CurrencyUtils.formatCurrency(recurring.amount)} salary")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        appIconBitmap?.let { builder.setLargeIcon(it) }

        NotificationManagerCompat.from(context).notify(recurring.id.toInt(), builder.build())
    }

    fun showDailyExpenseReminderNotification() {
        if (!hasPermission()) return
        createChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "add_expense")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_DAILY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_daily_expense_title))
            .setContentText(context.getString(R.string.notif_daily_expense_desc))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        appIconBitmap?.let { builder.setLargeIcon(it) }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_DAILY_ID, builder.build())
    }

    fun showBudgetNotification(categoryName: String, spent: Double, limit: Double, isExceeded: Boolean) {
        if (!hasPermission()) return
        createChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "categories")
        }
        val notifId = (categoryName.hashCode() and 0x7FFFFFFF) % 10000 + 20000
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isExceeded) {
            context.getString(R.string.notif_budget_exceeded_title, categoryName)
        } else {
            context.getString(R.string.notif_budget_warning_title, categoryName)
        }

        val text = if (isExceeded) {
            context.getString(
                R.string.notif_budget_exceeded_desc,
                categoryName,
                CurrencyUtils.formatCurrency(spent),
                CurrencyUtils.formatCurrency(limit)
            )
        } else {
            context.getString(
                R.string.notif_budget_warning_desc,
                categoryName,
                CurrencyUtils.formatCurrency(spent),
                CurrencyUtils.formatCurrency(limit)
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (isExceeded) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        appIconBitmap?.let { builder.setLargeIcon(it) }

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    fun showLoanReminderNotification(loanName: String, amount: Double, loanId: Long, remainingAmount: Double? = null) {
        if (!hasPermission()) return
        createChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "loans")
        }
        val notifId = (loanId.toInt() and 0x7FFFFFFF) % 10000 + 30000
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notif_loan_title, loanName)
        val text = if (remainingAmount != null && remainingAmount < amount && remainingAmount > 0.0) {
            "$loanName: ${CurrencyUtils.formatCurrency(remainingAmount)} remaining of ${CurrencyUtils.formatCurrency(amount)}"
        } else {
            context.getString(
                R.string.notif_loan_desc,
                loanName,
                CurrencyUtils.formatCurrency(amount)
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_LOANS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        appIconBitmap?.let { builder.setLargeIcon(it) }

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    fun cancelLoanNotification(loanId: Long) {
        val notifId = (loanId.toInt() and 0x7FFFFFFF) % 10000 + 30000
        NotificationManagerCompat.from(context).cancel(notifId)
    }

    fun showTestNotification() {
        if (!hasPermission()) return
        createChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SALARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test Notification")
            .setContentText("Notifications are working properly!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        appIconBitmap?.let { builder.setLargeIcon(it) }

        NotificationManagerCompat.from(context).notify(9999, builder.build())
    }

    fun showPrayerReminderNotification(prayerName: String, prayerTime: String) {
        if (!hasPermission()) return
        createChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("nav_destination", "prayer_qiblah")
        }
        val notifId = when (prayerName.lowercase(java.util.Locale.US)) {
            "fajr" -> 11001
            "dhuhr" -> 11002
            "asr" -> 11003
            "maghrib" -> 11004
            "isha" -> 11005
            else -> 11000
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for $prayerName prayer")
            .setContentText("It is now $prayerTime, time to perform $prayerName prayer.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("It is now $prayerTime, time to perform $prayerName prayer."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        appIconBitmap?.let { builder.setLargeIcon(it) }

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val salaryChannel = NotificationChannel(
                CHANNEL_SALARY,
                "Salary reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "Daily expense reminder (9 PM)",
                NotificationManager.IMPORTANCE_HIGH
            )

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            val loansChannel = NotificationChannel(
                CHANNEL_LOANS,
                "Loan payment reminders",
                NotificationManager.IMPORTANCE_HIGH
            )

            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER,
                "Prayer time reminders",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannels(
                listOf(salaryChannel, dailyChannel, budgetChannel, loansChannel, prayerChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_SALARY = "salary_reminders"
        const val CHANNEL_DAILY = "daily_expense_reminders"
        const val CHANNEL_BUDGET = "budget_alerts"
        const val CHANNEL_LOANS = "loan_reminders"
        const val CHANNEL_PRAYER = "prayer_reminders"

        const val NOTIFICATION_DAILY_ID = 10001
    }
}
