package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.ItemPriceRecord
import com.example.expensetracker.domain.repository.IItemPriceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackedItemComparison(
    val itemName: String,
    val latestPrice: Double,
    val previousPrice: Double?,
    val currency: String,
    val percentageChange: Double?, // e.g. +14.2%
    val latestDate: java.util.Date,
    val storeOrVendor: String?,
    val historyCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PriceHistoryViewModel @Inject constructor(
    private val itemPriceRepository: IItemPriceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedItem = MutableStateFlow<String?>(null)
    val selectedItem = _selectedItem.asStateFlow()

    // All latest tracked items with comparison
    val trackedItems: StateFlow<List<TrackedItemComparison>> = itemPriceRepository
        .getAllPriceRecords()
        .map { records ->
            // Group by normalized name
            val grouped = records.groupBy { it.normalizedItemName }
            grouped.map { (_, list) ->
                val sorted = list.sortedByDescending { it.date.time }
                val latest = sorted.first()
                val previous = sorted.getOrNull(1)
                val pctChange = if (previous != null && previous.unitPrice > 0) {
                    ((latest.unitPrice - previous.unitPrice) / previous.unitPrice) * 100.0
                } else null

                TrackedItemComparison(
                    itemName = latest.itemName,
                    latestPrice = latest.unitPrice,
                    previousPrice = previous?.unitPrice,
                    currency = latest.currency,
                    percentageChange = pctChange,
                    latestDate = latest.date,
                    storeOrVendor = latest.storeOrVendor,
                    historyCount = sorted.size
                )
            }.sortedByDescending { it.latestDate.time }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedItemHistory: StateFlow<List<ItemPriceRecord>> = _selectedItem
        .flatMapLatest { item ->
            if (item.isNullOrBlank()) flowOf(emptyList())
            else itemPriceRepository.getHistoryForItem(item)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectItem(itemName: String?) {
        _selectedItem.value = itemName
    }

    fun addManualPrice(
        itemName: String,
        unitPrice: Double,
        currency: String = "JOD",
        store: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            itemPriceRepository.recordItemPrice(
                itemName = itemName,
                unitPrice = unitPrice,
                currency = currency,
                storeOrVendor = store,
                notes = notes
            )
        }
    }

    fun deletePriceRecord(id: Long) {
        viewModelScope.launch {
            itemPriceRepository.deleteRecord(id)
        }
    }
}

