package com.mousa.mizani.presentation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.mousa.mizani.R
import com.mousa.mizani.core.export.CsvExporter
import com.mousa.mizani.core.export.CsvImportResult
import com.mousa.mizani.core.export.CsvImporter
import com.mousa.mizani.core.export.PdfTransactionExporter
import com.mousa.mizani.core.export.StyledExcelExporter
import com.mousa.mizani.core.export.TransactionExportLoader
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.presentation.feature.settings.transfer.ExportFileUris
import com.mousa.mizani.presentation.feature.settings.transfer.ExportFormat
import com.mousa.mizani.presentation.feature.settings.transfer.ExportScope
import com.mousa.mizani.presentation.feature.settings.transfer.ImportExportViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.MockedStatic
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ImportExportViewModelTest {

    @get:Rule
    val cacheDir = TemporaryFolder()

    private val context = mock(Context::class.java)
    private val contentResolver = mock(ContentResolver::class.java)
    private val userPreferences = mock(UserPreferences::class.java)
    private val csvExporter = mock(CsvExporter::class.java)
    private val csvImporter = mock(CsvImporter::class.java)
    private val exportLoader = mock(TransactionExportLoader::class.java)
    private val excelExporter = mock(StyledExcelExporter::class.java)
    private val pdfExporter = mock(PdfTransactionExporter::class.java)
    private val periodCalculator = mock(PeriodCalculator::class.java)
    private val uri = mock(Uri::class.java)

    private val dispatcher = UnconfinedTestDispatcher()

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        log = mockStatic(Log::class.java)
        `when`(context.cacheDir).thenReturn(cacheDir.root)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(context.getString(anyInt())).thenReturn("title")
    }

    @After
    fun tearDown() {
        log.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(fileUris: ExportFileUris = ExportFileUris { uri }) = ImportExportViewModel(
        context, userPreferences, csvExporter, csvImporter, exportLoader,
        excelExporter, pdfExporter, periodCalculator, fileUris, dispatcher
    )

    private fun <T> TestScope.collect(flow: SharedFlow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { values.add(it) }
        }
        return values
    }

    @Test
    fun `failed report export emits export_failed and no request`() = runTest(dispatcher) {
        `when`(csvExporter.exportPlainCsv(null)).thenAnswer { throw IOException("disk full") }
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.export(ExportScope.ALL, ExportFormat.CSV)

        assertEquals(listOf(R.string.export_failed), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `failed styled report export emits export_failed and no request`() = runTest(dispatcher) {
        `when`(exportLoader.loadRows(null)).thenAnswer { throw IllegalStateException("db closed") }
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.export(ExportScope.ALL, ExportFormat.PDF)

        assertEquals(listOf(R.string.export_failed), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `failed template export emits export_failed and no request`() = runTest(dispatcher) {
        val viewModel = viewModel { throw IllegalArgumentException("no provider root") }
        val errors = collect(viewModel.errors)

        viewModel.exportTemplate()

        assertEquals(listOf(R.string.export_failed), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `template export makes a csv request without reading transactions`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.exportTemplate()

        val request = viewModel.exportRequest.value
        assertEquals("text/csv", request?.mimeType)
        assertEquals(false, request?.isFullBackup)
        assertEquals(emptyList<Int>(), errors)
        verifyNoInteractions(csvExporter, exportLoader, csvImporter)
    }

    @Test
    fun `report export is never marked as a full backup`() = runTest(dispatcher) {
        `when`(csvExporter.exportPlainCsv(null)).thenReturn("date,type,amount")
        val viewModel = viewModel()

        viewModel.export(ExportScope.ALL, ExportFormat.CSV)

        assertEquals(false, viewModel.exportRequest.value?.isFullBackup)
        viewModel.clearExportRequest()
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `cancelled export is not reported as an error`() = runTest(dispatcher) {
        `when`(csvExporter.exportPlainCsv(null)).thenAnswer { throw CancellationException() }
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.export(ExportScope.ALL, ExportFormat.CSV)

        assertEquals(emptyList<Int>(), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `import passes the file content on and keeps the result`() = runTest(dispatcher) {
        val expected = CsvImportResult(2, 1, 0, emptyList())
        `when`(contentResolver.openInputStream(uri)).thenReturn("csv".byteInputStream())
        `when`(csvImporter.importTransactions("csv")).thenReturn(expected)
        val viewModel = viewModel()

        viewModel.importCsv(uri)

        assertEquals(expected, viewModel.importResult.value)
    }

    @Test
    fun `unreadable import file reports the error and imports nothing`() = runTest(dispatcher) {
        `when`(contentResolver.openInputStream(uri)).thenAnswer { throw IOException("gone") }
        val viewModel = viewModel()

        viewModel.importCsv(uri)

        assertEquals(CsvImportResult(0, 0, 0, listOf("gone")), viewModel.importResult.value)
        verifyNoInteractions(csvImporter)
    }
}
