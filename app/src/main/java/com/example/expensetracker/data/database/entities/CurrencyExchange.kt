package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "currency_exchanges")
data class CurrencyExchange(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val soldCurrency: String,       // e.g. "JOD"
    val soldAmount: Double,         // e.g. 500.0
    val boughtCurrency: String,     // e.g. "USD"
    val boughtAmount: Double,       // e.g. 705.0
    val exchangeRate: Double,       // Rate: boughtAmount / soldAmount
    val fee: Double = 0.0,
    val feeCurrency: String = "JOD",
    val location: String? = null,   // Exchange office, bank, or ATM
    val date: Date = Date(),
    val notes: String? = null,
    val createdAt: Date = Date()
)

