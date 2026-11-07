package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.ICategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SadaqahJarViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val categoryRepository: ICategoryRepository
) : ViewModel() {

    val isRoundUpEnabled: StateFlow<Boolean> = userPreferences.sadaqahRoundUpEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val jarBalance: StateFlow<Double> = userPreferences.sadaqahJarBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isFridayReminderEnabled: StateFlow<Boolean> = userPreferences.sadaqahFridayReminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setRoundUpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSadaqahRoundUpEnabled(enabled)
        }
    }

    fun setFridayReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSadaqahFridayReminderEnabled(enabled)
        }
    }

    fun emptyJar() {
        viewModelScope.launch {
            userPreferences.emptySadaqahJar()
        }
    }

    suspend fun getCharityCategory(): Category? {
        val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
        return categories.find { it.name.contains("Charity", ignoreCase = true) }
    }
}

