package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "configured_loans")
data class ConfiguredLoan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val defaultAmount: Double,
    val accountType: AccountType = AccountType.BANK,
    val isActive: Boolean = true,
    val createdAt: Date = Date()
)
