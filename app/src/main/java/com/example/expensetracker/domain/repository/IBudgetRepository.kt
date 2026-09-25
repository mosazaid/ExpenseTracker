package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface IBudgetRepository {
    fun getBudgetsInPeriod(periodStart: Date, periodEnd: Date): Flow<List<Budget>>
    suspend fun getBudgetForCategoryInPeriod(categoryId: Long, periodStart: Date, periodEnd: Date): Budget?
    suspend fun upsertBudget(categoryId: Long, amount: Double, periodStart: Date, periodEnd: Date): Long
    suspend fun deleteBudget(budget: Budget)
}
