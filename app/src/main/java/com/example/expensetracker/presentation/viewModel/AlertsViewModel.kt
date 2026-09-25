package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AppAlert
import com.example.expensetracker.domain.repository.IAlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: IAlertRepository
) : ViewModel() {

    val alerts: StateFlow<List<AppAlert>> = alertRepository.getActiveAlertsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissAlert(id: Long) {
        viewModelScope.launch {
            alertRepository.dismissAlert(id)
        }
    }
}
