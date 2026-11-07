package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.database.dao.RecurringTransactionDao
import com.example.expensetracker.data.database.entities.RecurringTransaction
import java.util.Date
import javax.inject.Inject

data class AutoDeductExecutionResult(
    val executedCount: Int,
    val executedTransactions: List<RecurringTransaction>
)

class ExecuteAutoRecurringUseCase @Inject constructor(
    private val recurringDao: RecurringTransactionDao,
    private val executeRecurringUseCase: ExecuteRecurringTransactionUseCase
) {
    suspend operator fun invoke(executionDate: Date = Date()): AutoDeductExecutionResult {
        val dueList = recurringDao.getDueRecurringTransactions(executionDate)
        if (dueList.isEmpty()) {
            return AutoDeductExecutionResult(0, emptyList())
        }

        val executed = mutableListOf<RecurringTransaction>()
        for (item in dueList) {
            executeRecurringUseCase(recurring = item, executionDate = executionDate)
            executed.add(item)
        }

        return AutoDeductExecutionResult(
            executedCount = executed.size,
            executedTransactions = executed
        )
    }
}

