package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface BudgetDao {

    @Query(
        """
        SELECT * FROM budgets
        WHERE periodStart = :periodStart AND periodEnd = :periodEnd
        """
    )
    fun getBudgetsInPeriod(periodStart: Date, periodEnd: Date): Flow<List<Budget>>

    @Query(
        """
        SELECT * FROM budgets
        WHERE categoryId = :categoryId
          AND periodStart = :periodStart
          AND periodEnd = :periodEnd
        LIMIT 1
        """
    )
    suspend fun getBudgetForCategoryInPeriod(
        categoryId: Long,
        periodStart: Date,
        periodEnd: Date
    ): Budget?

    @Query(
        """
        SELECT * FROM budgets
        WHERE categoryId = :categoryId
        ORDER BY periodEnd DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun getLatestBudgetForCategory(categoryId: Long): Budget?

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteAllBudgetsForCategory(categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>): List<Long>

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY id ASC")
    suspend fun getAllBudgetsSnapshot(): List<Budget>

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}
