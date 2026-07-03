package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.BudgetDao
import com.example.expensetracker.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {

    fun getBudgetsInPeriod(periodStart: Date, periodEnd: Date): Flow<List<Budget>> {
        return budgetDao.getBudgetsInPeriod(periodStart, periodEnd)
    }

    suspend fun getBudgetForCategoryInPeriod(
        categoryId: Long,
        periodStart: Date,
        periodEnd: Date
    ): Budget? {
        return budgetDao.getBudgetForCategoryInPeriod(categoryId, periodStart, periodEnd)
    }

    suspend fun upsertBudget(
        categoryId: Long,
        amount: Double,
        periodStart: Date,
        periodEnd: Date
    ): Long {
        val existing = budgetDao.getBudgetForCategoryInPeriod(categoryId, periodStart, periodEnd)
        return if (existing != null) {
            budgetDao.updateBudget(existing.copy(amount = amount))
            existing.id
        } else {
            budgetDao.insertBudget(
                Budget(
                    categoryId = categoryId,
                    amount = amount,
                    periodStart = periodStart,
                    periodEnd = periodEnd
                )
            )
        }
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
}
