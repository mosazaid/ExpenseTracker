package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.AppAlertDao
import com.example.expensetracker.data.database.entities.AppAlert
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val appAlertDao: AppAlertDao
) {

    fun getActiveAlertsFlow(): Flow<List<AppAlert>> =
        appAlertDao.getActiveAlertsFlow()

    suspend fun getActiveAlerts(): List<AppAlert> =
        appAlertDao.getActiveAlerts()

    fun getActiveAlertsCountFlow(): Flow<Int> =
        appAlertDao.getActiveAlertsCountFlow()

    suspend fun dismissAlert(id: Long) {
        appAlertDao.dismissAlert(id)
    }

    suspend fun dismissAlertsByTypeAndRelatedId(type: String, relatedId: Long) {
        appAlertDao.dismissAlertsByTypeAndRelatedId(type, relatedId)
    }

    suspend fun postBudgetAlert(
        categoryId: Long,
        categoryName: String,
        spent: Double,
        limit: Double,
        isExceeded: Boolean,
        periodKey: String
    ): Long {
        val existing = appAlertDao.findActiveAlert(AppAlert.TYPE_BUDGET, categoryId, periodKey)
        if (existing != null) {
            return existing.id
        }
        val title = if (isExceeded) "Budget Exceeded" else "Budget Warning"
        val message = if (isExceeded) {
            "Budget for $categoryName exceeded! (${formatAmount(spent)} / ${formatAmount(limit)})"
        } else {
            "Approaching budget limit for $categoryName (${formatAmount(spent)} / ${formatAmount(limit)})"
        }
        return appAlertDao.insertAlert(
            AppAlert(
                type = AppAlert.TYPE_BUDGET,
                title = title,
                message = message,
                relatedId = categoryId,
                periodKey = periodKey
            )
        )
    }

    suspend fun postLoanAlert(
        paymentId: Long,
        loanName: String,
        amount: Double,
        periodKey: String
    ): Long {
        val existing = appAlertDao.findActiveAlert(AppAlert.TYPE_LOAN, paymentId, periodKey)
        if (existing != null) {
            return existing.id
        }
        return appAlertDao.insertAlert(
            AppAlert(
                type = AppAlert.TYPE_LOAN,
                title = "Loan Payment Due",
                message = "You have a loan payment: $loanName (${formatAmount(amount)}). Tap to review and pay.",
                relatedId = paymentId,
                periodKey = periodKey
            )
        )
    }

    suspend fun postDebtAlert(
        transactionId: Long,
        personName: String,
        amount: Double,
        isOwedToMe: Boolean,
        periodKey: String
    ): Long {
        val existing = appAlertDao.findActiveAlert(AppAlert.TYPE_DEBT, transactionId, periodKey)
        if (existing != null) {
            return existing.id
        }
        val title = if (isOwedToMe) "Debt Owed To You" else "Debt You Owe"
        val message = if (isOwedToMe) {
            "$personName owes you ${formatAmount(amount)}"
        } else {
            "You owe $personName ${formatAmount(amount)}"
        }
        return appAlertDao.insertAlert(
            AppAlert(
                type = AppAlert.TYPE_DEBT,
                title = title,
                message = message,
                relatedId = transactionId,
                periodKey = periodKey
            )
        )
    }

    suspend fun postDailyExpenseAlert(periodKey: String): Long {
        val existing = appAlertDao.findActiveAlert(AppAlert.TYPE_DAILY, 0L, periodKey)
        if (existing != null) {
            return existing.id
        }
        return appAlertDao.insertAlert(
            AppAlert(
                type = AppAlert.TYPE_DAILY,
                title = "No Expenses Logged Today",
                message = "You haven't recorded any expenses today. Keep your budget updated!",
                relatedId = 0L,
                periodKey = periodKey
            )
        )
    }

    suspend fun dismissDailyAlertIfExpenseRecorded(periodKey: String) {
        val alert = appAlertDao.findActiveAlert(AppAlert.TYPE_DAILY, 0L, periodKey)
        if (alert != null) {
            appAlertDao.dismissAlert(alert.id)
        }
    }

    suspend fun postDebtRolloverAlert(
        monthKey: String,
        openDebtCount: Int
    ): Long {
        val existing = appAlertDao.findActiveAlert(AppAlert.TYPE_DEBT, -1L, monthKey)
        if (existing != null) {
            return existing.id
        }
        return appAlertDao.insertAlert(
            AppAlert(
                type = AppAlert.TYPE_DEBT,
                title = "Rollover Open Debts",
                message = "New month started! You have $openDebtCount open debt(s) to review and carry forward.",
                relatedId = -1L,
                periodKey = monthKey
            )
        )
    }

    private fun formatAmount(amount: Double): String {
        return String.format(java.util.Locale.US, "%.3f JOD", amount)
    }
}
