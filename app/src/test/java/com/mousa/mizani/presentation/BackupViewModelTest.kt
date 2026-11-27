package com.mousa.mizani.presentation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.mousa.mizani.R
import com.mousa.mizani.core.export.FullBackupManager
import com.mousa.mizani.core.export.FullBackupRestoreResult
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.presentation.feature.settings.backup.BackupFileSharer
import com.mousa.mizani.presentation.feature.settings.backup.BackupRestoreRunner
import com.mousa.mizani.presentation.feature.settings.backup.BackupViewModel
import com.mousa.mizani.presentation.feature.settings.backup.RestoreState
import com.mousa.mizani.presentation.feature.settings.transfer.ExportShareRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val context = mock(Context::class.java)
    private val contentResolver = mock(ContentResolver::class.java)
    private val userPreferences = mock(UserPreferences::class.java)
    private val fullBackupManager = mock(FullBackupManager::class.java)
    private val uri = mock(Uri::class.java)
    private val backupFile = File("mizani_full_backup_test.json")

    private val sharer = BackupFileSharer { file ->
        ExportShareRequest(uri, "application/json", file.name, "Backup", isFullBackup = true)
    }

    private val dispatcher = UnconfinedTestDispatcher()

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        log = mockStatic(Log::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(userPreferences.lastBackupCreatedAt).thenReturn(flowOf(null))
    }

    @After
    fun tearDown() {
        log.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(
        fileSharer: BackupFileSharer = sharer,
        restoreRunner: BackupRestoreRunner =
            BackupRestoreRunner(context, fullBackupManager, CoroutineScope(dispatcher))
    ) = BackupViewModel(userPreferences, fullBackupManager, fileSharer, restoreRunner)

    private fun <T> TestScope.collect(flow: SharedFlow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { values.add(it) }
        }
        return values
    }

    private fun request(isFullBackup: Boolean) =
        ExportShareRequest(uri, "text/csv", "file", "title", isFullBackup)

    @Test
    fun `delivering a full backup records the time`() = runTest(dispatcher) {
        viewModel().onDelivered(request(isFullBackup = true), now = 1000L)

        verify(userPreferences, times(1)).setLastBackupCreatedAt(1000L)
    }

    @Test
    fun `delivering a report records nothing`() = runTest(dispatcher) {
        viewModel().onDelivered(request(isFullBackup = false), now = 1000L)

        verify(userPreferences, never()).setLastBackupCreatedAt(1000L)
    }

    @Test
    fun `failing to record the time does not crash`() = runTest(dispatcher) {
        `when`(userPreferences.setLastBackupCreatedAt(1000L))
            .thenAnswer { throw IOException("disk full") }
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.onDelivered(request(isFullBackup = true), now = 1000L)

        assertEquals(emptyList<Int>(), errors)
    }

    @Test
    fun `creating without delivering records nothing`() = runTest(dispatcher) {
        `when`(fullBackupManager.exportFullBackup()).thenReturn(backupFile)
        val viewModel = viewModel()

        viewModel.createBackup()
        assertEquals(true, viewModel.exportRequest.value?.isFullBackup)
        assertEquals(backupFile.name, viewModel.exportRequest.value?.fileName)
        viewModel.clearExportRequest()

        assertNull(viewModel.exportRequest.value)
        verify(userPreferences).lastBackupCreatedAt
        org.mockito.Mockito.verifyNoMoreInteractions(userPreferences)
    }

    @Test
    fun `failed backup emits an error and no request`() = runTest(dispatcher) {
        `when`(fullBackupManager.exportFullBackup()).thenAnswer { throw IOException("disk full") }
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.createBackup()

        assertEquals(listOf(R.string.export_failed), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `failed sharing emits an error and no request`() = runTest(dispatcher) {
        `when`(fullBackupManager.exportFullBackup()).thenReturn(backupFile)
        val viewModel = viewModel(fileSharer = { throw IllegalArgumentException("no provider root") })
        val errors = collect(viewModel.errors)

        viewModel.createBackup()

        assertEquals(listOf(R.string.export_failed), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `cancelled backup is not reported as an error`() = runTest(dispatcher) {
        `when`(fullBackupManager.exportFullBackup()).thenAnswer { throw CancellationException() }
        val viewModel = viewModel()
        val errors = collect(viewModel.errors)

        viewModel.createBackup()

        assertEquals(emptyList<Int>(), errors)
        assertNull(viewModel.exportRequest.value)
    }

    @Test
    fun `restore result is kept until acknowledged`() = runTest(dispatcher) {
        val expected = FullBackupRestoreResult(isSuccess = true, transactionCount = 3)
        `when`(contentResolver.openInputStream(uri)).thenReturn("{json}".byteInputStream())
        `when`(fullBackupManager.importFullBackup("{json}")).thenReturn(expected)
        val viewModel = viewModel()

        viewModel.restore(uri)

        assertEquals(RestoreState.Finished(expected), viewModel.restoreState.value)
        assertEquals(false, viewModel.isBusy.value)
        viewModel.acknowledgeRestoreResult()
        assertEquals(RestoreState.Idle, viewModel.restoreState.value)
    }

    @Test
    fun `backup cannot start while a restore runs`() = runTest(dispatcher) {
        `when`(contentResolver.openInputStream(uri)).thenReturn("{json}".byteInputStream())
        `when`(fullBackupManager.importFullBackup("{json}"))
            .thenReturn(FullBackupRestoreResult(isSuccess = true))
        // The restore's scope is not advanced before the checks, so the restore is still running.
        val pausedRunner = BackupRestoreRunner(
            context, fullBackupManager, CoroutineScope(StandardTestDispatcher(testScheduler))
        )
        val viewModel = viewModel(restoreRunner = pausedRunner)

        viewModel.restore(uri)
        viewModel.createBackup()

        assertEquals(true, viewModel.isBusy.value)
        assertNull(viewModel.exportRequest.value)
        verify(fullBackupManager, never()).exportFullBackup()
    }
}
