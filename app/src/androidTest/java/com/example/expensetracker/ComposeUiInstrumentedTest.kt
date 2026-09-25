package com.example.expensetracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.presentation.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testExpenseTrackerTheme_rendersTextCorrectly() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                Text(text = "Expense Tracker Test Header")
            }
        }

        composeTestRule.onNodeWithText("Expense Tracker Test Header").assertIsDisplayed()
    }
}
