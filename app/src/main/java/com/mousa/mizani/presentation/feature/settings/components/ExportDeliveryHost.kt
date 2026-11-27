package com.mousa.mizani.presentation.feature.settings.components

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mousa.mizani.R
import com.mousa.mizani.core.export.ExportFileHelper
import com.mousa.mizani.presentation.feature.settings.transfer.ExportShareRequest
import kotlinx.coroutines.launch

private const val TAG = "ExportDelivery"
private val ButtonMinHeight = 48.dp

/**
 * Shows the "export ready" dialog for [request]: view, share, or save to Downloads (with the
 * document picker when Downloads cannot be used). [onDelivered] runs after a successful save or
 * copy, or once the share window has started; never for View, dismissing, or a failure.
 * [onClear] runs when the dialog is done with the request.
 */
@Composable
fun ExportDeliveryHost(
    request: ExportShareRequest?,
    snackbarHostState: SnackbarHostState,
    onDelivered: (ExportShareRequest) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentRequest by rememberUpdatedState(request)
    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    // The request stays open while the picker is shown, so it survives the activity being recreated.
    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        val pending = currentRequest
        if (destinationUri != null && pending != null) {
            runCatching { ExportFileHelper.copyToUri(context, pending.uri, destinationUri) }
                .onSuccess {
                    showMessage(context.getString(R.string.export_saved))
                    onDelivered(pending)
                }
                .onFailure { error ->
                    Log.e(TAG, "Could not copy the file", error)
                    showMessage(context.getString(R.string.export_save_failed))
                }
        }
        onClear()
    }

    val shown = request ?: return
    ExportReadyDialog(
        request = shown,
        onDismiss = onClear,
        onView = {
            runCatching { context.startActivity(viewIntent(shown)) }
                .onFailure { error ->
                    Log.w(TAG, "Could not open the file", error)
                    showMessage(context.getString(R.string.export_view_failed))
                }
            onClear()
        },
        onShare = {
            runCatching { context.startActivity(shareIntent(context, shown)) }
                .onSuccess { onDelivered(shown) }
                .onFailure { error ->
                    Log.w(TAG, "Could not share the file", error)
                    showMessage(context.getString(R.string.export_share_failed))
                }
            onClear()
        },
        onDownload = {
            ExportFileHelper.saveToDownloads(
                context = context,
                sourceUri = shown.uri,
                fileName = shown.fileName,
                mimeType = shown.mimeType
            ).onSuccess { savedName ->
                showMessage(context.getString(R.string.export_saved_to_downloads, savedName))
                onDelivered(shown)
                onClear()
            }.onFailure { error ->
                Log.w(TAG, "Could not save to Downloads; asking where to save", error)
                runCatching { saveDocumentLauncher.launch(shown.fileName) }
                    .onFailure {
                        showMessage(context.getString(R.string.export_save_failed))
                        onClear()
                    }
            }
        }
    )
}

private fun viewIntent(request: ExportShareRequest): Intent =
    Intent.createChooser(ExportFileHelper.buildViewIntent(request.uri, request.mimeType), request.title)

private fun shareIntent(context: Context, request: ExportShareRequest): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = request.mimeType
        putExtra(Intent.EXTRA_STREAM, request.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, context.getString(R.string.export_share_chooser_title, request.fileName))
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
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.export_ready_title), style = MaterialTheme.typography.titleLarge)
                Text(request.fileName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.export_ready_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonMinHeight)
                Button(onClick = onView, modifier = buttonModifier) {
                    Text(stringResource(R.string.export_view))
                }
                OutlinedButton(onClick = onShare, modifier = buttonModifier) {
                    Text(stringResource(R.string.export_share))
                }
                OutlinedButton(onClick = onDownload, modifier = buttonModifier) {
                    Text(stringResource(R.string.export_download))
                }
                TextButton(onClick = onDismiss, modifier = buttonModifier) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
