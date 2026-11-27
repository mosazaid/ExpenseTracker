package com.mousa.mizani.presentation

import com.mousa.mizani.R
import com.mousa.mizani.core.export.FullBackupRestoreResult
import com.mousa.mizani.presentation.feature.settings.backup.restoreNoteRes
import com.mousa.mizani.presentation.feature.settings.backup.restoredMessageRes
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreMessagesTest {

    @Test
    fun `restored settings use the all preferences restored text`() {
        val result = FullBackupRestoreResult(isSuccess = true, preferencesRestored = true)

        assertEquals(R.string.full_backup_success, restoredMessageRes(result))
    }

    @Test
    fun `failed settings say they were not restored`() {
        val result = FullBackupRestoreResult(isSuccess = true, settingsFailed = true)

        assertEquals(R.string.full_backup_settings_failed, restoredMessageRes(result))
    }

    @Test
    fun `a file without settings does not claim settings were restored`() {
        val result = FullBackupRestoreResult(isSuccess = true)

        assertEquals(R.string.full_backup_restored_records_only, restoredMessageRes(result))
    }

    @Test
    fun `a skipped app lock does not claim all preferences were restored and says why`() {
        val result = FullBackupRestoreResult(isSuccess = true, preferencesRestored = true, appLockSkipped = true)

        assertEquals(R.string.full_backup_success_except_app_lock, restoredMessageRes(result))
        assertEquals(listOf(R.string.app_lock_restore_skipped), restoreNoteRes(result))
    }

    @Test
    fun `a lock turned off by the restore is mentioned`() {
        val result = FullBackupRestoreResult(isSuccess = true, preferencesRestored = true, appLockTurnedOff = true)

        assertEquals(R.string.full_backup_success, restoredMessageRes(result))
        assertEquals(listOf(R.string.app_lock_restore_turned_off), restoreNoteRes(result))
    }

    @Test
    fun `a plain restore adds no app lock note`() {
        val result = FullBackupRestoreResult(isSuccess = true, preferencesRestored = true)

        assertEquals(emptyList<Int>(), restoreNoteRes(result))
    }

    @Test
    fun `failed settings add no app lock note`() {
        val result = FullBackupRestoreResult(
            isSuccess = true, settingsFailed = true, appLockSkipped = true, appLockTurnedOff = true
        )

        assertEquals(R.string.full_backup_settings_failed, restoredMessageRes(result))
        assertEquals(emptyList<Int>(), restoreNoteRes(result))
    }
}
