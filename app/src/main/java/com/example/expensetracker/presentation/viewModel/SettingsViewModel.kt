package com.example.expensetracker.presentation.viewModel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.RecurringRepository
import com.example.expensetracker.domain.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val csvExporter: CsvExporter,
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    val openingCashBalance = userPreferences.openingCashBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val openingBankBalance = userPreferences.openingBankBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeSalaryReminder = recurringRepository.getActiveRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

    fun setOpeningCashBalance(value: Double) {
        viewModelScope.launch {
            userPreferences.setOpeningCashBalance(value)
        }
    }

    fun setOpeningBankBalance(value: Double) {
        viewModelScope.launch {
            userPreferences.setOpeningBankBalance(value)
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val csv = csvExporter.exportTransactions()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(context.cacheDir, "expense_tracker_$timestamp.csv")
            file.writeText(csv)
            _exportUri.value = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }

    fun clearExportUri() {
        _exportUri.value = null
    }

    fun deactivateSalaryReminder(id: Long) {
        viewModelScope.launch {
            recurringRepository.deactivateRecurring(id)
        }
    }

    fun updateSalaryReminderAmount(recurring: RecurringTransaction, amount: Double) {
        viewModelScope.launch {
            recurringRepository.updateRecurring(recurring.copy(amount = amount))
        }
    }
}
