package com.example.expensetracker.presentation.navigation

object AppRoutes {
    const val OVERVIEW = "overview"
    const val HISTORY = "history"
    const val ADD_TRANSACTION = "addTransaction"
    const val STATISTICS = "statistics"
    const val CATEGORIES = "categories"
    const val TRANSFER = "transfer"
    const val WALLET = "wallet"
    const val SETTINGS = "settings"
    const val DATABASE_BROWSER = "databaseBrowser"
    const val MORE = "more"
    const val LOANS = "loans"
    const val ALERTS = "alerts"
    const val DEBTS = "debts"
    const val RECURRING = "recurring"
    const val ONBOARDING = "onboarding"

    const val EDIT_TRANSFER = "editTransfer/{transactionId}"
    const val EDIT_TRANSFER_ARG = "transactionId"

    const val EDIT_WALLET = "editWallet/{transactionId}"
    const val EDIT_WALLET_ARG = "transactionId"

    const val ADD_TRANSACTION_ARG_TRANSACTION_ID = "transactionId"
    const val ADD_TRANSACTION_ARG_RECURRING_ID = "recurringId"
    const val ADD_TRANSACTION_WITH_ARGS =
        "$ADD_TRANSACTION?$ADD_TRANSACTION_ARG_TRANSACTION_ID={$ADD_TRANSACTION_ARG_TRANSACTION_ID}&" +
            "$ADD_TRANSACTION_ARG_RECURRING_ID={$ADD_TRANSACTION_ARG_RECURRING_ID}"

    fun addTransactionRoute(transactionId: Long? = null, recurringId: Long? = null): String {
        val transactionValue = transactionId ?: -1L
        val recurringValue = recurringId ?: -1L
        return "$ADD_TRANSACTION?$ADD_TRANSACTION_ARG_TRANSACTION_ID=$transactionValue&" +
            "$ADD_TRANSACTION_ARG_RECURRING_ID=$recurringValue"
    }

    fun editTransferRoute(transactionId: Long): String = "editTransfer/$transactionId"

    fun editWalletRoute(transactionId: Long): String = "editWallet/$transactionId"
}
