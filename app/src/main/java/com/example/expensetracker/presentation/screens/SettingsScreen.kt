package com.example.expensetracker.presentation.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.export.ExportFileHelper
import com.example.expensetracker.core.locale.AppLanguage
import com.example.expensetracker.domain.CsvImportFormat
import com.example.expensetracker.presentation.navigation.AppRoutes
import com.example.expensetracker.presentation.theme.CurrencyUtils
import com.example.expensetracker.presentation.viewModel.ExportFormat
import com.example.expensetracker.presentation.viewModel.ExportScope
import com.example.expensetracker.presentation.viewModel.ExportShareRequest
import com.example.expensetracker.presentation.viewModel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val salaryReminders by viewModel.activeSalaryReminder.collectAsState()
    val exportRequest by viewModel.exportRequest.collectAsState()
    val language by viewModel.language.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    var currentCash by remember { mutableStateOf(0.0) }
    var currentBank by remember { mutableStateOf(0.0) }
    var cashInput by remember { mutableStateOf("") }
    var bankInput by remember { mutableStateOf("") }
    var justSaved by remember { mutableStateOf(false) }
    var exportScope by remember { mutableStateOf(ExportScope.ALL) }
    var exportFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var importFormatExpanded by remember { mutableStateOf(false) }
    var exportDialogRequest by remember { mutableStateOf<ExportShareRequest?>(null) }
    var pendingSaveRequest by remember { mutableStateOf<ExportShareRequest?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        val request = pendingSaveRequest
        pendingSaveRequest = null
        if (destinationUri != null && request != null) {
            runCatching {
                ExportFileHelper.copyToUri(context, request.uri, destinationUri)
            }.onSuccess {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.export_saved))
                }
            }.onFailure {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.export_save_failed))
                }
            }
        }
        exportDialogRequest = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importCsv(it) }
    }

    LaunchedEffect(Unit) {
        currentCash = viewModel.getCurrentCashBalance()
        currentBank = viewModel.getCurrentBankBalance()
        cashInput = currentCash.toString()
        bankInput = currentBank.toString()
    }

    LaunchedEffect(exportRequest) {
        exportRequest?.let { request ->
            exportDialogRequest = request
            viewModel.clearExportRequest()
        }
    }

    exportDialogRequest?.let { request ->
        ExportReadyDialog(
            request = request,
            onDismiss = { exportDialogRequest = null },
            onView = {
                runCatching {
                    context.startActivity(
                        Intent.createChooser(
                            ExportFileHelper.buildViewIntent(request.uri, request.mimeType),
                            request.title
                        )
                    )
                }.onFailure { error ->
                    val message = if (error is ActivityNotFoundException) {
                        context.getString(R.string.export_view_failed)
                    } else {
                        error.message ?: context.getString(R.string.export_view_failed)
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
                exportDialogRequest = null
            },
            onDownload = {
                ExportFileHelper.saveToDownloads(
                    context = context,
                    sourceUri = request.uri,
                    fileName = request.fileName,
                    mimeType = request.mimeType
                ).onSuccess { savedName ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.export_saved_to_downloads, savedName)
                        )
                    }
                    exportDialogRequest = null
                }.onFailure {
                    pendingSaveRequest = request
                    saveDocumentLauncher.launch(request.fileName)
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.importResult.collect { result ->
            importMessage = if (result.errors.isNotEmpty() && result.inserted == 0 && result.updated == 0) {
                result.errors.first()
            } else {
                context.getString(
                    R.string.import_result,
                    result.inserted,
                    result.updated,
                    result.skipped
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = language == AppLanguage.ENGLISH,
                onClick = {
                    viewModel.setLanguage(AppLanguage.ENGLISH)
                    activity?.recreate()
                },
                label = { Text(stringResource(R.string.english)) }
            )
            FilterChip(
                selected = language == AppLanguage.ARABIC,
                onClick = {
                    viewModel.setLanguage(AppLanguage.ARABIC)
                    activity?.recreate()
                },
                label = { Text(stringResource(R.string.arabic)) }
            )
        }

        HorizontalDivider()

        Text(stringResource(R.string.biometric_lock), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.biometric_lock_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.biometric_lock_enable))
            Switch(
                checked = biometricLockEnabled,
                onCheckedChange = { viewModel.setBiometricLockEnabled(it) }
            )
        }

        HorizontalDivider()

        Text(stringResource(R.string.database_browser), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.database_browser_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Button(
            onClick = { navController.navigate(AppRoutes.DATABASE_BROWSER) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.database_browser))
        }

        HorizontalDivider()

        Text(stringResource(R.string.balances), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.balances_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        OutlinedTextField(
            value = cashInput,
            onValueChange = { cashInput = it; justSaved = false },
            label = { Text(stringResource(R.string.current_cash_balance)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bankInput,
            onValueChange = { bankInput = it; justSaved = false },
            label = { Text(stringResource(R.string.current_bank_balance)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.setCurrentCashBalance(cashInput.toDoubleOrNull() ?: currentCash)
                    viewModel.setCurrentBankBalance(bankInput.toDoubleOrNull() ?: currentBank)
                    currentCash = viewModel.getCurrentCashBalance()
                    currentBank = viewModel.getCurrentBankBalance()
                    cashInput = currentCash.toString()
                    bankInput = currentBank.toString()
                    justSaved = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_current_balances))
        }
        if (justSaved) {
            Text(
                stringResource(R.string.balances_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        Text(stringResource(R.string.salary_reminder_section), style = MaterialTheme.typography.titleSmall)
        if (salaryReminders.isEmpty()) {
            Text(
                stringResource(R.string.no_salary_reminder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            salaryReminders.forEach { reminder ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${reminder.description} — ${CurrencyUtils.formatCurrency(reminder.amount)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Next: ${com.example.expensetracker.presentation.theme.DateUtils.formatDate(reminder.nextDueDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(onClick = { viewModel.deactivateSalaryReminder(reminder.id) }) {
                            Text(stringResource(R.string.turn_off_reminder), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text(stringResource(R.string.data), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.export_filter), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = exportScope == ExportScope.ALL,
                onClick = { exportScope = ExportScope.ALL },
                label = { Text(stringResource(R.string.export_all)) }
            )
            FilterChip(
                selected = exportScope == ExportScope.CURRENT_MONTH,
                onClick = { exportScope = ExportScope.CURRENT_MONTH },
                label = { Text(stringResource(R.string.export_current_month)) }
            )
        }

        Text(stringResource(R.string.export_format), style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = exportFormat == ExportFormat.CSV,
                onClick = { exportFormat = ExportFormat.CSV },
                label = { Text(stringResource(R.string.export_csv)) }
            )
            FilterChip(
                selected = exportFormat == ExportFormat.EXCEL,
                onClick = { exportFormat = ExportFormat.EXCEL },
                label = { Text(stringResource(R.string.export_excel)) }
            )
            FilterChip(
                selected = exportFormat == ExportFormat.PDF,
                onClick = { exportFormat = ExportFormat.PDF },
                label = { Text(stringResource(R.string.export_pdf)) }
            )
        }
        Text(
            stringResource(R.string.export_format_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Button(
            onClick = { viewModel.export(exportScope, exportFormat) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.export))
        }

        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.import_csv))
        }
        importMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        ImportFormatCard(
            expanded = importFormatExpanded,
            onToggle = { importFormatExpanded = !importFormatExpanded },
            onDownloadTemplate = { viewModel.exportImportTemplate() }
        )

        HorizontalDivider()

        Text(
            "Expense Tracker v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        }
    }
}

@Composable
private fun ExportReadyDialog(
    request: ExportShareRequest,
    onDismiss: () -> Unit,
    onView: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.export_ready_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    request.fileName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.export_ready_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Button(onClick = onView, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.export_view))
                }
                OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.export_download))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ImportFormatCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    onDownloadTemplate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.import_csv_format),
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            Text(
                stringResource(R.string.import_csv_format_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            if (expanded) {
                Text(
                    stringResource(R.string.import_required_columns),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    CsvImportFormat.REQUIRED_COLUMNS.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    stringResource(R.string.import_optional_columns),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    CsvImportFormat.OPTIONAL_COLUMNS.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    stringResource(R.string.import_type_values),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(CsvImportFormat.TYPE_VALUES, style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.import_account_values),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(CsvImportFormat.ACCOUNT_VALUES, style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.import_date_format),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(CsvImportFormat.DATE_FORMAT, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                Text(
                    stringResource(R.string.import_example_row),
                    style = MaterialTheme.typography.labelMedium
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        CsvImportFormat.HEADER,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        CsvImportFormat.EXAMPLE_ROW,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    stringResource(R.string.import_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                OutlinedButton(onClick = onDownloadTemplate, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.download_import_template))
                }
            }
        }
    }
}
