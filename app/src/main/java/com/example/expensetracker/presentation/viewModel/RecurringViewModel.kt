package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val recurringList: StateFlow<List<RecurringTransaction>> = recurringRepository
        .getAllRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryMap: StateFlow<Map<Long, Category>> = categoryRepository
        .getAllCategories()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun toggleActive(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            recurringRepository.setRecurringActive(id, !currentActive)
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringById(id)
        }
    }
}
