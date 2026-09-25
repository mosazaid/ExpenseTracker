package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "app_alerts")
data class AppAlert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "BUDGET", "LOAN", "DEBT", "DAILY"
    val title: String,
    val message: String,
    val relatedId: Long? = null,
    val periodKey: String = "",
    val isDismissed: Boolean = false,
    val createdAt: Date = Date()
) {
    companion object {
        const val TYPE_BUDGET = "BUDGET"
        const val TYPE_LOAN = "LOAN"
        const val TYPE_DEBT = "DEBT"
        const val TYPE_DAILY = "DAILY"
    }
}
