package com.mousa.mizani.core.debug

const val VERSION_TAPS_REQUIRED = 7

/**
 * Counts taps on the app version row. Returns true on every [VERSION_TAPS_REQUIRED]th tap
 * and restarts the count afterwards.
 */
class VersionTapCounter {
    private var taps = 0

    fun tap(): Boolean {
        taps++
        if (taps < VERSION_TAPS_REQUIRED) return false
        taps = 0
        return true
    }
}

/**
 * The database viewer is always available in debug builds. Release builds show it only
 * after the user has unlocked it.
 */
fun isDatabaseViewerVisible(isDebugBuild: Boolean, unlocked: Boolean): Boolean =
    isDebugBuild || unlocked
