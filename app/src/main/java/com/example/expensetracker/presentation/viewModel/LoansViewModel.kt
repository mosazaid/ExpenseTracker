package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.model.MonthlyLoanItem
import com.example.expensetracker.domain.repository.ILoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val loanRepository: ILoanRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val currentMonthKey: String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    val lastLoanSheetDate: StateFlow<String?> = userPreferences.lastLoanSheetDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markLoanSheetShownToday() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        viewModelScope.launch {
            userPreferences.setLastLoanSheetDate(todayStr)
        }
    }

    val configuredLoans: StateFlow<List<ConfiguredLoan>> = loanRepository.getAllConfiguredLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _monthlyItems = MutableStateFlow<List<MonthlyLoanItem>>(emptyList())
    val monthlyItems: StateFlow<List<MonthlyLoanItem>> = _monthlyItems.asStateFlow()

    private val _preservedAmount = MutableStateFlow(0.0)
    val preservedAmount: StateFlow<Double> = _preservedAmount.asStateFlow()

    init {
        loadMonthlyData()
    }

    fun loadMonthlyData() {
        viewModelScope.launch {
            val items = loanRepository.getMonthlyLoanItems(currentMonthKey)
            _monthlyItems.value = items
            _preservedAmount.value = items.filter { !it.payment.isPaid }.sumOf { it.payment.amount }
        }
    }

    fun saveConfiguredLoan(name: String, amount: Double, accountType: AccountType, id: Long = 0L) {
        viewModelScope.launch {
            val loan = ConfiguredLoan(
                id = id,
                name = name,
                defaultAmount = amount,
                accountType = accountType,
                isActive = true
            )
            loanRepository.saveConfiguredLoan(loan)
            loadMonthlyData()
        }
    }

    fun deleteConfiguredLoan(id: Long) {
        viewModelScope.launch {
            loanRepository.deleteConfiguredLoan(id)
            loadMonthlyData()
        }
    }

    fun updatePaymentAmountForThisMonth(paymentId: Long, newAmount: Double) {
        viewModelScope.launch {
            loanRepository.updateMonthlyPaymentAmount(paymentId, newAmount)
            loadMonthlyData()
        }
    }

    fun deductAndPayLoan(
        paymentId: Long,
        accountType: AccountType,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = loanRepository.deductAndPayLoan(paymentId, accountType)
            loadMonthlyData()
            onComplete(result != null)
        }
    }
}
