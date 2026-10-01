package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_splits",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("transactionId"),
        Index("subCategoryId")
    ]
)
data class TransactionSplit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long = 0,
    val subCategoryId: Long? = null,
    val subCategoryName: String? = null,
    val amount: Double,
    val note: String? = null,
    val isDebt: Boolean = false,
    val debtPersonName: String? = null,
    val isDebtSettled: Boolean = false
)
