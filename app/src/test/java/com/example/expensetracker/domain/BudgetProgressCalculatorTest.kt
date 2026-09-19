package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.Budget
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

class BudgetProgressCalculatorTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var calculator: BudgetProgressCalculator

    @Before
    fun setup() {
        budgetRepository = Mockito.mock(BudgetRepository::class.java)
        transactionRepository = Mockito.mock(TransactionRepository::class.java)
        calculator = BudgetProgressCalculator(budgetRepository, transactionRepository)
    }

    @Test
    fun testCategoryBudgetProgress_underLimit() {
        val progress = CategoryBudgetProgress(categoryId = 1L, limit = 500.0, spent = 250.0)
        assertEquals(0.5f, progress.percent, 0.001f)
        assertFalse(progress.isNearLimit)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun testCategoryBudgetProgress_nearLimit() {
        val progress = CategoryBudgetProgress(categoryId = 1L, limit = 500.0, spent = 460.0) // 92%
        assertEquals(0.92f, progress.percent, 0.001f)
        assertTrue(progress.isNearLimit)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun testCategoryBudgetProgress_overBudget() {
        val progress = CategoryBudgetProgress(categoryId = 1L, limit = 500.0, spent = 550.0)
        assertEquals(1.1f, progress.percent, 0.001f)
        assertFalse(progress.isNearLimit)
        assertTrue(progress.isOverBudget)
    }

    @Test
    fun testGetProgressForCategory_whenBudgetExists() = runTest {
        val start = Date(1000L)
        val end = Date(2000L)
        val budget = Budget(id = 1L, categoryId = 10L, amount = 300.0, periodStart = start, periodEnd = end)

        Mockito.`when`(budgetRepository.getBudgetForCategoryInPeriod(10L, start, end)).thenReturn(budget)
        Mockito.`when`(transactionRepository.getTotalAmountByCategoryAndDateRange(10L, start, end)).thenReturn(150.0)

        val result = calculator.getProgressForCategory(10L, start, end)

        assertNotNull(result)
        assertEquals(10L, result!!.categoryId)
        assertEquals(300.0, result.limit, 0.001)
        assertEquals(150.0, result.spent, 0.001)
        assertEquals(0.5f, result.percent, 0.001f)
    }

    @Test
    fun testGetProgressForCategory_whenBudgetMissing() = runTest {
        val start = Date(1000L)
        val end = Date(2000L)

        Mockito.`when`(budgetRepository.getBudgetForCategoryInPeriod(10L, start, end)).thenReturn(null)

        val result = calculator.getProgressForCategory(10L, start, end)
        assertNull(result)
    }
}
