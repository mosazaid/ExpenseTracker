package com.mousa.mizani.core

import com.mousa.mizani.core.debug.VERSION_TAPS_REQUIRED
import com.mousa.mizani.core.debug.VersionTapCounter
import com.mousa.mizani.core.debug.isDatabaseViewerVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseViewerAccessTest {

    @Test
    fun `six taps do nothing`() {
        val counter = VersionTapCounter()
        val results = List(VERSION_TAPS_REQUIRED - 1) { counter.tap() }
        assertTrue(results.none { it })
    }

    @Test
    fun `seventh tap fires`() {
        val counter = VersionTapCounter()
        repeat(VERSION_TAPS_REQUIRED - 1) { counter.tap() }
        assertTrue(counter.tap())
    }

    @Test
    fun `fourteenth tap fires again`() {
        val counter = VersionTapCounter()
        repeat(VERSION_TAPS_REQUIRED) { counter.tap() }
        repeat(VERSION_TAPS_REQUIRED - 1) { assertFalse(counter.tap()) }
        assertTrue(counter.tap())
    }

    @Test
    fun `debug build always shows the viewer`() {
        assertTrue(isDatabaseViewerVisible(isDebugBuild = true, unlocked = false))
        assertTrue(isDatabaseViewerVisible(isDebugBuild = true, unlocked = true))
    }

    @Test
    fun `release shows it only when unlocked`() {
        assertFalse(isDatabaseViewerVisible(isDebugBuild = false, unlocked = false))
        assertTrue(isDatabaseViewerVisible(isDebugBuild = false, unlocked = true))
    }
}
