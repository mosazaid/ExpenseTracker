package com.example.expensetracker.presentation.viewModel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.RecurringRepository
import com.example.expensetracker.domain.BalanceCalculator
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
    private val recurringRepository: RecurringRepository,
    private val balanceCalculator: BalanceCalculator
) : ViewModel() {

    val activeSalaryReminder = recurringRepository.getActiveRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

    /**
     * Returns the actual current balance (opening balance + net effect of all recorded
     * transactions), not the raw stored opening value. This is what the user perceives as
     * "how much cash/bank money do I have right now".
     */
    suspend fun getCurrentCashBalance(): Double = balanceCalculator.getAvailableBalance(AccountType.CASH)

    suspend fun getCurrentBankBalance(): Double = balanceCalculator.getAvailableBalance(AccountType.BANK)

    /**
     * Recalibrates the account so its current balance becomes exactly [target]. Unlike
     * writing to the raw opening balance, this correctly accounts for any income/expense
     * already recorded, so the value the user enters is the value they'll see immediately.
     */
    suspend fun setCurrentCashBalance(target: Double) {
        balanceCalculator.setCurrentBalance(AccountType.CASH, target)
    }

    suspend fun setCurrentBankBalance(target: Double) {
        balanceCalculator.setCurrentBalance(AccountType.BANK, target)
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
