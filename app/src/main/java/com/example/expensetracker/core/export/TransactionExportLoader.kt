package com.example.expensetracker.core.export

import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionExportLoader @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun loadRows(filter: TransactionExportFilter? = null): List<TransactionExportRow> {
        val allTransactions = transactionRepository.getAllTransactionsSnapshot()
        val categories = categoryRepository.getAllCategoriesSnapshot()
        val categoryMap = categories.associateBy { it.id }

        return allTransactions
            .filter { txn ->
                val afterStart = filter?.startDate?.let { !txn.date.before(it) } ?: true
                val beforeEnd = filter?.endDate?.let { !txn.date.after(it) } ?: true
                val typeMatch = filter?.types?.contains(txn.type) ?: true
                afterStart && beforeEnd && typeMatch
            }
            .sortedByDescending { it.date }
            .map { txn ->
                TransactionExportRow(
                    id = txn.id,
                    date = txn.date,
                    type = txn.type,
                    categoryName = categoryMap[txn.categoryId]?.name.orEmpty(),
                    amount = txn.amount,
                    account = txn.accountType,
                    toAccount = txn.toAccountType,
                    description = txn.description,
                    subDescription = txn.subDescription.orEmpty(),
                    startsNewPeriod = txn.startsNewPeriod,
                    carriedForwardBalance = txn.carriedForwardBalance,
                    linkedExpenseId = txn.linkedExpenseId,
                    debtorNote = txn.debtorNote.orEmpty(),
                    awaitingReimbursement = txn.awaitingReimbursement,
                    allowNegativeBalance = txn.allowNegativeBalance,
                    createdAt = txn.createdAt,
                    updatedAt = txn.updatedAt
                )
            }
    }
}
