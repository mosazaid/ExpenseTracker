package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.BudgetRepository
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator
) : ViewModel() {

    suspend fun upsertCategoryBudget(categoryId: Long, amount: Double) {
        val mode = userPreferences.monthMode.first()
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, Date(), mode)
        budgetRepository.upsertBudget(categoryId, amount, bounds.start, bounds.end)
    }
}
