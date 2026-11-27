package com.mousa.mizani.presentation.feature.settings.backup

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.R
import com.mousa.mizani.core.export.FullBackupManager
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.presentation.feature.settings.transfer.ExportShareRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val fullBackupManager: FullBackupManager,
    private val backupFileSharer: BackupFileSharer,
    private val restoreRunner: BackupRestoreRunner
) : ViewModel() {

    val lastBackupCreatedAt: StateFlow<Long?> = userPreferences.lastBackupCreatedAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _exportRequest = MutableStateFlow<ExportShareRequest?>(null)
    val exportRequest: StateFlow<ExportShareRequest?> = _exportRequest.asStateFlow()

    private val _errors = MutableSharedFlow<Int>()
    /** String resources describing a failure to show to the user. */
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    /** The restore in progress or its unshown result; outlives this view model. */
    val restoreState: StateFlow<RestoreState> = restoreRunner.state

    private val isCreating = MutableStateFlow(false)

    /** True while a backup is being created or restored: neither may start then. */
    val isBusy: StateFlow<Boolean> = combine(isCreating, restoreRunner.state) { creating, restore ->
        creating || restore == RestoreState.Running
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun createBackup() {
        if (isCreating.value || restoreRunner.state.value == RestoreState.Running) return
        isCreating.value = true
        viewModelScope.launch {
            try {
                val file = fullBackupManager.exportFullBackup()
                _exportRequest.value = backupFileSharer.toShareRequest(file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(FullBackupManager.TAG, "Could not create backup", e)
                _errors.emit(R.string.export_failed)
            } finally {
                isCreating.value = false
            }
        }
    }

    /** Call once the file has been saved, or the share window has opened for it. */
    fun onDelivered(request: ExportShareRequest, now: Long = System.currentTimeMillis()) {
        if (!request.isFullBackup) return
        viewModelScope.launch {
            try {
                userPreferences.setLastBackupCreatedAt(now)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The backup itself was delivered, so this is not reported as an export failure.
                Log.e(FullBackupManager.TAG, "Could not record the backup time", e)
            }
        }
    }

    /** Starts the restore unless a backup is being created or another restore runs. */
    fun restore(uri: Uri) {
        if (isCreating.value) return
        restoreRunner.start(uri)
    }

    /** Call once the restore result has been shown to the user. */
    fun acknowledgeRestoreResult() {
        restoreRunner.acknowledge()
    }

    fun clearExportRequest() {
        _exportRequest.value = null
    }
}
