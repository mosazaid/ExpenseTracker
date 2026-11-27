package com.mousa.mizani.data

import com.mousa.mizani.data.preferences.DeviceOnlyPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceOnlyPreferencesTest {

    @Test
    fun `backup map excludes device-only keys and keeps others`() {
        val all = mapOf(
            "last_backup_created_at" to "1000",
            "database_viewer_unlocked" to "true",
            "theme_mode" to "DARK"
        )

        val result = DeviceOnlyPreferences.withoutDeviceOnly(all)

        assertEquals(setOf("theme_mode"), result.keys)
        assertEquals("DARK", result["theme_mode"])
    }
}
