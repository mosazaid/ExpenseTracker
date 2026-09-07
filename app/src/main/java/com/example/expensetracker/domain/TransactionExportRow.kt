package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.TransactionType
import java.util.Date

data class TransactionExportRow(
    val id: Long,
    val date: Date,
    val type: TransactionType,
    val categoryName: String,
    val amount: Double,
    val account: AccountType,
    val toAccount: AccountType?,
    val description: String,
    val subDescription: String,
    val startsNewPeriod: Boolean,
    val carriedForwardBalance: Double?,
    val linkedExpenseId: Long?,
    val debtorNote: String,
    val awaitingReimbursement: Boolean,
    val allowNegativeBalance: Boolean,
    val createdAt: Date,
    val updatedAt: Date
)

object CsvImportFormat {
    const val HEADER =
        "id,date,type,category,amount,account,toAccount,description,subDescription," +
            "startsNewPeriod,carriedForwardBalance,linkedExpenseId,debtorNote," +
            "awaitingReimbursement,allowNegativeBalance,createdAt,updatedAt"

    const val EXAMPLE_ROW =
        "0,2026-01-15 10:30:00,EXPENSE,Groceries,45.50,CASH,,Supermarket X,coffee chips," +
            "false,,, ,false,false,2026-01-15 10:30:00,2026-01-15 10:30:00"

    val REQUIRED_COLUMNS = listOf("date", "type", "amount", "account", "description")

    val OPTIONAL_COLUMNS = listOf(
        "id", "category", "toAccount", "subDescription", "startsNewPeriod",
        "carriedForwardBalance", "linkedExpenseId", "debtorNote",
        "awaitingReimbursement", "allowNegativeBalance", "createdAt", "updatedAt"
    )

    val TYPE_VALUES = "INCOME, EXPENSE, TRANSFER, WALLET_MOVE"
    val ACCOUNT_VALUES = "CASH, BANK, WALLET"
    const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
}
