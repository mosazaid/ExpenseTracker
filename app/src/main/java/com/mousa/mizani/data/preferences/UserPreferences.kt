package com.mousa.mizani.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mousa.mizani.core.locale.AppLanguage
import com.mousa.mizani.core.locale.LocaleHelper
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

enum class TimeFormatPreference {
    H12,
    H24
}

/**
 * User-configurable rules controlling which transactions and balances are included
 * in Income, Expense, and Smart Financial Insights calculations.
 * Defaults preserve standard Mizani accounting behavior.
 */
data class CalculationPreferences(
    // Section 1: Income Calculations
    val includeDepositIncome: Boolean = false,
    val deductLoansFromIncome: Boolean = true,
    // Section 2: Expense Calculations
    val includeWalletExpenses: Boolean = false,
    val includeDepositExpenses: Boolean = false,
    val includeLoanLinkedExpenses: Boolean = false,
    // Section 3: Smart Financial Insights
    val includeCarriedForwardInInsights: Boolean = true,
    val useAvailableBalanceFloorInInsights: Boolean = true,
    val reserveWalletInInsights: Boolean = false,
    // Section 4: Statistics Calculations
    val statsApplyGlobalIncomeExpenseRules: Boolean = true,
    val statsIncludeManuallyExcluded: Boolean = false,
    // Section 5: Other Financial Tools (Runway & Zakah)
    val runwayIncludeVaultBalance: Boolean = true,
    val runwayExcludeWalletAndDepositBurn: Boolean = false,
    val zakahIncludeVaultOnAutoSync: Boolean = true
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val biometricLockKey = booleanPreferencesKey(AppLockRestoreRule.APP_LOCK_KEY)
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val themePaletteKey = stringPreferencesKey("theme_palette")
    private val languageKey = stringPreferencesKey("app_language")
    private val timeFormatKey = stringPreferencesKey("app_time_format")
    private val monthModeKey = stringPreferencesKey("month_mode")
    private val openingCashKey = stringPreferencesKey(RestoredBalances.OPENING_CASH_KEY)
    private val openingBankKey = stringPreferencesKey(RestoredBalances.OPENING_BANK_KEY)
    private val dismissedRecurringKey = stringSetPreferencesKey("dismissed_recurring_until")
    private val lastLoanSheetDateKey = stringPreferencesKey("last_loan_sheet_date")
    private val lastDebtRolloverMonthKey = stringPreferencesKey("last_debt_rollover_month")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val loanAlertsKey = booleanPreferencesKey("alert_loan_enabled")
    private val debtAlertsKey = booleanPreferencesKey("alert_debt_enabled")
    private val salaryAlertsKey = booleanPreferencesKey("alert_salary_enabled")
    private val budgetAlertsKey = booleanPreferencesKey("alert_budget_enabled")
    private val dailyReminderKey = booleanPreferencesKey("alert_daily_reminder_enabled")
    private val currencyCodeKey = stringPreferencesKey("app_currency_code")
    private val prayerCalculationMethodKey = stringPreferencesKey("prayer_calculation_method")
    private val prayerAlertsEnabledKey = booleanPreferencesKey("prayer_alerts_enabled")
    private val fajrAlertKey = booleanPreferencesKey("alert_fajr_enabled")
    private val dhuhrAlertKey = booleanPreferencesKey("alert_dhuhr_enabled")
    private val asrAlertKey = booleanPreferencesKey("alert_asr_enabled")
    private val maghribAlertKey = booleanPreferencesKey("alert_maghrib_enabled")
    private val ishaAlertKey = booleanPreferencesKey("alert_isha_enabled")
    private val prayerSavedCityEnKey = stringPreferencesKey("prayer_saved_city_en")
    private val prayerSavedCityArKey = stringPreferencesKey("prayer_saved_city_ar")
    private val prayerSavedLatKey = stringPreferencesKey("prayer_saved_lat")
    private val prayerSavedLngKey = stringPreferencesKey("prayer_saved_lng")
    private val prayerSavedTzKey = stringPreferencesKey("prayer_saved_tz")
    private val prayerSavedElevationKey = stringPreferencesKey("prayer_saved_elevation")
    private val prayerAdhanSoundKey = stringPreferencesKey("prayer_adhan_sound")
    private val enableDepositWalletTransferKey = booleanPreferencesKey("enable_deposit_wallet_transfer")
    private val sadaqahRoundUpEnabledKey = booleanPreferencesKey("sadaqah_roundup_enabled")
    private val sadaqahJarBalanceKey = stringPreferencesKey(RestoredBalances.SADAQAH_JAR_KEY)
    private val sadaqahFridayReminderEnabledKey = booleanPreferencesKey("sadaqah_friday_reminder_enabled")
    private val proTrialStartTimestampKey = longPreferencesKey("pro_trial_start_timestamp")
    private val isProSubscriberKey = booleanPreferencesKey("is_pro_subscriber")
    private val isOwnerBypassKey = booleanPreferencesKey("is_owner_bypass")
    private val userPlanTierKey = stringPreferencesKey("user_plan_tier")
    private val stickyPrayerAlertsKey = booleanPreferencesKey("sticky_prayer_alerts_enabled")
    private val stickyDailyReminderKey = booleanPreferencesKey("sticky_daily_reminder_enabled")
    private val stickyBudgetAlertsKey = booleanPreferencesKey("sticky_budget_alerts_enabled")
    private val stickyLoanAlertsKey = booleanPreferencesKey("sticky_loan_alerts_enabled")
    private val stickySalaryAlertsKey = booleanPreferencesKey("sticky_salary_alerts_enabled")
    private val databaseViewerUnlockedKey =
        booleanPreferencesKey(DeviceOnlyPreferences.DATABASE_VIEWER_UNLOCKED)
    private val lastBackupCreatedAtKey =
        longPreferencesKey(DeviceOnlyPreferences.LAST_BACKUP_CREATED_AT)

    // Calculation Rules keys
    private val calcIncludeDepositIncomeKey = booleanPreferencesKey("calc_include_deposit_income")
    private val calcDeductLoansFromIncomeKey = booleanPreferencesKey("calc_deduct_loans_from_income")
    private val calcIncludeWalletExpensesKey = booleanPreferencesKey("calc_include_wallet_expenses")
    private val calcIncludeDepositExpensesKey = booleanPreferencesKey("calc_include_deposit_expenses")
    private val calcIncludeLoanLinkedExpensesKey = booleanPreferencesKey("calc_include_loan_linked_expenses")
    private val calcIncludeCarriedForwardInsightsKey = booleanPreferencesKey("calc_include_carried_forward_insights")
    private val calcUseAvailableFloorInsightsKey = booleanPreferencesKey("calc_use_available_floor_insights")
    private val calcReserveWalletInsightsKey = booleanPreferencesKey("calc_reserve_wallet_insights")
    private val calcStatsApplyGlobalRulesKey = booleanPreferencesKey("calc_stats_apply_global_rules")
    private val calcStatsIncludeManuallyExcludedKey = booleanPreferencesKey("calc_stats_include_manually_excluded")
    private val calcRunwayIncludeVaultKey = booleanPreferencesKey("calc_runway_include_vault")
    private val calcRunwayExcludeWalletDepositKey = booleanPreferencesKey("calc_runway_exclude_wallet_deposit")
    private val calcZakahIncludeVaultKey = booleanPreferencesKey("calc_zakah_include_vault")

    val stickyPrayerAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[stickyPrayerAlertsKey] ?: false
    }

    suspend fun setStickyPrayerAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[stickyPrayerAlertsKey] = enabled }
    }

    val stickyDailyReminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[stickyDailyReminderKey] ?: false
    }

    suspend fun setStickyDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[stickyDailyReminderKey] = enabled }
    }

    val stickyBudgetAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[stickyBudgetAlertsKey] ?: false
    }

    suspend fun setStickyBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[stickyBudgetAlertsKey] = enabled }
    }

    val stickyLoanAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[stickyLoanAlertsKey] ?: false
    }

    suspend fun setStickyLoanAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[stickyLoanAlertsKey] = enabled }
    }

    val stickySalaryAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[stickySalaryAlertsKey] ?: false
    }

    suspend fun setStickySalaryAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[stickySalaryAlertsKey] = enabled }
    }

    val databaseViewerUnlocked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[databaseViewerUnlockedKey] ?: false
    }

    suspend fun setDatabaseViewerUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs -> prefs[databaseViewerUnlockedKey] = unlocked }
    }

    /** When a full backup was last saved or shared from this device; null if never. */
    val lastBackupCreatedAt: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[lastBackupCreatedAtKey]
    }

    suspend fun setLastBackupCreatedAt(millis: Long) {
        context.dataStore.edit { prefs -> prefs[lastBackupCreatedAtKey] = millis }
    }

    val isOwnerBypass: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[isOwnerBypassKey] ?: false
    }

    suspend fun setIsOwnerBypass(bypass: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[isOwnerBypassKey] = bypass
        }
    }

    val proTrialStartTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[proTrialStartTimestampKey] ?: 0L
    }

    suspend fun setProTrialStartTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[proTrialStartTimestampKey] = timestamp
        }
    }

    val isProSubscriber: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[isProSubscriberKey] ?: false
    }

    suspend fun setIsProSubscriber(isPro: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[isProSubscriberKey] = isPro
        }
    }

    val userPlanTier: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[userPlanTierKey] ?: "FREE"
    }

    suspend fun setUserPlanTier(tier: String) {
        context.dataStore.edit { prefs ->
            prefs[userPlanTierKey] = tier
        }
    }

    val sadaqahRoundUpEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[sadaqahRoundUpEnabledKey] ?: false
    }

    suspend fun setSadaqahRoundUpEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[sadaqahRoundUpEnabledKey] = enabled
        }
    }

    val sadaqahJarBalance: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[sadaqahJarBalanceKey]?.toDoubleOrNull() ?: 0.0
    }

    suspend fun addToSadaqahJar(spareChange: Double) {
        if (spareChange <= 0.0) return
        context.dataStore.edit { prefs ->
            val current = prefs[sadaqahJarBalanceKey]?.toDoubleOrNull() ?: 0.0
            prefs[sadaqahJarBalanceKey] = (current + spareChange).toString()
        }
    }

    suspend fun emptySadaqahJar() {
        context.dataStore.edit { prefs ->
            prefs[sadaqahJarBalanceKey] = "0.0"
        }
    }

    val sadaqahFridayReminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[sadaqahFridayReminderEnabledKey] ?: true
    }

    suspend fun setSadaqahFridayReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[sadaqahFridayReminderEnabledKey] = enabled
        }
    }

    val enableDepositWalletTransfer: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[enableDepositWalletTransferKey] ?: true
    }

    suspend fun setEnableDepositWalletTransfer(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[enableDepositWalletTransferKey] = enabled
        }
    }

    val timeFormat: Flow<TimeFormatPreference> = context.dataStore.data.map { prefs ->
        when (prefs[timeFormatKey]) {
            TimeFormatPreference.H24.name -> TimeFormatPreference.H24
            else -> TimeFormatPreference.H12
        }
    }

    suspend fun setTimeFormat(format: TimeFormatPreference) {
        context.dataStore.edit { prefs ->
            prefs[timeFormatKey] = format.name
        }
    }

    val prayerCalculationMethod: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[prayerCalculationMethodKey]
    }

    suspend fun setPrayerCalculationMethod(methodKey: String?) {
        context.dataStore.edit { prefs ->
            if (methodKey != null) {
                prefs[prayerCalculationMethodKey] = methodKey
            } else {
                prefs.remove(prayerCalculationMethodKey)
            }
        }
    }

    val prayerAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[prayerAlertsEnabledKey] ?: true
    }

    suspend fun setPrayerAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[prayerAlertsEnabledKey] = enabled }
    }

    val fajrAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[fajrAlertKey] ?: true }
    val dhuhrAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[dhuhrAlertKey] ?: true }
    val asrAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[asrAlertKey] ?: true }
    val maghribAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[maghribAlertKey] ?: true }
    val ishaAlertEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[ishaAlertKey] ?: true }

    suspend fun setPrayerReminderEnabled(prayerName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (prayerName.lowercase(java.util.Locale.US)) {
                "fajr" -> prefs[fajrAlertKey] = enabled
                "dhuhr" -> prefs[dhuhrAlertKey] = enabled
                "asr" -> prefs[asrAlertKey] = enabled
                "maghrib" -> prefs[maghribAlertKey] = enabled
                "isha" -> prefs[ishaAlertKey] = enabled
            }
        }
    }

    val prayerAdhanSound: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[prayerAdhanSoundKey] ?: "MAKKAH"
    }

    suspend fun setPrayerAdhanSound(soundKey: String) {
        context.dataStore.edit { prefs -> prefs[prayerAdhanSoundKey] = soundKey }
    }

    fun getSalahChecklist(dateKey: String): Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val key = stringSetPreferencesKey("salah_checklist_$dateKey")
        prefs[key] ?: emptySet()
    }

    suspend fun toggleSalahChecklist(dateKey: String, prayerName: String) {
        val key = stringSetPreferencesKey("salah_checklist_$dateKey")
        context.dataStore.edit { prefs ->
            val currentSet = prefs[key]?.toMutableSet() ?: mutableSetOf()
            if (currentSet.contains(prayerName)) {
                currentSet.remove(prayerName)
            } else {
                currentSet.add(prayerName)
            }
            prefs[key] = currentSet
        }
    }

    val currencyCode: Flow<String> = context.dataStore.data.map { prefs ->
        val code = prefs[currencyCodeKey]
            ?: com.mousa.mizani.core.format.CurrencyUtils.currencyWhenUnset(prefs[languageKey])
        com.mousa.mizani.core.format.CurrencyUtils.activeCurrencyCode = code
        code
    }

    suspend fun setCurrencyCode(code: String) {
        com.mousa.mizani.core.format.CurrencyUtils.activeCurrencyCode = code
        context.dataStore.edit { prefs ->
            prefs[currencyCodeKey] = code
        }
    }

    val loanAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[loanAlertsKey] ?: true
    }

    val debtAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[debtAlertsKey] ?: true
    }

    val salaryAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[salaryAlertsKey] ?: true
    }

    val budgetAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[budgetAlertsKey] ?: true
    }

    val dailyReminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[dailyReminderKey] ?: false
    }

    suspend fun setLoanAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[loanAlertsKey] = enabled }
    }

    suspend fun setDebtAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[debtAlertsKey] = enabled }
    }

    suspend fun setSalaryAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[salaryAlertsKey] = enabled }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[budgetAlertsKey] = enabled }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[dailyReminderKey] = enabled }
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[onboardingCompletedKey] = completed
        }
    }

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
        LocaleHelper.shownLanguage(prefs[languageKey], LocaleHelper.phoneLanguage())
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

    val calculationPreferences: Flow<CalculationPreferences> = context.dataStore.data.map { prefs ->
        CalculationPreferences(
            includeDepositIncome = prefs[calcIncludeDepositIncomeKey] ?: false,
            deductLoansFromIncome = prefs[calcDeductLoansFromIncomeKey] ?: true,
            includeWalletExpenses = prefs[calcIncludeWalletExpensesKey] ?: false,
            includeDepositExpenses = prefs[calcIncludeDepositExpensesKey] ?: false,
            includeLoanLinkedExpenses = prefs[calcIncludeLoanLinkedExpensesKey] ?: false,
            includeCarriedForwardInInsights = prefs[calcIncludeCarriedForwardInsightsKey] ?: true,
            useAvailableBalanceFloorInInsights = prefs[calcUseAvailableFloorInsightsKey] ?: true,
            reserveWalletInInsights = prefs[calcReserveWalletInsightsKey] ?: false,
            statsApplyGlobalIncomeExpenseRules = prefs[calcStatsApplyGlobalRulesKey] ?: true,
            statsIncludeManuallyExcluded = prefs[calcStatsIncludeManuallyExcludedKey] ?: false,
            runwayIncludeVaultBalance = prefs[calcRunwayIncludeVaultKey] ?: true,
            runwayExcludeWalletAndDepositBurn = prefs[calcRunwayExcludeWalletDepositKey] ?: false,
            zakahIncludeVaultOnAutoSync = prefs[calcZakahIncludeVaultKey] ?: true
        )
    }

    suspend fun getCalculationPreferences(): CalculationPreferences = calculationPreferences.first()

    suspend fun setCalcIncludeDepositIncome(enabled: Boolean) {
        context.dataStore.edit { it[calcIncludeDepositIncomeKey] = enabled }
    }

    suspend fun setCalcDeductLoansFromIncome(enabled: Boolean) {
        context.dataStore.edit { it[calcDeductLoansFromIncomeKey] = enabled }
    }

    suspend fun setCalcIncludeWalletExpenses(enabled: Boolean) {
        context.dataStore.edit { it[calcIncludeWalletExpensesKey] = enabled }
    }

    suspend fun setCalcIncludeDepositExpenses(enabled: Boolean) {
        context.dataStore.edit { it[calcIncludeDepositExpensesKey] = enabled }
    }

    suspend fun setCalcIncludeLoanLinkedExpenses(enabled: Boolean) {
        context.dataStore.edit { it[calcIncludeLoanLinkedExpensesKey] = enabled }
    }

    suspend fun setCalcIncludeCarriedForwardInsights(enabled: Boolean) {
        context.dataStore.edit { it[calcIncludeCarriedForwardInsightsKey] = enabled }
    }

    suspend fun setCalcUseAvailableFloorInsights(enabled: Boolean) {
        context.dataStore.edit { it[calcUseAvailableFloorInsightsKey] = enabled }
    }

    suspend fun setCalcReserveWalletInsights(enabled: Boolean) {
        context.dataStore.edit { it[calcReserveWalletInsightsKey] = enabled }
    }

    suspend fun setCalcStatsApplyGlobalRules(enabled: Boolean) {
        context.dataStore.edit { it[calcStatsApplyGlobalRulesKey] = enabled }
    }

    suspend fun setCalcStatsIncludeManuallyExcluded(enabled: Boolean) {
        context.dataStore.edit { it[calcStatsIncludeManuallyExcludedKey] = enabled }
    }

    suspend fun setCalcRunwayIncludeVault(enabled: Boolean) {
        context.dataStore.edit { it[calcRunwayIncludeVaultKey] = enabled }
    }

    suspend fun setCalcRunwayExcludeWalletDeposit(enabled: Boolean) {
        context.dataStore.edit { it[calcRunwayExcludeWalletDepositKey] = enabled }
    }

    suspend fun setCalcZakahIncludeVault(enabled: Boolean) {
        context.dataStore.edit { it[calcZakahIncludeVaultKey] = enabled }
    }

    suspend fun resetCalculationPreferences() {
        val defaults = CalculationPreferences()
        context.dataStore.edit { prefs ->
            prefs[calcIncludeDepositIncomeKey] = defaults.includeDepositIncome
            prefs[calcDeductLoansFromIncomeKey] = defaults.deductLoansFromIncome
            prefs[calcIncludeWalletExpensesKey] = defaults.includeWalletExpenses
            prefs[calcIncludeDepositExpensesKey] = defaults.includeDepositExpenses
            prefs[calcIncludeLoanLinkedExpensesKey] = defaults.includeLoanLinkedExpenses
            prefs[calcIncludeCarriedForwardInsightsKey] = defaults.includeCarriedForwardInInsights
            prefs[calcUseAvailableFloorInsightsKey] = defaults.useAvailableBalanceFloorInInsights
            prefs[calcReserveWalletInsightsKey] = defaults.reserveWalletInInsights
            prefs[calcStatsApplyGlobalRulesKey] = defaults.statsApplyGlobalIncomeExpenseRules
            prefs[calcStatsIncludeManuallyExcludedKey] = defaults.statsIncludeManuallyExcluded
            prefs[calcRunwayIncludeVaultKey] = defaults.runwayIncludeVaultBalance
            prefs[calcRunwayExcludeWalletDepositKey] = defaults.runwayExcludeWalletAndDepositBurn
            prefs[calcZakahIncludeVaultKey] = defaults.zakahIncludeVaultOnAutoSync
        }
    }

    private fun recurringDismissKey(recurringId: Long, dueMillis: Long): String =
        "$recurringId:$dueMillis"

    suspend fun exportAllPreferencesMap(): Map<String, String> {
        val prefs = context.dataStore.data.first()
        val result = mutableMapOf<String, String>()
        for ((key, value) in prefs.asMap()) {
            if (value is Set<*>) {
                result[key.name] = value.joinToString(",")
            } else {
                result[key.name] = value.toString()
            }
        }
        return DeviceOnlyPreferences.withoutDeviceOnly(result)
    }

    suspend fun importAllPreferencesMap(map: Map<String, String>) {
        context.dataStore.edit { prefs ->
            map[themeModeKey.name]?.let { prefs[themeModeKey] = it }
            map[themePaletteKey.name]?.let { prefs[themePaletteKey] = it }
            map[languageKey.name]?.let {
                prefs[languageKey] = it
                LocaleHelper.persistLanguage(context, it)
            }
            map[timeFormatKey.name]?.let { prefs[timeFormatKey] = it }
            map[monthModeKey.name]?.let { prefs[monthModeKey] = it }
            map[openingCashKey.name]?.let { prefs[openingCashKey] = it }
            map[openingBankKey.name]?.let { prefs[openingBankKey] = it }
            map[currencyCodeKey.name]?.let {
                prefs[currencyCodeKey] = it
                com.mousa.mizani.core.format.CurrencyUtils.activeCurrencyCode = it
            }
            map[biometricLockKey.name]?.toBooleanStrictOrNull()?.let { prefs[biometricLockKey] = it }
            map[onboardingCompletedKey.name]?.toBooleanStrictOrNull()?.let { prefs[onboardingCompletedKey] = it }
            map[loanAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[loanAlertsKey] = it }
            map[debtAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[debtAlertsKey] = it }
            map[salaryAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[salaryAlertsKey] = it }
            map[budgetAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[budgetAlertsKey] = it }
            map[dailyReminderKey.name]?.toBooleanStrictOrNull()?.let { prefs[dailyReminderKey] = it }
            map[prayerAlertsEnabledKey.name]?.toBooleanStrictOrNull()?.let { prefs[prayerAlertsEnabledKey] = it }
            map[fajrAlertKey.name]?.toBooleanStrictOrNull()?.let { prefs[fajrAlertKey] = it }
            map[dhuhrAlertKey.name]?.toBooleanStrictOrNull()?.let { prefs[dhuhrAlertKey] = it }
            map[asrAlertKey.name]?.toBooleanStrictOrNull()?.let { prefs[asrAlertKey] = it }
            map[maghribAlertKey.name]?.toBooleanStrictOrNull()?.let { prefs[maghribAlertKey] = it }
            map[ishaAlertKey.name]?.toBooleanStrictOrNull()?.let { prefs[ishaAlertKey] = it }
            map[prayerCalculationMethodKey.name]?.let { prefs[prayerCalculationMethodKey] = it }
            map[prayerSavedCityEnKey.name]?.let { prefs[prayerSavedCityEnKey] = it }
            map[prayerSavedCityArKey.name]?.let { prefs[prayerSavedCityArKey] = it }
            map[prayerSavedLatKey.name]?.let { prefs[prayerSavedLatKey] = it }
            map[prayerSavedLngKey.name]?.let { prefs[prayerSavedLngKey] = it }
            map[prayerSavedTzKey.name]?.let { prefs[prayerSavedTzKey] = it }
            map[prayerSavedElevationKey.name]?.let { prefs[prayerSavedElevationKey] = it }
            map[stickyPrayerAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[stickyPrayerAlertsKey] = it }
            map[stickyDailyReminderKey.name]?.toBooleanStrictOrNull()?.let { prefs[stickyDailyReminderKey] = it }
            map[stickyBudgetAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[stickyBudgetAlertsKey] = it }
            map[stickyLoanAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[stickyLoanAlertsKey] = it }
            map[stickySalaryAlertsKey.name]?.toBooleanStrictOrNull()?.let { prefs[stickySalaryAlertsKey] = it }
            map[calcIncludeDepositIncomeKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcIncludeDepositIncomeKey] = it }
            map[calcDeductLoansFromIncomeKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcDeductLoansFromIncomeKey] = it }
            map[calcIncludeWalletExpensesKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcIncludeWalletExpensesKey] = it }
            map[calcIncludeDepositExpensesKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcIncludeDepositExpensesKey] = it }
            map[calcIncludeLoanLinkedExpensesKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcIncludeLoanLinkedExpensesKey] = it }
            map[calcIncludeCarriedForwardInsightsKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcIncludeCarriedForwardInsightsKey] = it }
            map[calcUseAvailableFloorInsightsKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcUseAvailableFloorInsightsKey] = it }
            map[calcReserveWalletInsightsKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcReserveWalletInsightsKey] = it }
            map[calcStatsApplyGlobalRulesKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcStatsApplyGlobalRulesKey] = it }
            map[calcStatsIncludeManuallyExcludedKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcStatsIncludeManuallyExcludedKey] = it }
            map[calcRunwayIncludeVaultKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcRunwayIncludeVaultKey] = it }
            map[calcRunwayExcludeWalletDepositKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcRunwayExcludeWalletDepositKey] = it }
            map[calcZakahIncludeVaultKey.name]?.toBooleanStrictOrNull()?.let { prefs[calcZakahIncludeVaultKey] = it }
            map[sadaqahRoundUpEnabledKey.name]?.toBooleanStrictOrNull()?.let { prefs[sadaqahRoundUpEnabledKey] = it }
            map[sadaqahFridayReminderEnabledKey.name]?.toBooleanStrictOrNull()?.let { prefs[sadaqahFridayReminderEnabledKey] = it }
            map[enableDepositWalletTransferKey.name]?.toBooleanStrictOrNull()?.let { prefs[enableDepositWalletTransferKey] = it }
            map[sadaqahJarBalanceKey.name]?.let { prefs[sadaqahJarBalanceKey] = it }
            // A real export without a balance means it was 0 then (see RestoredBalances).
            RestoredBalances.keysToClear(map).forEach { prefs.remove(stringPreferencesKey(it)) }
            map[prayerAdhanSoundKey.name]?.let { prefs[prayerAdhanSoundKey] = it }
            map.filterKeys { it.startsWith("salah_checklist_") }.forEach { (name, value) ->
                prefs[stringSetPreferencesKey(name)] = value.split(",").filter { it.isNotBlank() }.toSet()
            }
            // Pro / owner entitlement keys are deliberately not restored: the file is user-editable.
            map[lastLoanSheetDateKey.name]?.let { prefs[lastLoanSheetDateKey] = it }
            map[lastDebtRolloverMonthKey.name]?.let { prefs[lastDebtRolloverMonthKey] = it }
            map[dismissedRecurringKey.name]?.let {
                prefs[dismissedRecurringKey] = it.split(",").filter { s -> s.isNotBlank() }.toSet()
            }
        }
    }
}
