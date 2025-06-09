package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val type: TransactionType,
    val categoryId: Long?,
    val accountType: AccountType,
    val frequency: RecurrenceFrequency,
    val nextDueDate: Date,
    val isActive: Boolean = true,
    val createdAt: Date = Date()
)

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}