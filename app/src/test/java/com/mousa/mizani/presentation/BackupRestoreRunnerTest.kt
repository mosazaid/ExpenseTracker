package com.mousa.mizani.presentation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.mousa.mizani.core.export.FullBackupManager
import com.mousa.mizani.core.export.FullBackupRestoreResult
import com.mousa.mizani.presentation.feature.settings.backup.BackupRestoreRunner
import com.mousa.mizani.presentation.feature.settings.backup.RestoreState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreRunnerTest {

    private val context = mock(Context::class.java)
    private val contentResolver = mock(ContentResolver::class.java)
    private val fullBackupManager = mock(FullBackupManager::class.java)
    private val uri = mock(Uri::class.java)

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

    @Before
    fun setUp() {
        log = mockStatic(Log::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
    }

    @After
    fun tearDown() {
        log.close()
    }

    private fun runner(scope: CoroutineScope) = BackupRestoreRunner(context, fullBackupManager, scope)

    @Test
    fun `restore passes the file content on and keeps the result`() = runTest {
        val expected = FullBackupRestoreResult(isSuccess = true, transactionCount = 3)
        `when`(contentResolver.openInputStream(uri)).thenReturn("{json}".byteInputStream())
        `when`(fullBackupManager.importFullBackup("{json}")).thenReturn(expected)
        val runner = runner(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        assertTrue(runner.start(uri))

        assertEquals(RestoreState.Finished(expected), runner.state.value)
    }

    @Test
    fun `acknowledging the result returns to idle`() = runTest {
        `when`(contentResolver.openInputStream(uri)).thenReturn("{json}".byteInputStream())
        `when`(fullBackupManager.importFullBackup("{json}"))
            .thenReturn(FullBackupRestoreResult(isSuccess = true))
        val runner = runner(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        runner.start(uri)

        runner.acknowledge()

        assertEquals(RestoreState.Idle, runner.state.value)
    }

    @Test
    fun `a second restore cannot start while one runs`() = runTest {
        `when`(contentResolver.openInputStream(uri)).thenReturn("{json}".byteInputStream())
        `when`(fullBackupManager.importFullBackup("{json}"))
            .thenReturn(FullBackupRestoreResult(isSuccess = true))
        val runner = runner(CoroutineScope(StandardTestDispatcher(testScheduler)))

        assertTrue(runner.start(uri))
        assertEquals(RestoreState.Running, runner.state.value)
        assertFalse(runner.start(uri))
        testScheduler.advanceUntilIdle()

        verify(fullBackupManager, times(1)).importFullBackup("{json}")
        assertEquals(RestoreState.Finished(FullBackupRestoreResult(isSuccess = true)), runner.state.value)
    }

    @Test
    fun `unreadable backup file gives a failed result and restores nothing`() = runTest {
        `when`(contentResolver.openInputStream(uri)).thenAnswer { throw IOException("gone") }
        val runner = runner(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        runner.start(uri)

        assertEquals(
            RestoreState.Finished(FullBackupRestoreResult(isSuccess = false, errorMessage = "gone")),
            runner.state.value
        )
        verifyNoInteractions(fullBackupManager)
    }

    @Test
    fun `missing input stream gives a failed result and restores nothing`() = runTest {
        `when`(contentResolver.openInputStream(uri)).thenReturn(null)
        val runner = runner(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        runner.start(uri)

        assertEquals(
            RestoreState.Finished(FullBackupRestoreResult(isSuccess = false, errorMessage = "Cannot read file")),
            runner.state.value
        )
        verifyNoInteractions(fullBackupManager)
    }

    @Test
    fun `cancelled restore leaves no result and can be started again`() = runTest {
        `when`(contentResolver.openInputStream(uri)).thenReturn("{json}".byteInputStream())
        `when`(fullBackupManager.importFullBackup("{json}"))
            .thenAnswer { throw CancellationException() }
        val runner = runner(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        runner.start(uri)

        assertEquals(RestoreState.Idle, runner.state.value)
    }
}
