package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun exportTransactions(): String {
        val transactions = transactionRepository.getAllTransactionsSnapshot()
        val categories = categoryRepository.getAllCategoriesSnapshot()
        val categoryMap = categories.associateBy { it.id }

        val header = listOf(
            "id", "date", "type", "category", "amount", "account", "toAccount",
            "description", "startsNewPeriod", "linkedExpenseId", "debtorNote",
            "awaitingReimbursement", "allowNegativeBalance", "createdAt", "updatedAt"
        ).joinToString(",")

        val rows = transactions.map { txn ->
            listOf(
                txn.id.toString(),
                csvEscape(dateFormat.format(txn.date)),
                csvEscape(txn.type.name),
                csvEscape(categoryMap[txn.categoryId]?.name.orEmpty()),
                txn.amount.toString(),
                csvEscape(txn.accountType.name),
                csvEscape(txn.toAccountType?.name.orEmpty()),
                csvEscape(txn.description),
                txn.startsNewPeriod.toString(),
                txn.linkedExpenseId?.toString().orEmpty(),
                csvEscape(txn.debtorNote.orEmpty()),
                txn.awaitingReimbursement.toString(),
                txn.allowNegativeBalance.toString(),
                csvEscape(dateFormat.format(txn.createdAt)),
                csvEscape(dateFormat.format(txn.updatedAt))
            ).joinToString(",")
        }

        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
