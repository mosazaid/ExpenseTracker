package com.example.expensetracker.presentation.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    val loanAlertsEnabled: StateFlow<Boolean> = userPreferences.loanAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val debtAlertsEnabled: StateFlow<Boolean> = userPreferences.debtAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val salaryAlertsEnabled: StateFlow<Boolean> = userPreferences.salaryAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val budgetAlertsEnabled: StateFlow<Boolean> = userPreferences.budgetAlertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dailyReminderEnabled: StateFlow<Boolean> = userPreferences.dailyReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun areSystemNotificationsEnabled(): Boolean = notificationHelper.areNotificationsEnabled()

    fun setLoanAlerts(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setLoanAlertsEnabled(enabled) }
    }

    fun setDebtAlerts(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDebtAlertsEnabled(enabled) }
    }

    fun setSalaryAlerts(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSalaryAlertsEnabled(enabled) }
    }

    fun setBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setBudgetAlertsEnabled(enabled) }
    }

    fun setDailyReminder(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDailyReminderEnabled(enabled) }
    }

    fun sendTestNotification() {
        notificationHelper.showDailyExpenseReminderNotification()
    }
}
