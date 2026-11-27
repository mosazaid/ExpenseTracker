package com.mousa.mizani.presentation.feature.settings.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mousa.mizani.R
import com.mousa.mizani.core.export.FullBackupRestoreResult
import com.mousa.mizani.presentation.common.AppTopBar
import com.mousa.mizani.presentation.feature.settings.components.ExportDeliveryHost
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val ScreenPadding = 16.dp
private val ButtonMinHeight = 48.dp
private val BACKUP_MIME_TYPES = arrayOf("application/json", "text/*")

@Composable
fun BackupSettingsScreen(
    navController: NavController,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val lastBackupCreatedAt by viewModel.lastBackupCreatedAt.collectAsState()
    val exportRequest by viewModel.exportRequest.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRestoreUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    LaunchedEffect(viewModel) {
        viewModel.errors.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    ExportDeliveryHost(
        request = exportRequest,
        snackbarHostState = snackbarHostState,
        onDelivered = { viewModel.onDelivered(it) },
        onClear = viewModel::clearExportRequest
    )

    pendingRestoreUri?.let { uri ->
        RestoreConfirmDialog(
            onConfirm = {
                pendingRestoreUri = null
                viewModel.restore(uri)
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }

    // The result stays in the runner until acknowledged, so leaving the page does not lose it.
    (restoreState as? RestoreState.Finished)?.let { finished ->
        RestoreResultDialog(result = finished.result, onDismiss = viewModel::acknowledgeRestoreResult)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_backup_restore),
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
                .padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = lastBackupLabel(lastBackupCreatedAt),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.full_backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoWarning()
            if (isBusy) {
                BusyIndicator(isRestoring = restoreState == RestoreState.Running)
            }
            Button(
                onClick = viewModel::createBackup,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonMinHeight)
            ) {
                Text(stringResource(R.string.backup_now))
            }
            OutlinedButton(
                onClick = { pickBackupLauncher.launch(BACKUP_MIME_TYPES) },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonMinHeight)
            ) {
                Text(stringResource(R.string.full_backup_import_btn))
            }
        }
    }
}

/** "Last backup created: <date in the app's locale>", or "No backup yet". */
@Composable
fun lastBackupLabel(createdAt: Long?): String {
    if (createdAt == null) return stringResource(R.string.backup_none_yet)
    val locale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    val date = remember(createdAt, locale) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(createdAt))
    }
    return stringResource(R.string.backup_last_created, date)
}

@Composable
private fun PhotoWarning() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.backup_photos_warning),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BusyIndicator(isRestoring: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(if (isRestoring) R.string.restore_in_progress else R.string.backup_in_progress),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RestoreConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(stringResource(R.string.full_backup_import_confirm_title)) },
        text = { Text(stringResource(R.string.full_backup_import_confirm_desc)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.full_backup_import_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun RestoreResultDialog(result: FullBackupRestoreResult, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (result.isSuccess) R.string.restore_finished_title else R.string.restore_failed_title
                )
            )
        },
        text = {
            Text(
                text = restoreMessage(context, result),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}

/** Claims the settings were restored only when they were. */
private fun restoreMessage(context: Context, result: FullBackupRestoreResult): String {
    if (!result.isSuccess) {
        val reason = result.errorMessage ?: context.getString(R.string.unknown_error)
        return context.getString(R.string.full_backup_error, reason)
    }
    val restored = context.getString(
        restoredMessageRes(result), result.transactionCount, result.categoryCount, result.loanCount
    )
    val notes = restoreNoteRes(result).map { context.getString(it) }
    val fixed = if (result.fixedRecordsCount == 0) {
        emptyList()
    } else {
        listOf(context.getString(R.string.full_backup_fixed_records, result.fixedRecordsCount))
    }
    return (listOf(restored) + notes + fixed).joinToString(" ")
}
