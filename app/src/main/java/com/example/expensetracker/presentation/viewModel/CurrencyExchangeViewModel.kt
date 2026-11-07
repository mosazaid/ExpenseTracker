package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.CurrencyExchange
import com.example.expensetracker.domain.currency.CurrencyRates
import com.example.expensetracker.domain.repository.ICurrencyExchangeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CurrencyExchangeViewModel @Inject constructor(
    private val exchangeRepository: ICurrencyExchangeRepository
) : ViewModel() {

    val exchanges: StateFlow<List<CurrencyExchange>> = exchangeRepository
        .getAllExchanges()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addExchange(
        soldCurrency: String,
        soldAmount: Double,
        boughtCurrency: String,
        boughtAmount: Double,
        fee: Double,
        feeCurrency: String,
        location: String?,
        date: Date,
        notes: String?
    ) {
        viewModelScope.launch {
            val rate = if (soldAmount > 0) boughtAmount / soldAmount else 1.0
            val exchange = CurrencyExchange(
                soldCurrency = soldCurrency,
                soldAmount = soldAmount,
                boughtCurrency = boughtCurrency,
                boughtAmount = boughtAmount,
                exchangeRate = rate,
                fee = fee,
                feeCurrency = feeCurrency,
                location = location?.trim().takeIf { !it.isNullOrBlank() },
                date = date,
                notes = notes?.trim().takeIf { !it.isNullOrBlank() }
            )
            exchangeRepository.insertExchange(exchange)
        }
    }

    fun deleteExchange(id: Long) {
        viewModelScope.launch {
            exchangeRepository.deleteExchangeById(id)
        }
    }

    fun getSuggestedRate(soldCurrency: String, boughtCurrency: String): Double {
        return CurrencyRates.getDefaultRate(soldCurrency, boughtCurrency)
    }
}

