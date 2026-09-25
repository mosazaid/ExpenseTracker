package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.Budget
import com.example.expensetracker.domain.repository.IBudgetRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class CategoryBudgetProgress(
    val categoryId: Long,
    val limit: Double,
    val spent: Double
) {
    val percent: Float
        get() = if (limit <= 0) 0f else (spent / limit).toFloat().coerceIn(0f, 1.5f)

    val isOverBudget: Boolean get() = spent > limit

    val isNearLimit: Boolean get() = !isOverBudget && spent >= limit * 0.9
}

@Singleton
class BudgetProgressCalculator @Inject constructor(
    private val budgetRepository: IBudgetRepository,
    private val transactionRepository: ITransactionRepository
) {

    suspend fun getProgressForCategory(
        categoryId: Long,
        periodStart: Date,
        periodEnd: Date
    ): CategoryBudgetProgress? {
        val budget = budgetRepository.getBudgetForCategoryInPeriod(categoryId, periodStart, periodEnd)
            ?: return null
        val spent = transactionRepository.getTotalAmountByCategoryAndDateRange(
            categoryId,
            periodStart,
            periodEnd
        )
        return CategoryBudgetProgress(
            categoryId = categoryId,
            limit = budget.amount,
            spent = spent
        )
    }

    suspend fun getProgressMap(
        categoryIds: List<Long>,
        periodStart: Date,
        periodEnd: Date
    ): Map<Long, CategoryBudgetProgress> {
        return categoryIds.mapNotNull { id ->
            getProgressForCategory(id, periodStart, periodEnd)?.let { id to it }
        }.toMap()
    }
}
