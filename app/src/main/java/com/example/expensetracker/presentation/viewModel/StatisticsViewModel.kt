package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val periodCalculator: PeriodCalculator,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _statisticsState = MutableStateFlow(StatisticsState())
    val statisticsState: StateFlow<StatisticsState> = _statisticsState.asStateFlow()

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    fun loadStatisticsForPeriod(period: HistoryPeriod, referenceDate: Date = Date()) {
        viewModelScope.launch {
            _statisticsState.value = _statisticsState.value.copy(isLoading = true)
            val bounds = periodCalculator.getBounds(period, referenceDate, monthMode.value)
            val totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME, bounds.start, bounds.end
            )
            val totalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE, bounds.start, bounds.end
            )
            _statisticsState.value = _statisticsState.value.copy(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense,
                periodLabel = bounds.label,
                isLoading = false
            )
        }
    }
}

data class StatisticsState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val periodLabel: String = "",
    val isLoading: Boolean = false
)
