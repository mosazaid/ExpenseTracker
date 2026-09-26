package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.model.MonthlyLoanItem
import com.example.expensetracker.domain.repository.ILoanRepository
import com.example.expensetracker.core.format.CurrencyUtils
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
    private val userPreferences: UserPreferences,
    private val balanceCalculator: BalanceCalculator
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

    fun saveConfiguredLoan(
        name: String,
        amount: Double,
        accountType: AccountType,
        deductFromIncome: Boolean = true,
        id: Long = 0L
    ) {
        viewModelScope.launch {
            val loan = ConfiguredLoan(
                id = id,
                name = name,
                defaultAmount = amount,
                accountType = accountType,
                deductFromIncome = deductFromIncome,
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

    suspend fun getAvailableBalance(accountType: AccountType): Double {
        return balanceCalculator.getAvailableBalance(accountType)
    }

    fun deductAndPayLoan(
        paymentId: Long,
        customAmount: Double? = null,
        accountType: AccountType? = null,
        onComplete: (success: Boolean, errorMessage: String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val payment = loanRepository.getPaymentById(paymentId)
            if (payment == null) {
                onComplete(false, "Loan payment not found")
                return@launch
            }
            val loanConfig = loanRepository.getLoanById(payment.loanConfigId)
            val targetAccount = accountType ?: loanConfig?.accountType ?: payment.accountType
            val finalAmount = customAmount ?: payment.amount

            if (finalAmount <= 0.0) {
                onComplete(false, "Amount must be greater than zero")
                return@launch
            }

            val currentBalance = balanceCalculator.getAvailableBalance(targetAccount)
            if (finalAmount > currentBalance) {
                val accName = if (targetAccount == AccountType.BANK) "Bank" else "Cash"
                onComplete(
                    false,
                    "Insufficient $accName balance. Available: ${CurrencyUtils.formatCurrency(currentBalance)}, Required: ${CurrencyUtils.formatCurrency(finalAmount)}"
                )
                return@launch
            }

            if (finalAmount != payment.amount) {
                loanRepository.updateMonthlyPaymentAmount(paymentId, finalAmount)
            }

            val result = loanRepository.deductAndPayLoan(paymentId, targetAccount)
            loadMonthlyData()
            if (result != null) {
                onComplete(true, null)
            } else {
                onComplete(false, "Failed to deduct loan")
            }
        }
    }

    fun getPaymentHistoryForLoanFlow(loanConfigId: Long): kotlinx.coroutines.flow.Flow<List<com.example.expensetracker.data.database.dao.LoanPaymentHistoryItem>> {
        return loanRepository.getPaymentHistoryForLoanFlow(loanConfigId)
    }
}
