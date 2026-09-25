package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.AppAlert
import kotlinx.coroutines.flow.Flow

interface IAlertRepository {
    fun getActiveAlertsFlow(): Flow<List<AppAlert>>
    suspend fun getActiveAlerts(): List<AppAlert>
    fun getActiveAlertsCountFlow(): Flow<Int>
    suspend fun dismissAlert(id: Long)
    suspend fun dismissAlertsByTypeAndRelatedId(type: String, relatedId: Long)
    suspend fun postBudgetAlert(
        categoryId: Long,
        categoryName: String,
        spent: Double,
        limit: Double,
        isExceeded: Boolean,
        periodKey: String
    ): Long
    suspend fun postLoanAlert(
        paymentId: Long,
        loanName: String,
        amount: Double,
        periodKey: String
    ): Long
    suspend fun postDebtAlert(
        transactionId: Long,
        personName: String,
        amount: Double,
        isOwedToMe: Boolean,
        periodKey: String
    ): Long
    suspend fun postDailyExpenseAlert(periodKey: String): Long
    suspend fun dismissDailyAlertIfExpenseRecorded(periodKey: String)
    suspend fun postDebtRolloverAlert(
        monthKey: String,
        openDebtCount: Int
    ): Long
}
