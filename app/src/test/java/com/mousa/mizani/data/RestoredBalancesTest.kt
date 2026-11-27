package com.mousa.mizani.data

import com.mousa.mizani.data.preferences.RestoredBalances
import org.junit.Assert.assertEquals
import org.junit.Test

class RestoredBalancesTest {

    private val allBalances = setOf("opening_cash_balance", "opening_bank_balance", "sadaqah_jar_balance")

    @Test
    fun `real export with every balance clears nothing`() {
        val backup = mapOf(
            "onboarding_completed" to "true",
            "opening_cash_balance" to "120.0",
            "opening_bank_balance" to "900.0",
            "sadaqah_jar_balance" to "3.5"
        )

        assertEquals(emptySet<String>(), RestoredBalances.keysToClear(backup))
    }

    @Test
    fun `real export without balances clears all three`() {
        val backup = mapOf("onboarding_completed" to "true", "app_language" to "ar")

        assertEquals(allBalances, RestoredBalances.keysToClear(backup))
    }

    @Test
    fun `real export clears only the balances it lacks`() {
        val backup = mapOf("onboarding_completed" to "true", "opening_cash_balance" to "50.0")

        assertEquals(
            setOf("opening_bank_balance", "sadaqah_jar_balance"),
            RestoredBalances.keysToClear(backup)
        )
    }

    @Test
    fun `file without a settings block clears nothing`() {
        assertEquals(emptySet<String>(), RestoredBalances.keysToClear(null))
    }

    @Test
    fun `empty settings block clears nothing`() {
        assertEquals(emptySet<String>(), RestoredBalances.keysToClear(emptyMap()))
    }

    @Test
    fun `partial block that no app export could write clears nothing`() {
        val backup = mapOf("app_language" to "ar", "theme_mode" to "DARK")

        assertEquals(emptySet<String>(), RestoredBalances.keysToClear(backup))
    }
}
