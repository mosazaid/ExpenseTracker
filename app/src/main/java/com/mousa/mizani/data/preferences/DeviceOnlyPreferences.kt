package com.mousa.mizani.data.preferences

/** Preferences that describe this device only, so they are never written to a backup file. */
object DeviceOnlyPreferences {
    const val LAST_BACKUP_CREATED_AT = "last_backup_created_at"
    const val DATABASE_VIEWER_UNLOCKED = "database_viewer_unlocked"

    val KEYS: Set<String> = setOf(LAST_BACKUP_CREATED_AT, DATABASE_VIEWER_UNLOCKED)

    fun withoutDeviceOnly(all: Map<String, String>): Map<String, String> =
        all.filterKeys { it !in KEYS }
}
