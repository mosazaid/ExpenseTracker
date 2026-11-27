package com.mousa.mizani.presentation.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.core.locale.AppLanguage
import com.mousa.mizani.core.locale.LocaleHelper
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** Appearance, language and region, and month settings. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    fun setMonthMode(mode: MonthMode) {
        viewModelScope.launch {
            userPreferences.setMonthMode(mode)
        }
    }

    // The default locale is the chosen language, or the phone's when nothing was chosen.
    val language: StateFlow<AppLanguage> = userPreferences.language
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LocaleHelper.shownLanguage(null, Locale.getDefault().language)
        )

    val themeMode: StateFlow<com.mousa.mizani.data.preferences.ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.mousa.mizani.data.preferences.ThemeMode.SYSTEM)

    val themePalette: StateFlow<com.mousa.mizani.data.preferences.ThemePalette> = userPreferences.themePalette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.mousa.mizani.data.preferences.ThemePalette.EMERALD)

    fun setThemePalette(palette: com.mousa.mizani.data.preferences.ThemePalette) {
        viewModelScope.launch {
            userPreferences.setThemePalette(palette)
        }
    }

    val timeFormat: StateFlow<com.mousa.mizani.data.preferences.TimeFormatPreference> = userPreferences.timeFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.mousa.mizani.data.preferences.TimeFormatPreference.H12)

    fun setTimeFormat(format: com.mousa.mizani.data.preferences.TimeFormatPreference) {
        viewModelScope.launch {
            userPreferences.setTimeFormat(format)
        }
    }

    val currencyCode: StateFlow<String> = userPreferences.currencyCode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.mousa.mizani.core.format.CurrencyUtils.currencyWhenUnset(LocaleHelper.savedLanguage(context))
        )

    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            userPreferences.setCurrencyCode(code)
        }
    }

    val enableDepositWalletTransfer: StateFlow<Boolean> = userPreferences.enableDepositWalletTransfer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setEnableDepositWalletTransfer(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setEnableDepositWalletTransfer(enabled)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferences.setLanguage(language)
        }
    }

    fun setThemeMode(mode: com.mousa.mizani.data.preferences.ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }
}
