package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.repository.ICategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: ICategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    val allCategories = categoryRepository.getAllCategories()

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryRepository.getCategoriesByType(type)
    }

    fun insertCategory(category: Category) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                categoryRepository.insertCategory(category)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                categoryRepository.updateCategory(category)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                categoryRepository.deleteCategory(category)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class CategoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
