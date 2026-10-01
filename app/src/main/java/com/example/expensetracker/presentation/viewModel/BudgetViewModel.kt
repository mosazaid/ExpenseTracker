package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BudgetProgressCalculator
import com.example.expensetracker.domain.CategoryBudgetProgress
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.repository.IBudgetRepository
import com.example.expensetracker.domain.repository.ICategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: IBudgetRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val budgetProgressCalculator: BudgetProgressCalculator,
    private val categoryRepository: ICategoryRepository
) : ViewModel() {

    private val _budgetProgressMap = MutableStateFlow<Map<Long, CategoryBudgetProgress>>(emptyMap())
    val budgetProgressMap: StateFlow<Map<Long, CategoryBudgetProgress>> = _budgetProgressMap.asStateFlow()

    init {
        loadBudgetProgress()
    }

    fun loadBudgetProgress() {
        viewModelScope.launch {
            val mode = userPreferences.monthMode.first()
            val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, Date(), mode)
            val categories = categoryRepository.getAllCategoriesSnapshot()
            val progress = budgetProgressCalculator.getProgressMap(
                categories.map { it.id },
                bounds.start,
                bounds.end
            )
            _budgetProgressMap.value = progress
        }
    }

    suspend fun upsertCategoryBudget(categoryId: Long, amount: Double) {
        val mode = userPreferences.monthMode.first()
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, Date(), mode)
        budgetRepository.upsertBudget(categoryId, amount, bounds.start, bounds.end)
        loadBudgetProgress()
    }

    suspend fun deleteCategoryBudget(categoryId: Long) {
        budgetRepository.deleteBudgetsForCategory(categoryId)
        loadBudgetProgress()
    }
}
