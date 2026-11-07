package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,               // e.g. "Dubai Vacation 2026"
    val destination: String,         // e.g. "United Arab Emirates"
    val baseCurrency: String,        // e.g. "AED"
    val homeCurrency: String = "JOD",// e.g. "JOD"
    val totalBudget: Double,         // Budget amount in baseCurrency
    val startDate: Date,
    val endDate: Date,
    val isActive: Boolean = false,
    val notes: String? = null,
    val createdAt: Date = Date()
)

