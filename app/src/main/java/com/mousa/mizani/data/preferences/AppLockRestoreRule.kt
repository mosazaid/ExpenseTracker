package com.mousa.mizani.data.preferences

/** Keeps a restored backup from locking the user out of a phone that cannot unlock the app. */
object AppLockRestoreRule {

    const val APP_LOCK_KEY = "biometric_lock_enabled"

    data class Decision(
        val settings: Map<String, String>,
        /** The backup turns the lock on, but this phone cannot unlock it, so it was left out. */
        val isLockSkipped: Boolean,
        /** The backup turns off a lock that is on now. */
        val turnsLockOff: Boolean
    )

    /**
     * The settings to restore from a backup. When this phone cannot unlock the app, a backup that
     * turns the lock on leaves the lock as it is on this phone; every other setting is kept.
     */
    fun settingsToRestore(
        backup: Map<String, String>,
        isLockAvailable: Boolean,
        isLockOnNow: Boolean
    ): Decision {
        val backupLock = backup[APP_LOCK_KEY]?.toBooleanStrictOrNull()
        val isLockSkipped = backupLock == true && !isLockAvailable
        return Decision(
            settings = if (isLockSkipped) backup - APP_LOCK_KEY else backup,
            isLockSkipped = isLockSkipped,
            turnsLockOff = backupLock == false && isLockOnNow
        )
    }
}
