package com.mousa.mizani.presentation.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.R
import com.mousa.mizani.core.debug.VersionTapCounter
import com.mousa.mizani.core.locale.AppLanguage
import com.mousa.mizani.core.locale.LocaleHelper
import com.mousa.mizani.core.security.AppLockAvailability
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.ThemeMode
import com.mousa.mizani.data.preferences.ThemePalette
import com.mousa.mizani.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "SettingsIndex"

/** The Settings list: row summaries, the app lock switch and the hidden database viewer. */
@HiltViewModel
class SettingsIndexViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val appLockAvailability: AppLockAvailability
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode.asState(ThemeMode.SYSTEM)
    val themePalette: StateFlow<ThemePalette> = userPreferences.themePalette.asState(ThemePalette.EMERALD)

    // The default locale is the chosen language, or the phone's when nothing was chosen.
    val language: StateFlow<AppLanguage> = userPreferences.language
        .asState(LocaleHelper.shownLanguage(null, Locale.getDefault().language))

    /** Null until read, so the row shows no made-up currency. */
    val currencyCode: StateFlow<String?> = userPreferences.currencyCode.asState(null)
    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode.asState(MonthMode.CALENDAR)
    val lastBackupCreatedAt: StateFlow<Long?> = userPreferences.lastBackupCreatedAt.asState(null)
    val appLockEnabled: StateFlow<Boolean> = userPreferences.biometricLockEnabled.asState(false)
    val databaseViewerUnlocked: StateFlow<Boolean> = userPreferences.databaseViewerUnlocked.asState(false)

    private val _messages = MutableSharedFlow<Int>()
    /** String resources for short messages to show to the user. */
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    // Only touched from the main thread (tap handlers).
    private val versionTapCounter = VersionTapCounter()

    /** Turning the lock on needs a fingerprint or screen lock; turning it off is always allowed. */
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !appLockAvailability.isAvailable()) {
                _messages.emit(R.string.app_lock_unavailable)
                return@launch
            }
            save("Could not change the app lock") { userPreferences.setBiometricLockEnabled(enabled) }
        }
    }

    /** Debug builds always show the database viewer, so taps there change nothing. */
    fun onVersionTapped(isDebugBuild: Boolean) {
        if (isDebugBuild || !versionTapCounter.tap()) return
        viewModelScope.launch {
            save("Could not change the database viewer") {
                val isUnlocked = !userPreferences.databaseViewerUnlocked.first()
                userPreferences.setDatabaseViewerUnlocked(isUnlocked)
                _messages.emit(if (isUnlocked) R.string.database_viewer_shown else R.string.database_viewer_hidden)
            }
        }
    }

    private suspend fun save(failureLog: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, failureLog, e)
            _messages.emit(R.string.settings_save_failed)
        }
    }

    private fun <T> Flow<T>.asState(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)
}
