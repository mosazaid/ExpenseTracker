package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "item_price_history",
    indices = [
        Index("normalizedItemName"),
        Index("date")
    ]
)
data class ItemPriceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val normalizedItemName: String, // lowercase, trimmed for fast match
    val unitPrice: Double,
    val currency: String = "JOD",
    val subCategoryId: Long? = null,
    val transactionId: Long? = null,
    val storeOrVendor: String? = null,
    val date: Date = Date(),
    val notes: String? = null,
    val createdAt: Date = Date()
)

