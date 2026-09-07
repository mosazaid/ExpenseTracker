package com.example.expensetracker.domain

import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    private val loader: TransactionExportLoader
) {
    private val dateFormat = SimpleDateFormat(CsvImportFormat.DATE_FORMAT, Locale.US)

    suspend fun exportPlainCsv(filter: TransactionExportFilter? = null): String {
        val rows = loader.loadRows(filter)
        val filterLabel = filter?.label ?: "All transactions"
        val headerComments = listOf(
            "# Expense Tracker — transaction export",
            "# Filter: $filterLabel",
            "# Generated: ${dateFormat.format(java.util.Date())}",
            "# Import: use the header row below (lines starting with # are ignored)",
            "#"
        )
        val header = CsvImportFormat.HEADER
        val dataRows = rows.map { row ->
            listOf(
                row.id.toString(),
                csvEscape(dateFormat.format(row.date)),
                csvEscape(row.type.name),
                csvEscape(row.categoryName),
                row.amount.toString(),
                csvEscape(row.account.name),
                csvEscape(row.toAccount?.name.orEmpty()),
                csvEscape(row.description),
                csvEscape(row.subDescription),
                row.startsNewPeriod.toString(),
                row.carriedForwardBalance?.toString().orEmpty(),
                row.linkedExpenseId?.toString().orEmpty(),
                csvEscape(row.debtorNote),
                row.awaitingReimbursement.toString(),
                row.allowNegativeBalance.toString(),
                csvEscape(dateFormat.format(row.createdAt)),
                csvEscape(dateFormat.format(row.updatedAt))
            ).joinToString(",")
        }
        return (headerComments + header + dataRows).joinToString("\n")
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
