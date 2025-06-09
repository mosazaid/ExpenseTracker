package com.example.expensetracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsByMonthAndYear(month: Int, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year")
    suspend fun getBudgetByCategoryAndDate(categoryId: Long, month: Int, year: Int): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("UPDATE budgets SET spent = :spent WHERE categoryId = :categoryId AND month = :month AND year = :year")
    suspend fun updateBudgetSpent(categoryId: Long, month: Int, year: Int, spent: Double)
}
