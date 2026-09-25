package com.example.expensetracker.core.export

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CsvImportResult(
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
    val errors: List<String>
)

@Singleton
class CsvImporter @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun importTransactions(csvContent: String): CsvImportResult {
        val lines = csvContent.lines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
        if (lines.isEmpty()) {
            return CsvImportResult(0, 0, 0, listOf("File is empty"))
        }

        val header = parseCsvLine(lines.first()).map { it.lowercase(Locale.US) }
        if (!header.contains("date") || !header.contains("type") || !header.contains("amount")) {
            return CsvImportResult(0, 0, 0, listOf("Invalid CSV header"))
        }

        val categories = categoryRepository.getAllCategoriesSnapshot()
        val categoryByNameAndType = categories.groupBy { it.name.lowercase(Locale.US) }
        var inserted = 0
        var updated = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        lines.drop(1).forEachIndexed { index, line ->
            val rowNumber = index + 2
            runCatching {
                val values = parseCsvLine(line)
                val row = header.mapIndexed { i, key -> key to values.getOrNull(i).orEmpty() }.toMap()

                val type = TransactionType.valueOf(row["type"]!!.uppercase(Locale.US))
                val amount = row["amount"]!!.toDouble()
                val description = row["description"].orEmpty()
                val subDescription = row["subdescription"].takeIf { !it.isNullOrBlank() }
                val date = dateFormat.parse(row["date"]!!) ?: Date()
                val accountType = AccountType.valueOf(row["account"]!!.uppercase(Locale.US))
                val toAccountRaw = row["toaccount"].orEmpty()
                val toAccountType = toAccountRaw.takeIf { it.isNotBlank() }
                    ?.let { AccountType.valueOf(it.uppercase(Locale.US)) }

                val categoryName = row["category"].orEmpty()
                val categoryId = if (categoryName.isBlank()) {
                    null
                } else {
                    categoryByNameAndType[categoryName.lowercase(Locale.US)]
                        ?.firstOrNull { it.type == type || type == TransactionType.TRANSFER || type == TransactionType.WALLET_MOVE }
                        ?.id
                        ?: categoryByNameAndType[categoryName.lowercase(Locale.US)]?.firstOrNull()?.id
                }

                val id = row["id"]?.toLongOrNull() ?: 0L
                val existing = if (id > 0L) transactionRepository.getTransactionById(id) else null

                val transaction = Transaction(
                    id = existing?.id ?: 0L,
                    amount = amount,
                    description = description,
                    subDescription = subDescription,
                    date = date,
                    type = type,
                    categoryId = categoryId,
                    accountType = accountType,
                    toAccountType = toAccountType,
                    startsNewPeriod = row["startsnewperiod"]?.toBooleanStrictOrNull() ?: false,
                    carriedForwardBalance = row["carriedforwardbalance"]?.toDoubleOrNull(),
                    linkedExpenseId = row["linkedexpenseid"]?.toLongOrNull(),
                    debtorNote = row["debtornote"].takeIf { !it.isNullOrBlank() },
                    awaitingReimbursement = row["awaitingreimbursement"]?.toBooleanStrictOrNull() ?: false,
                    allowNegativeBalance = row["allownegativebalance"]?.toBooleanStrictOrNull() ?: false,
                    createdAt = row["createdat"]?.let { dateFormat.parse(it) } ?: existing?.createdAt ?: Date(),
                    updatedAt = Date()
                )

                if (existing != null) {
                    transactionRepository.updateTransaction(transaction)
                    updated++
                } else {
                    transactionRepository.insertTransaction(transaction)
                    inserted++
                }
            }.onFailure { error ->
                skipped++
                errors.add("Row $rowNumber: ${error.message ?: "Unknown error"}")
            }
        }

        return CsvImportResult(inserted, updated, skipped, errors)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
