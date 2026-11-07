package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.DepositCalculator
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.domain.runway.FinancialRunwayCalculator
import com.example.expensetracker.domain.runway.FinancialRunwayResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

data class RunwayUiState(
    val runwayResult: FinancialRunwayResult? = null,
    val emergencySimulatedExpense: Double = 0.0,
    val simulatedRunwayMonths: Double? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class FinancialRunwayViewModel @Inject constructor(
    private val balanceCalculator: BalanceCalculator,
    private val depositCalculator: DepositCalculator,
    private val transactionRepository: ITransactionRepository
) : ViewModel() {

    private val _simulatedEmergency = MutableStateFlow(0.0)
    val simulatedEmergency = _simulatedEmergency.asStateFlow()

    val uiState: StateFlow<RunwayUiState> = combine(
        transactionRepository.getAllTransactions(),
        _simulatedEmergency
    ) { transactions, simulatedShock ->
        val balances = balanceCalculator.getAllTimeBalances()
        val vaultBalance = depositCalculator.getDepositBalance()
        val totalLiquid = balances.cash + balances.bank + vaultBalance

        // Calculate average monthly essential expense over past 90 days
        val ninetyDaysAgo = Date(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
        val recentExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE &&
            !it.excludeFromMonthExpenses &&
            it.date.after(ninetyDaysAgo)
        }
        val total90DaySpend = recentExpenses.sumOf { it.amount }
        // Monthly average (90 days = 3 months)
        val monthlyBurn = if (total90DaySpend > 0) total90DaySpend / 3.0 else 350.0 // fallback default

        val baseRunway = FinancialRunwayCalculator.calculate(totalLiquid, monthlyBurn)
        val simulated = if (simulatedShock > 0) {
            FinancialRunwayCalculator.simulate(totalLiquid, monthlyBurn, simulatedShock).runwayMonths
        } else {
            baseRunway.runwayMonths
        }

        RunwayUiState(
            runwayResult = baseRunway,
            emergencySimulatedExpense = simulatedShock,
            simulatedRunwayMonths = simulated,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RunwayUiState()
    )

    fun setSimulatedEmergency(amount: Double) {
        _simulatedEmergency.value = amount
    }
}
