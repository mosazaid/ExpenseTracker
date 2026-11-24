package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.domain.repository.ITransactionRepository
import javax.inject.Inject

/**
 * Clean Architecture Use Case: Add or Update Transaction
 * Encapsulates transactional validation and persistence.
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: ITransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        require(transaction.amount > 0) { "Transaction amount must be greater than zero" }
        return if (transaction.id == 0L) {
            transactionRepository.insertTransaction(transaction)
        } else {
            transactionRepository.updateTransaction(transaction)
            transaction.id
        }
    }
}
