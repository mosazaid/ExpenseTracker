package com.mousa.mizani.data.preferences

/**
 * Which money balances a restore must reset because the backup had none.
 *
 * Every app version that writes a settings block copies all stored settings into it, and the
 * balance keys only exist once a balance was set. So a real export without a balance key means
 * that balance was 0 when the backup was made. A real export always has "onboarding_completed"
 * (the app cannot be reached, so nothing can be exported, before onboarding saves it). A block
 * without it was not written by the app, so it never resets a balance.
 */
object RestoredBalances {

    const val OPENING_CASH_KEY = "opening_cash_balance"
    const val OPENING_BANK_KEY = "opening_bank_balance"
    const val SADAQAH_JAR_KEY = "sadaqah_jar_balance"
    private const val EXPORT_MARKER_KEY = "onboarding_completed"

    private val BALANCE_KEYS: Set<String> = setOf(OPENING_CASH_KEY, OPENING_BANK_KEY, SADAQAH_JAR_KEY)

    /** The balance keys to remove on restore; empty when there is no block or it is not a real export. */
    fun keysToClear(backup: Map<String, String>?): Set<String> {
        if (backup == null || EXPORT_MARKER_KEY !in backup) return emptySet()
        return BALANCE_KEYS - backup.keys
    }
}
