package com.mousa.mizani.presentation

import com.mousa.mizani.core.security.BiometricAuthManager
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.presentation.feature.settings.BiometricLockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricLockViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val stored = MutableSharedFlow<Boolean>()
    private val userPreferences = mock(UserPreferences::class.java)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        `when`(userPreferences.biometricLockEnabled).thenReturn(stored)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the lock state is unknown until the preference is read`() = runTest(dispatcher) {
        val viewModel = BiometricLockViewModel(mock(BiometricAuthManager::class.java), userPreferences)
        assertNull(viewModel.biometricLockEnabled.value)

        backgroundScope.launch { viewModel.biometricLockEnabled.collect {} }
        assertNull(viewModel.biometricLockEnabled.value)

        stored.emit(true)
        assertEquals(true, viewModel.biometricLockEnabled.value)
    }
}
