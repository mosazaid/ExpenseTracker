package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val date: Date,
    val type: TransactionType,
    val categoryId: Long?,
    val accountType: AccountType,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class TransactionType {
    INCOME, EXPENSE
}

enum class AccountType {
    CASH, BANK
}