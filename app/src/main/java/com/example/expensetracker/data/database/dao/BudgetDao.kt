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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}
