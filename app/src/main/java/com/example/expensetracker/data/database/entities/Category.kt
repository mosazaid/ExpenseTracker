package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: String,
    val type: TransactionType,
    val isDefault: Boolean = false,
    val createdAt: Date = Date()
)