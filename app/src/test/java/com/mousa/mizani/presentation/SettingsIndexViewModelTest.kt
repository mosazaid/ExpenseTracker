package com.mousa.mizani.presentation

import com.mousa.mizani.R
import com.mousa.mizani.core.debug.VERSION_TAPS_REQUIRED
import com.mousa.mizani.core.security.AppLockAvailability
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.presentation.feature.settings.SettingsIndexViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsIndexViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val userPreferences = mock(UserPreferences::class.java, Answer { call ->
        if (Flow::class.java.isAssignableFrom(call.method.returnType)) emptyFlow<Any>()
        else RETURNS_DEFAULTS.answer(call)
    })

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(isLockAvailable: Boolean = true) =
        SettingsIndexViewModel(userPreferences, AppLockAvailability { isLockAvailable })

    private fun <T> TestScope.collect(flow: SharedFlow<T>): List<T> {
        val values = mutableListOf<T>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            flow.collect { values.add(it) }
        }
        return values
    }

    @Test
    fun `app lock stays off and explains why when this phone cannot unlock it`() = runTest(dispatcher) {
        val viewModel = viewModel(isLockAvailable = false)
        val messages = collect(viewModel.messages)

        viewModel.setAppLockEnabled(true)

        verify(userPreferences, never()).setBiometricLockEnabled(anyBoolean())
        assertEquals(listOf(R.string.app_lock_unavailable), messages)
    }

    @Test
    fun `app lock turns on when this phone can unlock it`() = runTest(dispatcher) {
        val viewModel = viewModel(isLockAvailable = true)
        val messages = collect(viewModel.messages)

        viewModel.setAppLockEnabled(true)

        verify(userPreferences).setBiometricLockEnabled(true)
        assertEquals(emptyList<Int>(), messages)
    }

    @Test
    fun `app lock can always be turned off`() = runTest(dispatcher) {
        viewModel(isLockAvailable = false).setAppLockEnabled(false)

        verify(userPreferences).setBiometricLockEnabled(false)
    }

    @Test
    fun `enough version taps in a release build show the database viewer`() = runTest(dispatcher) {
        `when`(userPreferences.databaseViewerUnlocked).thenReturn(flowOf(false))
        val viewModel = viewModel()
        val messages = collect(viewModel.messages)

        repeat(VERSION_TAPS_REQUIRED - 1) { viewModel.onVersionTapped(isDebugBuild = false) }
        verify(userPreferences, never()).setDatabaseViewerUnlocked(anyBoolean())
        viewModel.onVersionTapped(isDebugBuild = false)

        verify(userPreferences).setDatabaseViewerUnlocked(true)
        assertEquals(listOf(R.string.database_viewer_shown), messages)
    }

    @Test
    fun `enough version taps hide a shown database viewer`() = runTest(dispatcher) {
        `when`(userPreferences.databaseViewerUnlocked).thenReturn(flowOf(true))
        val viewModel = viewModel()
        val messages = collect(viewModel.messages)

        repeat(VERSION_TAPS_REQUIRED) { viewModel.onVersionTapped(isDebugBuild = false) }

        verify(userPreferences).setDatabaseViewerUnlocked(false)
        assertEquals(listOf(R.string.database_viewer_hidden), messages)
    }

    @Test
    fun `version taps change nothing in a debug build`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val messages = collect(viewModel.messages)

        repeat(VERSION_TAPS_REQUIRED) { viewModel.onVersionTapped(isDebugBuild = true) }

        verify(userPreferences, never()).setDatabaseViewerUnlocked(anyBoolean())
        assertEquals(emptyList<Int>(), messages)
    }
}
