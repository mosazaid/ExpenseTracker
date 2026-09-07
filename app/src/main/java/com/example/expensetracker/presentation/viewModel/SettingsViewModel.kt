package com.example.expensetracker.presentation.viewModel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.locale.AppLanguage
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.RecurringRepository
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.CsvExporter
import com.example.expensetracker.domain.CsvImportResult
import com.example.expensetracker.domain.CsvImporter
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PdfTransactionExporter
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.StyledExcelExporter
import com.example.expensetracker.domain.TransactionExportFilter
import com.example.expensetracker.domain.TransactionExportLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ExportScope {
    ALL,
    CURRENT_MONTH
}

enum class ExportFormat {
    CSV,
    EXCEL,
    PDF
}

data class ExportShareRequest(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val title: String
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val csvExporter: CsvExporter,
    private val csvImporter: CsvImporter,
    private val exportLoader: TransactionExportLoader,
    private val styledExcelExporter: StyledExcelExporter,
    private val pdfTransactionExporter: PdfTransactionExporter,
    private val recurringRepository: RecurringRepository,
    private val balanceCalculator: BalanceCalculator,
    private val periodCalculator: PeriodCalculator,
    private val notificationHelper: com.example.expensetracker.util.NotificationHelper
) : ViewModel() {

    val activeSalaryReminder = recurringRepository.getActiveRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val language: StateFlow<AppLanguage> = userPreferences.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val themeMode: StateFlow<com.example.expensetracker.data.preferences.ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.expensetracker.data.preferences.ThemeMode.SYSTEM)

    val biometricLockEnabled: StateFlow<Boolean> = userPreferences.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _exportRequest = MutableStateFlow<ExportShareRequest?>(null)
    val exportRequest: StateFlow<ExportShareRequest?> = _exportRequest.asStateFlow()

    private val _importResult = MutableSharedFlow<CsvImportResult>()
    val importResult = _importResult.asSharedFlow()

    suspend fun getCurrentCashBalance(): Double = balanceCalculator.getAvailableBalance(AccountType.CASH)

    suspend fun getCurrentBankBalance(): Double = balanceCalculator.getAvailableBalance(AccountType.BANK)

    suspend fun setCurrentCashBalance(target: Double) {
        balanceCalculator.setCurrentBalance(AccountType.CASH, target)
    }

    suspend fun setCurrentBankBalance(target: Double) {
        balanceCalculator.setCurrentBalance(AccountType.BANK, target)
    }

    fun export(scope: ExportScope = ExportScope.ALL, format: ExportFormat = ExportFormat.CSV) {
        viewModelScope.launch {
            val filter = buildFilter(scope)
            val suffix = when (scope) {
                ExportScope.ALL -> "all"
                ExportScope.CURRENT_MONTH -> "month"
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val rows = exportLoader.loadRows(filter)
            val filterLabel = filter?.label ?: "All transactions"

            val (fileName, mimeType, title) = when (format) {
                ExportFormat.CSV -> Triple(
                    "expense_tracker_${suffix}_$timestamp.csv",
                    "text/csv",
                    context.getString(com.example.expensetracker.R.string.export_csv)
                )
                ExportFormat.EXCEL -> Triple(
                    "expense_tracker_${suffix}_$timestamp.html",
                    "text/html",
                    context.getString(com.example.expensetracker.R.string.export_excel)
                )
                ExportFormat.PDF -> Triple(
                    "expense_tracker_${suffix}_$timestamp.pdf",
                    "application/pdf",
                    context.getString(com.example.expensetracker.R.string.export_pdf)
                )
            }

            val file = File(context.cacheDir, fileName)
            when (format) {
                ExportFormat.CSV -> {
                    val csv = csvExporter.exportPlainCsv(filter)
                    // UTF-8 BOM helps Excel on Windows recognize encoding (incl. Arabic text).
                    file.writeText("\uFEFF$csv", Charsets.UTF_8)
                }
                ExportFormat.EXCEL -> styledExcelExporter.exportToFile(rows, filterLabel, file)
                ExportFormat.PDF -> pdfTransactionExporter.exportToFile(rows, filterLabel, file)
            }

            _exportRequest.value = ExportShareRequest(
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                ),
                mimeType = mimeType,
                fileName = fileName,
                title = title
            )
        }
    }

    /** Plain CSV matching the import format — for re-import / template. */
    fun exportImportTemplate() {
        viewModelScope.launch {
            val csv = csvExporter.exportPlainCsv(filter = null)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "expense_tracker_import_template_$timestamp.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText("\uFEFF$csv", Charsets.UTF_8)
            _exportRequest.value = ExportShareRequest(
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                ),
                mimeType = "text/csv",
                fileName = fileName,
                title = context.getString(com.example.expensetracker.R.string.download_import_template)
            )
        }
    }

    private suspend fun buildFilter(scope: ExportScope): TransactionExportFilter? {
        return when (scope) {
            ExportScope.ALL -> null
            ExportScope.CURRENT_MONTH -> {
                val mode = monthMode.value
                val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, Date(), mode)
                TransactionExportFilter(
                    startDate = bounds.start,
                    endDate = bounds.end,
                    label = bounds.label
                )
            }
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: throw IllegalStateException("Cannot read file")
            }.onSuccess { content ->
                val result = csvImporter.importTransactions(content)
                _importResult.emit(result)
            }.onFailure { error ->
                _importResult.emit(
                    CsvImportResult(0, 0, 0, listOf(error.message ?: "Import failed"))
                )
            }
        }
    }

    fun clearExportRequest() {
        _exportRequest.value = null
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferences.setLanguage(language)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setBiometricLockEnabled(enabled)
        }
    }

    fun setThemeMode(mode: com.example.expensetracker.data.preferences.ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return notificationHelper.areNotificationsEnabled()
    }

    fun sendTestSalaryNotification() {
        notificationHelper.showTestNotification()
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
