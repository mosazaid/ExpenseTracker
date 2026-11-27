package com.mousa.mizani.presentation

import android.content.Context
import android.content.SharedPreferences
import com.mousa.mizani.core.format.CurrencyUtils
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.presentation.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.stubbing.Answer

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelCurrencyTest {

    private val context = mock(Context::class.java)
    private val sharedPreferences = mock(SharedPreferences::class.java)
    private lateinit var originalCurrency: String

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        originalCurrency = CurrencyUtils.activeCurrencyCode
        CurrencyUtils.activeCurrencyCode = "KWD"
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
    }

    @After
    fun tearDown() {
        CurrencyUtils.activeCurrencyCode = originalCurrency
        Dispatchers.resetMain()
    }

    private inline fun <reified T : Any> silentFlows(): T = mock(T::class.java, Answer { call ->
        if (Flow::class.java.isAssignableFrom(call.method.returnType)) emptyFlow<Any>()
        else RETURNS_DEFAULTS.answer(call)
    })

    private fun viewModel() = SettingsViewModel(context, silentFlows<UserPreferences>())

    @Test
    fun `the initial currency matches the unset value for no saved language`() {
        `when`(sharedPreferences.getString(anyString(), isNull())).thenReturn(null)

        assertEquals(CurrencyUtils.currencyWhenUnset(null), viewModel().currencyCode.value)
    }

    @Test
    fun `the initial currency matches the unset value for saved Arabic`() {
        `when`(sharedPreferences.getString(anyString(), isNull())).thenReturn("ar")

        assertEquals(CurrencyUtils.currencyWhenUnset("ar"), viewModel().currencyCode.value)
    }
}
