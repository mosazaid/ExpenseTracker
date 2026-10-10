package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
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
            recurringId = recurring.id
        )
        val insertedId = transactionRepository.insertTransaction(transaction)
        recurringRepository.advanceRecurringDueDate(recurring)
        return insertedId
    }
}
