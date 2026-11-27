package com.mousa.mizani.presentation.feature.settings.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mousa.mizani.core.coroutines.ApplicationScope
import com.mousa.mizani.core.export.FullBackupManager
import com.mousa.mizani.core.export.FullBackupRestoreResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RestoreState {
    data object Idle : RestoreState
    data object Running : RestoreState

    /** Kept until the page has shown it, so it is not lost when the page goes away. */
    data class Finished(val result: FullBackupRestoreResult) : RestoreState
}

/**
 * Runs a backup restore in a scope that outlives the page, so leaving the page or recreating
 * the activity cannot stop a restore between replacing the records and importing the settings.
 */
@Singleton
class BackupRestoreRunner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fullBackupManager: FullBackupManager,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val state: StateFlow<RestoreState> = _state.asStateFlow()

    /** Starts restoring [uri]. Returns false, and does nothing, while another restore runs. */
    fun start(uri: Uri): Boolean {
        val isStarted = synchronized(this) {
            if (_state.value == RestoreState.Running) {
                false
            } else {
                _state.value = RestoreState.Running
                true
            }
        }
        if (isStarted) {
            scope.launch { _state.value = RestoreState.Finished(restore(uri)) }
        }
        return isStarted
    }

    /** Call once the result has been shown. */
    fun acknowledge() {
        synchronized(this) {
            if (_state.value is RestoreState.Finished) _state.value = RestoreState.Idle
        }
    }

    private suspend fun restore(uri: Uri): FullBackupRestoreResult = try {
        fullBackupManager.importFullBackup(readText(uri))
    } catch (e: CancellationException) {
        _state.value = RestoreState.Idle
        throw e
    } catch (e: Exception) {
        // The manager reports its own failures in the result, so this is a read failure.
        Log.e(FullBackupManager.TAG, "Could not read backup file", e)
        FullBackupRestoreResult(isSuccess = false, errorMessage = e.message)
    }

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Cannot read file")
}
