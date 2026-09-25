package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    ],
    indices = [Index("categoryId"), Index("linkedExpenseId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val subDescription: String? = null,
    val date: Date,
    val type: TransactionType,
    val categoryId: Long?,
    val accountType: AccountType,
    val toAccountType: AccountType? = null,
    val startsNewPeriod: Boolean = false,
    val carriedForwardBalance: Double? = null,
    val allowNegativeBalance: Boolean = false,
    val linkedExpenseId: Long? = null,
    val debtorNote: String? = null,
    val awaitingReimbursement: Boolean = false,
    val debtType: String? = null,
    val isDebtSettled: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER, WALLET_MOVE
}

enum class AccountType {
    CASH, BANK, WALLET
}
