package com.mousa.mizani.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    ],
    indices = [Index("categoryId")]
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
    val createdAt: Date = Date(),
    /** true: record the transaction automatically when due. false: only send a reminder. */
    @ColumnInfo(defaultValue = "0")
    val autoAdd: Boolean = false,
    /** Day of month a monthly/yearly item aims for; null means use the day of [nextDueDate]. */
    val anchorDay: Int? = null
)

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}