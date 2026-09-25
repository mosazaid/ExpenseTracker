package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.AppAlert
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AppAlertDao {

    @Query("SELECT * FROM app_alerts WHERE isDismissed = 0 ORDER BY createdAt DESC")
    fun getActiveAlertsFlow(): Flow<List<AppAlert>>

    @Query("SELECT * FROM app_alerts WHERE isDismissed = 0 ORDER BY createdAt DESC")
    suspend fun getActiveAlerts(): List<AppAlert>

    @Query("SELECT COUNT(*) FROM app_alerts WHERE isDismissed = 0")
    fun getActiveAlertsCountFlow(): Flow<Int>

    @Query("SELECT * FROM app_alerts WHERE type = :type AND isDismissed = 0")
    suspend fun getActiveAlertsByType(type: String): List<AppAlert>

    @Query("SELECT * FROM app_alerts WHERE type = :type AND relatedId = :relatedId AND periodKey = :periodKey AND isDismissed = 0 LIMIT 1")
    suspend fun findActiveAlert(type: String, relatedId: Long, periodKey: String): AppAlert?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AppAlert): Long

    @Query("UPDATE app_alerts SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissAlert(id: Long)

    @Query("UPDATE app_alerts SET isDismissed = 1 WHERE type = :type AND relatedId = :relatedId")
    suspend fun dismissAlertsByTypeAndRelatedId(type: String, relatedId: Long)

    @Query("DELETE FROM app_alerts WHERE isDismissed = 1 AND createdAt < :beforeDate")
    suspend fun deleteOldDismissedAlerts(beforeDate: Date)

    @Delete
    suspend fun deleteAlert(alert: AppAlert)
}
