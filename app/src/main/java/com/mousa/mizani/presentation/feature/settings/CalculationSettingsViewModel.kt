package com.mousa.mizani.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.data.preferences.CalculationPreferences
import com.mousa.mizani.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculationSettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val calculationPreferences: StateFlow<CalculationPreferences> = userPreferences.calculationPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalculationPreferences())

    fun setIncludeDepositIncome(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcIncludeDepositIncome(enabled) }
    }

    fun setDeductLoansFromIncome(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcDeductLoansFromIncome(enabled) }
    }

    fun setIncludeWalletExpenses(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcIncludeWalletExpenses(enabled) }
    }

    fun setIncludeDepositExpenses(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcIncludeDepositExpenses(enabled) }
    }

    fun setIncludeLoanLinkedExpenses(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcIncludeLoanLinkedExpenses(enabled) }
    }

    fun setIncludeCarriedForwardInInsights(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcIncludeCarriedForwardInsights(enabled) }
    }

    fun setUseAvailableBalanceFloorInInsights(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcUseAvailableFloorInsights(enabled) }
    }

    fun setReserveWalletInInsights(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setCalcReserveWalletInsights(enabled) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { userPreferences.resetCalculationPreferences() }
    }
}
