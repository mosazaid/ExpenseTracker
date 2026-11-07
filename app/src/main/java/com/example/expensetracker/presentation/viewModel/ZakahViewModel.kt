package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.DepositCalculator
import com.example.expensetracker.domain.ZakahCalculator
import com.example.expensetracker.domain.ZakahCalculator.ZakahAssessment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

data class ZakahUiState(
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val vaultBalance: Double = 0.0,
    val includeVault: Boolean = true,
    val otherAssets: Double = 0.0,
    val immediateDebts: Double = 0.0,
    val goldPricePerGram: Double = ZakahCalculator.DEFAULT_GOLD_PRICE_PER_GRAM,
    val goldGrams: Double = 0.0,
    val goldKarat: Int = 21,
    val hawlStartDate: Date = Date(System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000), // default ~6 months ago
    val assessment: ZakahAssessment? = null,
    val hawlDaysPassed: Int = 180,
    val hawlDaysRemaining: Int = 174
)

@HiltViewModel
class ZakahViewModel @Inject constructor(
    private val balanceCalculator: BalanceCalculator,
    private val depositCalculator: DepositCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZakahUiState())
    val uiState: StateFlow<ZakahUiState> = _uiState.asStateFlow()

    init {
        syncBalances()
    }

    fun syncBalances() {
        viewModelScope.launch {
            val balances = balanceCalculator.getAllTimeBalances()
            val vault = depositCalculator.getDepositBalance()
            val current = _uiState.value
            _uiState.value = current.copy(
                cashBalance = balances.cash,
                bankBalance = balances.bank,
                vaultBalance = vault
            )
            recalculate()
        }
    }

    fun setIncludeVault(include: Boolean) {
        _uiState.value = _uiState.value.copy(includeVault = include)
        recalculate()
    }

    fun setOtherAssets(amount: Double) {
        _uiState.value = _uiState.value.copy(otherAssets = amount)
        recalculate()
    }

    fun setImmediateDebts(amount: Double) {
        _uiState.value = _uiState.value.copy(immediateDebts = amount)
        recalculate()
    }

    fun setGoldPricePerGram(price: Double) {
        _uiState.value = _uiState.value.copy(goldPricePerGram = price)
        recalculate()
    }

    fun setGoldCalculator(grams: Double, karat: Int) {
        _uiState.value = _uiState.value.copy(goldGrams = grams, goldKarat = karat)
        recalculate()
    }

    fun setHawlStartDate(date: Date) {
        val now = System.currentTimeMillis()
        val diffDays = TimeUnit.MILLISECONDS.toDays(max(0L, now - date.time)).toInt()
        val hawlDuration = 354 // Lunar year in days
        val remaining = max(0, hawlDuration - diffDays)

        _uiState.value = _uiState.value.copy(
            hawlStartDate = date,
            hawlDaysPassed = diffDays,
            hawlDaysRemaining = remaining
        )
    }

    private fun recalculate() {
        val state = _uiState.value
        val effectiveVault = if (state.includeVault) state.vaultBalance else 0.0

        // Calculate gold value if grams > 0:
        // Pure 24k conversion: grams * (karat / 24.0) * goldPricePerGram
        val calculatedGoldValue = if (state.goldGrams > 0) {
            state.goldGrams * (state.goldKarat / 24.0) * state.goldPricePerGram
        } else 0.0

        val totalOtherAssets = state.otherAssets + calculatedGoldValue

        val assessment = ZakahCalculator.calculateZakah(
            cashBalance = state.cashBalance,
            bankBalance = state.bankBalance + effectiveVault,
            otherAssets = totalOtherAssets,
            immediateDebts = state.immediateDebts,
            goldPricePerGram = state.goldPricePerGram
        )

        _uiState.value = state.copy(assessment = assessment)
    }
}
