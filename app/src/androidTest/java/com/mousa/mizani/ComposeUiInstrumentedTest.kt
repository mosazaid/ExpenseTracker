package com.mousa.mizani

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mousa.mizani.presentation.theme.MizaniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMizaniTheme_rendersTextCorrectly() {
        composeTestRule.setContent {
            MizaniTheme {
                Text(text = "Mizani Test Header")
            }
        }

        composeTestRule.onNodeWithText("Mizani Test Header").assertIsDisplayed()
    }
}
