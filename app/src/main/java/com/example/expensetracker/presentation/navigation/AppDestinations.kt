package com.example.expensetracker.presentation.navigation

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
@Serializable object DatabaseBrowser
@Serializable object Onboarding

// ── Parameterised destinations ────────────────────────────────────────────────
/**
 * Navigate to add a new transaction, or edit an existing one.
 *
 * @param transactionId  -1L means "new transaction"
 * @param recurringId    -1L means "not launched from a recurring template"
 */
@Serializable
data class AddTransaction(
    val transactionId: Long = -1L,
    val recurringId: Long = -1L
)

/** Edit an existing transfer by its transaction ID. */
@Serializable
data class EditTransfer(val transactionId: Long)

/** Edit an existing wallet move by its transaction ID. */
@Serializable
data class EditWallet(val transactionId: Long)
