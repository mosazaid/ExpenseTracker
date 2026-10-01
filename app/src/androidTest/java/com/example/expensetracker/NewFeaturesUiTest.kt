package com.example.expensetracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.CategoryBudgetProgress
import com.example.expensetracker.presentation.components.CategoryBudgetsCard
import com.example.expensetracker.presentation.components.CategoryRow
import com.example.expensetracker.presentation.components.FinancialHealthScoreCard
import com.example.expensetracker.presentation.components.FinancialInsightsCard
import com.example.expensetracker.presentation.components.TransactionDetailBottomSheet
import com.example.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class NewFeaturesUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCategoryRow_rendersWithoutBudget() {
        val category = Category(
            id = 1L,
            name = "Groceries",
            icon = "ShoppingCart",
            color = "#4CAF50",
            type = TransactionType.EXPENSE,
            isDefault = true
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryRow(
                    category = category,
                    budgetProgress = null,
                    onEdit = {},
                    onDelete = {},
                    onSetBudget = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ Set Budget").assertIsDisplayed()
    }

    @Test
    fun testCategoryRow_rendersWithBudget() {
        val category = Category(
            id = 2L,
            name = "Entertainment",
            icon = "Movie",
            color = "#9C27B0",
            type = TransactionType.EXPENSE,
            isDefault = false
        )
        val progress = CategoryBudgetProgress(
            categoryId = 2L,
            limit = 500.0,
            spent = 250.0
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryRow(
                    category = category,
                    budgetProgress = progress,
                    onEdit = {},
                    onDelete = {},
                    onSetBudget = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Entertainment").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly Budget").assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun testCategoryBudgetsCard_rendersEmptyState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryBudgetsCard(
                    budgetProgressMap = emptyMap(),
                    categoryMap = emptyMap(),
                    onManageBudgets = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Category Budgets").assertIsDisplayed()
        composeTestRule.onNodeWithText("No category budgets set").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set Up Budgets").assertIsDisplayed()
    }

    @Test
    fun testCategoryBudgetsCard_rendersActiveBudgets() {
        val cat = Category(
            id = 5L,
            name = "Dining Out",
            icon = "Restaurant",
            color = "#FF9800",
            type = TransactionType.EXPENSE
        )
        val progress = CategoryBudgetProgress(categoryId = 5L, limit = 200.0, spent = 160.0)

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryBudgetsCard(
                    budgetProgressMap = mapOf(5L to progress),
                    categoryMap = mapOf(5L to cat),
                    onManageBudgets = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Category Budgets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dining Out").assertIsDisplayed()
        composeTestRule.onNodeWithText("80%").assertIsDisplayed()
    }

    @Test
    fun testFinancialInsightsCard_rendersOnTrack() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FinancialInsightsCard(
                    totalIncome = 3000.0,
                    totalExpense = 800.0,
                    totalWallet = 0.0
                )
            }
        }

        composeTestRule.onNodeWithText("AI Spending Insights").assertIsDisplayed()
        composeTestRule.onNodeWithText("ON TRACK").assertIsDisplayed()
    }

    @Test
    fun testFinancialHealthScoreCard_rendersScoreCard() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FinancialHealthScoreCard(
                    totalIncome = 5000.0,
                    totalExpense = 1500.0,
                    budgetAdherenceRatio = 0.9f,
                    activeLoansUnpaidCount = 0
                )
            }
        }

        composeTestRule.onNodeWithText("Financial Health Score").assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial Master").assertIsDisplayed()
    }

    @Test
    fun testTransactionDetailBottomSheet_rendersTransactionDetails() {
        val transaction = Transaction(
            id = 101L,
            amount = 45.50,
            description = "Evening treats",
            date = Date(),
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            accountType = AccountType.WALLET
        )
        val category = Category(
            id = 1L,
            name = "Snacks & Sweets",
            icon = "Restaurant",
            color = "#FF5722",
            type = TransactionType.EXPENSE
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TransactionDetailBottomSheet(
                    transaction = transaction,
                    category = category,
                    splits = emptyList(),
                    onDismissRequest = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Transaction Details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snacks & Sweets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Evening treats").assertIsDisplayed()
    }
}
