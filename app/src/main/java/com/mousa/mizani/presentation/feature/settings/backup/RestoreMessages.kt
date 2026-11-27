package com.mousa.mizani.presentation.feature.settings.backup

import androidx.annotation.StringRes
import com.mousa.mizani.R
import com.mousa.mizani.core.export.FullBackupRestoreResult

/**
 * The message for a successful restore. Only a restore whose settings were all imported may say
 * so; the other texts say the settings failed, that App lock was left out, or that the file had none.
 */
@StringRes
fun restoredMessageRes(result: FullBackupRestoreResult): Int = when {
    result.settingsFailed -> R.string.full_backup_settings_failed
    result.preferencesRestored && result.appLockSkipped -> R.string.full_backup_success_except_app_lock
    result.preferencesRestored -> R.string.full_backup_success
    else -> R.string.full_backup_restored_records_only
}

/** Extra sentences about App lock, shown after the main message. Only when the settings were imported. */
fun restoreNoteRes(result: FullBackupRestoreResult): List<Int> {
    if (!result.preferencesRestored) return emptyList()
    return buildList {
        if (result.appLockSkipped) add(R.string.app_lock_restore_skipped)
        if (result.appLockTurnedOff) add(R.string.app_lock_restore_turned_off)
    }
}
