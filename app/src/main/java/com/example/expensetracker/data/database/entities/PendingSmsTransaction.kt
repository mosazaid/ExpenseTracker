package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String = "",
    val rawBody: String,
    val amount: Double,
    val currency: String = "JOD",
    val merchant: String = "",
    val cardOrAccount: String? = null,
    val suggestedCategory: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isProcessed: Boolean = false,
    val isDismissed: Boolean = false
)

