package com.mousa.mizani.presentation.feature.settings.transfer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.core.export.CsvImportResult
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.settings.components.ExportDeliveryHost
import com.mousa.mizani.presentation.feature.settings.components.SettingsChoiceRow
import com.mousa.mizani.presentation.feature.settings.components.SettingsGroupHeader

private val ScreenPadding = 16.dp
private val ButtonMinHeight = 48.dp
private val CSV_MIME_TYPES = arrayOf("text/*", "text/csv", "application/csv")

@Composable
fun ImportExportSettingsScreen(
    navController: NavController,
    viewModel: ImportExportViewModel = hiltViewModel()
) {
    val exportRequest by viewModel.exportRequest.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var exportScope by rememberSaveable { mutableStateOf(ExportScope.ALL) }
    var exportFormat by rememberSaveable { mutableStateOf(ExportFormat.CSV) }
    var isFormatExpanded by rememberSaveable { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importCsv)
    }

    LaunchedEffect(viewModel) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    // Reports and templates are not backups, so delivering them records nothing.
    ExportDeliveryHost(
        request = exportRequest,
        snackbarHostState = snackbarHostState,
        onDelivered = {},
        onClear = viewModel::clearExportRequest
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_import_export),
                navController = navController,
                canNavigateBack = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = ScreenPadding)
        ) {
            SettingsGroupHeader(stringResource(R.string.export_report_title))
            SettingsChoiceRow(
                title = stringResource(R.string.export_filter),
                options = ExportScope.entries,
                selected = exportScope,
                label = { stringResource(it.labelRes) },
                onSelect = { exportScope = it }
            )
            SettingsChoiceRow(
                title = stringResource(R.string.export_format),
                options = ExportFormat.entries,
                selected = exportFormat,
                label = { stringResource(it.labelRes) },
                onSelect = { exportFormat = it }
            )
            PaddedColumn {
                HelpText(stringResource(R.string.export_format_help))
                Button(
                    onClick = { viewModel.export(exportScope, exportFormat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonMinHeight)
                ) {
                    Text(stringResource(R.string.export))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = ScreenPadding))
            SettingsGroupHeader(stringResource(R.string.import_section_title))
            PaddedColumn {
                OutlinedButton(
                    onClick = { importLauncher.launch(CSV_MIME_TYPES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonMinHeight)
                ) {
                    Text(stringResource(R.string.import_csv))
                }
                importResult?.let { result ->
                    Text(
                        text = importMessage(result),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ImportFormatCard(
                    expanded = isFormatExpanded,
                    onToggle = { isFormatExpanded = !isFormatExpanded },
                    onDownloadTemplate = viewModel::exportTemplate
                )
            }
        }
    }
}

@Composable
private fun PaddedColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

@Composable
private fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The importer's first error when nothing was imported; otherwise the counts. */
@Composable
private fun importMessage(result: CsvImportResult): String =
    if (result.errors.isNotEmpty() && result.inserted == 0 && result.updated == 0) {
        result.errors.first()
    } else {
        stringResource(R.string.import_result, result.inserted, result.updated, result.skipped)
    }

@get:StringRes
private val ExportScope.labelRes: Int
    get() = when (this) {
        ExportScope.ALL -> R.string.export_all
        ExportScope.CURRENT_MONTH -> R.string.export_current_month
    }

@get:StringRes
private val ExportFormat.labelRes: Int
    get() = when (this) {
        ExportFormat.CSV -> R.string.export_csv
        ExportFormat.EXCEL -> R.string.export_excel
        ExportFormat.PDF -> R.string.export_pdf
    }
