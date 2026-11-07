package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.domain.islamic.MurabahaCalculator
import com.example.expensetracker.domain.islamic.MurabahaResult
import com.example.expensetracker.domain.repository.ILoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MurabahaInputState(
    val assetName: String = "Car Financing",
    val assetPriceStr: String = "15000",
    val downPaymentStr: String = "3000",
    val profitRateStr: String = "5.5",
    val tenorYearsStr: String = "5"
)

@HiltViewModel
class MurabahaSimulatorViewModel @Inject constructor(
    private val loanRepository: ILoanRepository
) : ViewModel() {

    private val _inputState = MutableStateFlow(MurabahaInputState())
    val inputState: StateFlow<MurabahaInputState> = _inputState.asStateFlow()

    private val _calculationResult = MutableStateFlow(computeResult(_inputState.value))
    val calculationResult: StateFlow<MurabahaResult> = _calculationResult.asStateFlow()

    private fun computeResult(inputs: MurabahaInputState): MurabahaResult {
        val price = inputs.assetPriceStr.toDoubleOrNull() ?: 0.0
        val down = inputs.downPaymentStr.toDoubleOrNull() ?: 0.0
        val rate = inputs.profitRateStr.toDoubleOrNull() ?: 0.0
        val tenor = inputs.tenorYearsStr.toIntOrNull() ?: 1
        return MurabahaCalculator.calculate(price, down, rate, tenor)
    }

    fun updateInputs(
        assetName: String = _inputState.value.assetName,
        priceStr: String = _inputState.value.assetPriceStr,
        downStr: String = _inputState.value.downPaymentStr,
        rateStr: String = _inputState.value.profitRateStr,
        tenorStr: String = _inputState.value.tenorYearsStr
    ) {
        val newState = _inputState.value.copy(
            assetName = assetName,
            assetPriceStr = priceStr,
            downPaymentStr = downStr,
            profitRateStr = rateStr,
            tenorYearsStr = tenorStr
        )
        _inputState.value = newState
        _calculationResult.value = computeResult(newState)
    }

    fun saveAsConfiguredLoan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = _calculationResult.value
            if (res.monthlyInstallment > 0) {
                val loan = ConfiguredLoan(
                    name = "Murabaha: ${_inputState.value.assetName.ifBlank { "Islamic Loan" }}",
                    defaultAmount = res.monthlyInstallment,
                    accountType = AccountType.BANK,
                    deductFromIncome = true,
                    isActive = true
                )
                loanRepository.saveConfiguredLoan(loan)
                onSuccess()
            }
        }
    }
}
