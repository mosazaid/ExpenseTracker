package com.mousa.mizani.core.export.backup

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Restore deletes the core tables, so decode must refuse any file that is not a Mizani backup. */
class BackupFileValidationTest {

    /** The lists every exporter since format 1 has written, even when empty. */
    private val coreKeys = listOf(
        "categories", "subCategories", "budgets", "configuredLoans", "monthlyLoanPayments",
        "recurringTransactions", "transactions", "transactionSplits", "appAlerts"
    )

    @Test
    fun `empty object is rejected`() {
        assertRejected("{}")
    }

    @Test
    fun `unrelated json is rejected`() {
        assertRejected("""{"name":"my-package","version":"1.0.0","dependencies":{}}""")
        assertRejected("""{"version":1,"items":[]}""")
    }

    @Test
    fun `core lists without a version are rejected`() {
        assertRejected(emptyBackup(1).apply { remove("version") }.toString())
    }

    @Test
    fun `version that is not a supported whole number is rejected`() {
        listOf<Any>(
            "2", true, 0, -1, 1.5, BackupJsonCodec.FORMAT_VERSION + 1, 4_294_967_297L, JSONObject.NULL
        ).forEach { version ->
            assertRejected(emptyBackup(1).put("version", version).toString())
        }
    }

    @Test
    fun `a missing core list is rejected and named`() {
        coreKeys.forEach { key ->
            val error = assertRejected(emptyBackup(1).apply { remove(key) }.toString())

            assertTrue(error.message.orEmpty(), "'$key'" in error.message.orEmpty())
        }
    }

    @Test
    fun `a core key with the wrong type is rejected`() {
        coreKeys.forEach { key ->
            listOf<Any>(JSONObject(), "[]", 0, JSONObject.NULL).forEach { wrong ->
                assertRejected(emptyBackup(2).put(key, wrong).toString())
            }
        }
    }

    @Test
    fun `backup of an empty app is accepted`() {
        val nothing = BackupSnapshot(
            categories = emptyList(), subCategories = emptyList(), budgets = emptyList(),
            configuredLoans = emptyList(), monthlyLoanPayments = emptyList(),
            recurringTransactions = emptyList(), transactions = emptyList(),
            transactionSplits = emptyList(), appAlerts = emptyList(),
            trips = emptyList(), currencyExchanges = emptyList(),
            itemPriceHistory = emptyList(), pendingSmsTransactions = emptyList()
        )
        val json = BackupJsonCodec.encode(nothing, emptyMap(), 23, "2026-11-27T10:00:00Z").toString()

        val decoded = BackupJsonCodec.decode(json)

        assertEquals(BackupJsonCodec.FORMAT_VERSION, decoded.formatVersion)
        assertEquals(nothing, decoded.snapshot)
    }

    @Test
    fun `format 1 file with only the nine original lists is accepted`() {
        val decoded = BackupJsonCodec.decode(emptyBackup(1).toString())

        assertEquals(1, decoded.formatVersion)
        assertTrue(decoded.snapshot.transactions.isEmpty())
        assertNull(decoded.snapshot.trips)
        assertNull(decoded.preferences)
    }

    private fun emptyBackup(version: Int): JSONObject = JSONObject().apply {
        put("version", version)
        coreKeys.forEach { put(it, JSONArray()) }
    }

    private fun assertRejected(json: String): IllegalArgumentException =
        assertThrows(json, IllegalArgumentException::class.java) { BackupJsonCodec.decode(json) }
}
