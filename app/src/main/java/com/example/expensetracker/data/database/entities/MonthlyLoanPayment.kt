package com.example.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "monthly_loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = ConfiguredLoan::class,
            parentColumns = ["id"],
            childColumns = ["loanConfigId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("loanConfigId"),
        Index(value = ["loanConfigId", "monthKey"], unique = true)
    ]
)
data class MonthlyLoanPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanConfigId: Long,
    val monthKey: String,
    val amount: Double,
    val accountType: AccountType = AccountType.BANK,
    val isPaid: Boolean = false,
    val paidDate: Date? = null,
    val transactionId: Long? = null,
    val isDismissed: Boolean = false,
    val createdAt: Date = Date()
)
