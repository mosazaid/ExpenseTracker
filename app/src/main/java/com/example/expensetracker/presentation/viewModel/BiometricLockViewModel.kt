package com.example.expensetracker.presentation.viewModel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.example.expensetracker.core.security.BiometricAuthManager
import com.example.expensetracker.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class BiometricLockViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager,
    userPreferences: UserPreferences
) : ViewModel() {

    val biometricLockEnabled: StateFlow<Boolean> = userPreferences.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun lock() {
        _isUnlocked.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun requestUnlock(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        notRecognizedMessage: String
    ) {
        clearError()
        biometricAuthManager.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                _isUnlocked.value = true
                _errorMessage.value = null
            },
            onError = { message ->
                _isUnlocked.value = false
                _errorMessage.value = message
            },
            onFailedAttempt = {
                _isUnlocked.value = false
                _errorMessage.value = notRecognizedMessage
            }
        )
    }
}
