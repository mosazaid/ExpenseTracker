package com.example.expensetracker.presentation.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.Manifest
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import com.example.expensetracker.core.location.LocationHelper
import com.example.expensetracker.core.permission.PermissionHelper
import com.example.expensetracker.presentation.components.PermissionRationaleDialog
import com.example.expensetracker.presentation.components.PermissionType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.export.ExportFileHelper
import com.example.expensetracker.core.locale.AppLanguage
import com.example.expensetracker.core.export.CsvImportFormat
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.core.time.DateUtils
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.navigation.Categories
import com.example.expensetracker.presentation.navigation.DatabaseBrowser
import com.example.expensetracker.presentation.navigation.NotificationSettings
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
    val themeMode by viewModel.themeMode.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
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
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val locationHelper = remember { LocationHelper(context) }
    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDeniedLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            val detected = locationHelper.detectLocationInfo()
            if (detected != null) {
                viewModel.setCurrencyCode(detected.detectedCurrencyCode)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Detected: ${detected.detectedCurrencyCode} (${detected.cityName ?: detected.countryCode})")
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location unavailable. Please select manually.")
                }
            }
        } else {
            val isPermanently = (context as? android.app.Activity)?.let {
                !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            isPermanentlyDeniedLocation = isPermanently
            showLocationRationaleDialog = true
        }
    }
    var exportDialogRequest by remember { mutableStateOf<ExportShareRequest?>(null) }
    var pendingSaveRequest by remember { mutableStateOf<ExportShareRequest?>(null) }

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
            onShare = {
                runCatching {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = request.mimeType
                        putExtra(Intent.EXTRA_STREAM, request.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${request.fileName}"))
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
            AppTopBar(
                title = stringResource(R.string.settings),
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Section 1: Appearance & Language
            Text("1. Appearance & Language", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.language), style = MaterialTheme.typography.labelMedium)
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

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.currency_settings_title), style = MaterialTheme.typography.labelMedium)
                TextButton(
                    onClick = {
                        if (PermissionHelper.hasLocationPermission(context)) {
                            val detected = locationHelper.detectLocationInfo()
                            if (detected != null) {
                                viewModel.setCurrencyCode(detected.detectedCurrencyCode)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Detected: ${detected.detectedCurrencyCode} (${detected.cityName ?: detected.countryCode})")
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Location unavailable. Please select manually.")
                                }
                            }
                        } else {
                            val shouldShowRationale = (context as? android.app.Activity)?.let {
                                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
                            } ?: false
                            if (shouldShowRationale) {
                                isPermanentlyDeniedLocation = false
                                showLocationRationaleDialog = true
                            } else {
                                locationPermissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-detect via GPS", style = MaterialTheme.typography.labelSmall)
                }
            }
            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded }
            ) {
                val activeInfo = CurrencyUtils.getCurrencyInfo(currencyCode)
                OutlinedTextField(
                    value = "${activeInfo.code} (${activeInfo.symbol} / ${activeInfo.symbolAr})",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    CurrencyUtils.SUPPORTED_CURRENCIES.forEach { curr ->
                        DropdownMenuItem(
                            text = { Text("${curr.code} — ${curr.symbol} (${curr.symbolAr})") },
                            onClick = {
                                viewModel.setCurrencyCode(curr.code)
                                currencyMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Dark Mode", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.expensetracker.data.preferences.ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = {
                            Text(
                                when (mode) {
                                    com.example.expensetracker.data.preferences.ThemeMode.SYSTEM -> "System"
                                    com.example.expensetracker.data.preferences.ThemeMode.LIGHT -> "Light"
                                    com.example.expensetracker.data.preferences.ThemeMode.DARK -> "Dark"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.theme_palette_label), style = MaterialTheme.typography.labelMedium)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.expensetracker.data.preferences.ThemePalette.entries.forEach { palette ->
                    val isSelected = themePalette == palette
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setThemePalette(palette) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color(palette.primaryHex))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color(palette.secondaryHex))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color(palette.tertiaryHex))
                                    )
                                }
                                Text(
                                    text = stringResource(palette.titleRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            HorizontalDivider()

            // ── Section 2: Currency & Balances
            Text("2. Currency & Balances", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Text(
                stringResource(R.string.balances_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            OutlinedTextField(
                value = cashInput,
                onValueChange = { cashInput = CurrencyUtils.cleanDecimalInput(it); justSaved = false },
                label = { Text(stringResource(R.string.current_cash_balance)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bankInput,
                onValueChange = { bankInput = CurrencyUtils.cleanDecimalInput(it); justSaved = false },
                label = { Text(stringResource(R.string.current_bank_balance)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
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

            // ── Section 3: Alerts & Notifications
            Text("3. Alerts & Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(NotificationSettings) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.manage_notifications_btn),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daily 9 PM logging, budget limits, salary rollovers, and loan alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider()

            // ── Section 4: Security & Privacy
            Text("4. Security & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.biometric_lock_enable), fontWeight = FontWeight.Medium)
                Switch(
                    checked = biometricLockEnabled,
                    onCheckedChange = { viewModel.setBiometricLockEnabled(it) }
                )
            }

            HorizontalDivider()

            // ── Section 5: Data & Backup
            Text("5. Data & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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

            // ── Section 6: Developer Tools
            Text("6. Developer Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { navController.navigate(DatabaseBrowser) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.database_browser))
            }

            HorizontalDivider()

            Text(
                "Expense Tracker v1.1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    if (showLocationRationaleDialog) {
        PermissionRationaleDialog(
            permissionType = PermissionType.LOCATION,
            isPermanentlyDenied = isPermanentlyDeniedLocation,
            onConfirm = {
                showLocationRationaleDialog = false
                if (isPermanentlyDeniedLocation) {
                    PermissionHelper.openAppSettings(context)
                } else {
                    locationPermissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
                }
            },
            onDismiss = { showLocationRationaleDialog = false }
        )
    }
}

@Composable
private fun ExportReadyDialog(
    request: ExportShareRequest,
    onDismiss: () -> Unit,
    onView: () -> Unit,
    onShare: () -> Unit,
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
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                    Text("Share File")
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
