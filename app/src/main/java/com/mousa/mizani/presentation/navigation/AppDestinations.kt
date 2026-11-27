package com.mousa.mizani.presentation.navigation

import kotlinx.serialization.Serializable

// ── Bottom-tab destinations ───────────────────────────────────────────────────
@Serializable object Overview
@Serializable object History
@Serializable object Statistics
@Serializable object More

// ── Secondary / push destinations ────────────────────────────────────────────
@Serializable object Transfer
@Serializable object Wallet
@Serializable object Loans
@Serializable object Alerts
@Serializable object Debts
@Serializable object Recurring
@Serializable object Categories
@Serializable object Settings
@Serializable object CalculationSettings
@Serializable object NotificationSettings
@Serializable object DatabaseBrowser
@Serializable object Onboarding
@Serializable object ZakahCalculator
@Serializable object PrayerQiblah
@Serializable object DepositVault
@Serializable object CurrencyExchangeTracker
@Serializable object TravelModeTrips
@Serializable object PriceHistoryTracker
@Serializable object SadaqahJar
@Serializable object FinancialRunway
@Serializable object MurabahaSimulator
@Serializable object SubscriptionPlan

// ── Settings pages ───────────────────────────────────────────────────────────
@Serializable object AppearanceSettings
@Serializable object LanguageRegionSettings
@Serializable object AccountBalancesSettings
@Serializable object MonthCalculationSettings
@Serializable object BackupSettings
@Serializable object ImportExportSettings

// ── Parameterised destinations ────────────────────────────────────────────────
/**
 * Navigate to add a new transaction, or edit an existing one.
 *
 * @param transactionId  -1L means "new transaction"
 * @param recurringId    -1L means "not launched from a recurring template"
 * @param expectedDueMillis  the due date the sender meant to record; the form stays empty if the
 *                           recurring item has moved past it. -1L means "whatever is current"
 */
@Serializable
data class AddTransaction(
    val transactionId: Long = -1L,
    val recurringId: Long = -1L,
    val accountType: String? = null,
    val expectedDueMillis: Long = -1L
)

/** The form for the period of a recurring item that is due at [dueMillis], if one was named. */
fun recurringFormRoute(recurringId: Long, dueMillis: Long?): AddTransaction =
    AddTransaction(recurringId = recurringId, expectedDueMillis = dueMillis ?: -1L)

/** Edit an existing transfer by its transaction ID. */
@Serializable
data class EditTransfer(val transactionId: Long)

/** Edit an existing wallet move by its transaction ID. */
@Serializable
data class EditWallet(val transactionId: Long)
