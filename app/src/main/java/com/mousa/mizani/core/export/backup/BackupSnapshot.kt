package com.mousa.mizani.core.export.backup

import com.mousa.mizani.data.database.entities.AppAlert
import com.mousa.mizani.data.database.entities.Budget
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.ConfiguredLoan
import com.mousa.mizani.data.database.entities.CurrencyExchange
import com.mousa.mizani.data.database.entities.ItemPriceRecord
import com.mousa.mizani.data.database.entities.MonthlyLoanPayment
import com.mousa.mizani.data.database.entities.PendingSmsTransaction
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.SubCategory
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionSplit
import com.mousa.mizani.data.database.entities.Trip

/**
 * Every table held in a full backup.
 *
 * The four nullable lists were added in backup format 2. In a format-1 file they are
 * missing, so they stay `null`, and restoring leaves those tables on the device untouched
 * (rather than wiping them with an empty list).
 */
data class BackupSnapshot(
    val categories: List<Category> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val configuredLoans: List<ConfiguredLoan> = emptyList(),
    val monthlyLoanPayments: List<MonthlyLoanPayment> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val transactionSplits: List<TransactionSplit> = emptyList(),
    val appAlerts: List<AppAlert> = emptyList(),
    val trips: List<Trip>? = null,
    val currencyExchanges: List<CurrencyExchange>? = null,
    val itemPriceHistory: List<ItemPriceRecord>? = null,
    val pendingSmsTransactions: List<PendingSmsTransaction>? = null
)

/** A decoded backup file: the database tables plus the saved preferences (null if absent). */
data class DecodedBackup(
    val formatVersion: Int,
    val snapshot: BackupSnapshot,
    val preferences: Map<String, String>?
)
