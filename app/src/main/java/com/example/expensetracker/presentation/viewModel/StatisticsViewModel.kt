package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _statisticsState = MutableStateFlow(StatisticsState())
    val statisticsState: StateFlow<StatisticsState> = _statisticsState.asStateFlow()

    init {
        loadCurrentMonthStatistics()
    }

    private fun loadCurrentMonthStatistics() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val endOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            val totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME, startOfMonth, endOfMonth
            )

            val totalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE, startOfMonth, endOfMonth
            )

            _statisticsState.value = _statisticsState.value.copy(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense
            )
        }
    }

    fun loadStatisticsForPeriod(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            val totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME, startDate, endDate
            )

            val totalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE, startDate, endDate
            )

            _statisticsState.value = _statisticsState.value.copy(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense
            )
        }
    }
}

data class StatisticsState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = false
)