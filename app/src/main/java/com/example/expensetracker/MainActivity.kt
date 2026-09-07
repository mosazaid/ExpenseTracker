package com.example.expensetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.expensetracker.presentation.components.BiometricGate
import com.example.expensetracker.presentation.screens.MainScreen
import com.example.expensetracker.presentation.theme.ExpenseTrackerTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.expensetracker.data.preferences.ThemeMode
import com.example.expensetracker.data.preferences.UserPreferences
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            ExpenseTrackerTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    BiometricGate {
                        MainScreen(
                            initialRecurringId = intent?.getLongExtra("recurringId", -1L)
                                ?.takeIf { it != -1L }
                        )
                    }
                }
            }
        }
    }
}
