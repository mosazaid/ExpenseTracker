package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import java.util.Date
import javax.inject.Inject

/**
 * Clean Architecture Use Case: Execute Recurring Transaction
 * Encapsulates creating the concrete transaction instance and advancing the recurrence schedule.
 */
class ExecuteRecurringTransactionUseCase @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val recurringRepository: IRecurringRepository
) {
    suspend operator fun invoke(
        recurring: RecurringTransaction,
        executionDate: Date = Date()
    ): Long {
        val transaction = Transaction(
            amount = recurring.amount,
            description = recurring.description,
            date = executionDate,
            type = recurring.type,
            categoryId = recurring.categoryId,
            accountType = recurring.accountType,
            recurringId = recurring.id,
            // Same rule as manual entry: Wallet and Vault spending stays out of monthly expenses.
            excludeFromMonthExpenses = recurring.accountType == AccountType.WALLET ||
                recurring.accountType == AccountType.DEPOSIT
        )
        val insertedId = transactionRepository.insertTransaction(transaction)
        recurringRepository.advanceRecurringDueDate(recurring)
        return insertedId
    }
}
