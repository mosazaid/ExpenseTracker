package com.mousa.mizani.core.export

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.TransactionType
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

    /**
     * A blank import file: explanation comments, the header, and a made-up example row
     * written as a comment, so importing it unchanged adds nothing.
     */
    fun template(): String = listOf(
        "# Mizani — import template",
        "# Add one transaction per line below the header row (lines starting with # are ignored)",
        "# To try the example row, remove the leading # and the space after it",
        "#",
        HEADER,
        "# $EXAMPLE_ROW"
    ).joinToString("\n")
}
