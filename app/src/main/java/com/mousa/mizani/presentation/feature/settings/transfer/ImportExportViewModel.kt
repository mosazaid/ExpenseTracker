package com.mousa.mizani.presentation.feature.settings.transfer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.R
import com.mousa.mizani.core.coroutines.IoDispatcher
import com.mousa.mizani.core.export.CsvExporter
import com.mousa.mizani.core.export.CsvImportFormat
import com.mousa.mizani.core.export.CsvImportResult
import com.mousa.mizani.core.export.CsvImporter
import com.mousa.mizani.core.export.PdfTransactionExporter
import com.mousa.mizani.core.export.StyledExcelExporter
import com.mousa.mizani.core.export.TransactionExportFilter
import com.mousa.mizani.core.export.TransactionExportLoader
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.constants.HistoryPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val TAG = "ImportExport"
private val UTF8_BOM = Char(0xFEFF).toString()
private const val CSV_MIME = "text/csv"

/** Report export, CSV import and the blank import template. */
@HiltViewModel
class ImportExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val csvExporter: CsvExporter,
    private val csvImporter: CsvImporter,
    private val exportLoader: TransactionExportLoader,
    private val styledExcelExporter: StyledExcelExporter,
    private val pdfTransactionExporter: PdfTransactionExporter,
    private val periodCalculator: PeriodCalculator,
    private val exportFileUris: ExportFileUris,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _exportRequest = MutableStateFlow<ExportShareRequest?>(null)
    val exportRequest: StateFlow<ExportShareRequest?> = _exportRequest.asStateFlow()

    private val _errors = MutableSharedFlow<Int>()
    /** String resources describing a failure to show to the user. */
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    private val _importResult = MutableStateFlow<CsvImportResult?>(null)
    /** The last import's result; kept here so it survives the page being recreated. */
    val importResult: StateFlow<CsvImportResult?> = _importResult.asStateFlow()

    fun export(scope: ExportScope, format: ExportFormat) {
        createFile("Could not export the report") {
            val filter = buildFilter(scope)
            val file = File(context.cacheDir, reportFileName(scope, format))
            when (format) {
                ExportFormat.CSV -> writeCsv(file, csvExporter.exportPlainCsv(filter))
                ExportFormat.EXCEL ->
                    styledExcelExporter.exportToFile(exportLoader.loadRows(filter), labelOf(filter), file)
                ExportFormat.PDF ->
                    pdfTransactionExporter.exportToFile(exportLoader.loadRows(filter), labelOf(filter), file)
            }
            ExportShareRequest(
                uri = exportFileUris.uriFor(file),
                mimeType = format.mimeType,
                fileName = file.name,
                title = context.getString(format.titleRes)
            )
        }
    }

    /** Blank CSV matching the import format. Holds none of the user's transactions. */
    fun exportTemplate() {
        createFile("Could not export the import template") {
            val file = File(context.cacheDir, "expense_tracker_import_template_${timestamp()}.csv")
            writeCsv(file, CsvImportFormat.template())
            ExportShareRequest(
                uri = exportFileUris.uriFor(file),
                mimeType = CSV_MIME,
                fileName = file.name,
                title = context.getString(R.string.download_import_template)
            )
        }
    }

    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            val result = try {
                val content = withContext(ioDispatcher) { readText(uri) }
                csvImporter.importTransactions(content)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Could not import the CSV file", e)
                CsvImportResult(0, 0, 0, listOf(e.message ?: context.getString(R.string.import_failed)))
            }
            _importResult.value = result
        }
    }

    fun clearExportRequest() {
        _exportRequest.value = null
    }

    private fun createFile(failureLog: String, create: suspend () -> ExportShareRequest) {
        viewModelScope.launch {
            try {
                _exportRequest.value = withContext(ioDispatcher) { create() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, failureLog, e)
                _errors.emit(R.string.export_failed)
            }
        }
    }

    private suspend fun buildFilter(scope: ExportScope): TransactionExportFilter? = when (scope) {
        ExportScope.ALL -> null
        ExportScope.CURRENT_MONTH -> {
            val bounds = periodCalculator.getBounds(
                period = HistoryPeriod.MONTH,
                referenceDate = Date(),
                monthMode = userPreferences.monthMode.first()
            )
            TransactionExportFilter(startDate = bounds.start, endDate = bounds.end, label = bounds.label)
        }
    }

    private fun labelOf(filter: TransactionExportFilter?): String =
        filter?.label ?: context.getString(R.string.export_all)

    private fun reportFileName(scope: ExportScope, format: ExportFormat): String {
        val suffix = when (scope) {
            ExportScope.ALL -> "all"
            ExportScope.CURRENT_MONTH -> "month"
        }
        return "expense_tracker_${suffix}_${timestamp()}.${format.extension}"
    }

    // The BOM helps Excel on Windows recognise the encoding, including Arabic text.
    private fun writeCsv(file: File, csv: String) = file.writeText("$UTF8_BOM$csv", Charsets.UTF_8)

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Cannot read file")
}

private val ExportFormat.extension: String
    get() = when (this) {
        ExportFormat.CSV -> "csv"
        ExportFormat.EXCEL -> "html"
        ExportFormat.PDF -> "pdf"
    }

private val ExportFormat.mimeType: String
    get() = when (this) {
        ExportFormat.CSV -> CSV_MIME
        ExportFormat.EXCEL -> "text/html"
        ExportFormat.PDF -> "application/pdf"
    }

private val ExportFormat.titleRes: Int
    get() = when (this) {
        ExportFormat.CSV -> R.string.export_csv
        ExportFormat.EXCEL -> R.string.export_excel
        ExportFormat.PDF -> R.string.export_pdf
    }
