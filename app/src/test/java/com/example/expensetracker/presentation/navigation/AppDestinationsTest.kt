package com.example.expensetracker.presentation.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppDestinationsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testAddTransaction_defaultArguments() {
        val dest = AddTransaction()
        assertEquals(-1L, dest.transactionId)
        assertEquals(-1L, dest.recurringId)
    }

    @Test
    fun testAddTransaction_customArguments() {
        val dest = AddTransaction(transactionId = 42L, recurringId = 99L)
        assertEquals(42L, dest.transactionId)
        assertEquals(99L, dest.recurringId)

        val encoded = json.encodeToString(dest)
        val decoded = json.decodeFromString<AddTransaction>(encoded)
        assertEquals(dest, decoded)
    }

    @Test
    fun testEditTransfer_serialization() {
        val dest = EditTransfer(transactionId = 123L)
        assertEquals(123L, dest.transactionId)

        val encoded = json.encodeToString(dest)
        val decoded = json.decodeFromString<EditTransfer>(encoded)
        assertEquals(dest, decoded)
    }

    @Test
    fun testEditWallet_serialization() {
        val dest = EditWallet(transactionId = 456L)
        assertEquals(456L, dest.transactionId)

        val encoded = json.encodeToString(dest)
        val decoded = json.decodeFromString<EditWallet>(encoded)
        assertEquals(dest, decoded)
    }

    @Test
    fun testObjectDestinations_serialization() {
        // Test that all singleton objects serialize without error
        assertNotNull(json.encodeToString(Overview))
        assertNotNull(json.encodeToString(History))
        assertNotNull(json.encodeToString(Statistics))
        assertNotNull(json.encodeToString(More))
        assertNotNull(json.encodeToString(Transfer))
        assertNotNull(json.encodeToString(Wallet))
        assertNotNull(json.encodeToString(Loans))
        assertNotNull(json.encodeToString(Alerts))
        assertNotNull(json.encodeToString(Debts))
        assertNotNull(json.encodeToString(Recurring))
        assertNotNull(json.encodeToString(Categories))
        assertNotNull(json.encodeToString(Settings))
        assertNotNull(json.encodeToString(DatabaseBrowser))
        assertNotNull(json.encodeToString(Onboarding))
    }
}
