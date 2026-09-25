package com.example.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.expensetracker.core.locale.AppLanguage
import com.example.expensetracker.core.locale.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class MonthMode {
    CALENDAR,
    SALARY
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val biometricLockKey = booleanPreferencesKey("biometric_lock_enabled")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val themePaletteKey = stringPreferencesKey("theme_palette")
    private val languageKey = stringPreferencesKey("app_language")
    private val monthModeKey = stringPreferencesKey("month_mode")
    private val openingCashKey = stringPreferencesKey("opening_cash_balance")
    private val openingBankKey = stringPreferencesKey("opening_bank_balance")
    private val dismissedRecurringKey = stringSetPreferencesKey("dismissed_recurring_until")
    private val lastLoanSheetDateKey = stringPreferencesKey("last_loan_sheet_date")
    private val lastDebtRolloverMonthKey = stringPreferencesKey("last_debt_rollover_month")

    val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[biometricLockKey] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeModeKey]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val themePalette: Flow<ThemePalette> = context.dataStore.data.map { prefs ->
        try {
            ThemePalette.valueOf(prefs[themePaletteKey] ?: ThemePalette.EMERALD.name)
        } catch (_: Exception) {
            ThemePalette.EMERALD
        }
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[languageKey])
    }

    val monthMode: Flow<MonthMode> = context.dataStore.data.map { prefs ->
        when (prefs[monthModeKey]) {
            MonthMode.SALARY.name -> MonthMode.SALARY
            else -> MonthMode.CALENDAR
        }
    }

    val openingCashBalance: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[openingCashKey]?.toDoubleOrNull() ?: 0.0
    }

    val openingBankBalance: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[openingBankKey]?.toDoubleOrNull() ?: 0.0
    }

    val dismissedRecurringKeys: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[dismissedRecurringKey] ?: emptySet()
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[biometricLockKey] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        context.dataStore.edit { prefs ->
            prefs[themePaletteKey] = palette.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        LocaleHelper.persistLanguage(context, language.code)
        context.dataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }

    suspend fun setMonthMode(mode: MonthMode) {
        context.dataStore.edit { prefs ->
            prefs[monthModeKey] = mode.name
        }
    }

    suspend fun setOpeningCashBalance(amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[openingCashKey] = amount.toString()
        }
    }

    suspend fun setOpeningBankBalance(amount: Double) {
        context.dataStore.edit { prefs ->
            prefs[openingBankKey] = amount.toString()
        }
    }

    suspend fun getOpeningCashBalance(): Double {
        return openingCashBalance.first()
    }

    suspend fun getOpeningBankBalance(): Double {
        return openingBankBalance.first()
    }

    suspend fun dismissRecurringBanner(recurringId: Long, untilMillis: Long) {
        val key = recurringDismissKey(recurringId, untilMillis)
        context.dataStore.edit { prefs ->
            val current = prefs[dismissedRecurringKey] ?: emptySet()
            prefs[dismissedRecurringKey] = current + key
        }
    }

    suspend fun clearDismissedRecurring(recurringId: Long) {
        context.dataStore.edit { prefs ->
            val current = prefs[dismissedRecurringKey] ?: emptySet()
            prefs[dismissedRecurringKey] = current.filterNot { it.startsWith("$recurringId:") }.toSet()
        }
    }

    suspend fun isRecurringDismissed(recurringId: Long, dueMillis: Long): Boolean {
        val key = recurringDismissKey(recurringId, dueMillis)
        return dismissedRecurringKeys.first().contains(key)
    }

    val lastLoanSheetDate: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[lastLoanSheetDateKey]
    }

    suspend fun setLastLoanSheetDate(dateString: String) {
        context.dataStore.edit { prefs ->
            prefs[lastLoanSheetDateKey] = dateString
        }
    }

    suspend fun getLastLoanSheetDate(): String? = lastLoanSheetDate.first()

    val lastDebtRolloverMonth: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[lastDebtRolloverMonthKey]
    }

    suspend fun setLastDebtRolloverMonth(monthString: String) {
        context.dataStore.edit { prefs ->
            prefs[lastDebtRolloverMonthKey] = monthString
        }
    }

    suspend fun getLastDebtRolloverMonth(): String? = lastDebtRolloverMonth.first()

    private fun recurringDismissKey(recurringId: Long, dueMillis: Long): String =
        "$recurringId:$dueMillis"
}
