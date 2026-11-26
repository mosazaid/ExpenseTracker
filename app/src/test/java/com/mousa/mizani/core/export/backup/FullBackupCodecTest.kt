package com.mousa.mizani.core.export.backup

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.AppAlert
import com.mousa.mizani.data.database.entities.Budget
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.ConfiguredLoan
import com.mousa.mizani.data.database.entities.CurrencyExchange
import com.mousa.mizani.data.database.entities.ItemPriceRecord
import com.mousa.mizani.data.database.entities.MonthlyLoanPayment
import com.mousa.mizani.data.database.entities.PendingSmsTransaction
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.SubCategory
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionSplit
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.database.entities.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Date

class FullBackupCodecTest {

    private val t0 = Date(1_700_000_000_000L)
    private val t1 = Date(1_702_000_000_000L)

    private val fullSnapshot = BackupSnapshot(
        categories = listOf(Category(1, "Food", "restaurant", "#FF0000", TransactionType.EXPENSE, true, t0)),
        subCategories = listOf(SubCategory(10, 1, "Groceries", t0)),
        budgets = listOf(Budget(20, 1, 300.0, t0, t1, t0)),
        configuredLoans = listOf(ConfiguredLoan(30, "Car", 150.0, AccountType.BANK, true, true, t0)),
        monthlyLoanPayments = listOf(
            MonthlyLoanPayment(40, 30, "2026-11", 150.0, AccountType.BANK, true, t1, 100, false, t0)
        ),
        recurringTransactions = listOf(
            RecurringTransaction(50, 9.99, "Netflix", TransactionType.EXPENSE, 1, AccountType.BANK,
                RecurrenceFrequency.MONTHLY, t1, true, t0, autoAdd = true, anchorDay = 31)
        ),
        transactions = listOf(
            Transaction(
                id = 100, amount = 42.5, description = "Supermarket", subDescription = "weekly",
                date = t1, type = TransactionType.EXPENSE, categoryId = 1, accountType = AccountType.CASH,
                linkedExpenseId = 101, debtorNote = "Ali", awaitingReimbursement = true, debtType = "LENT",
                recurringId = 50, receiptImagePath = "/receipts/a.jpg", excludeFromMonthExpenses = true,
                currency = "AED", originalAmount = 220.0, exchangeRateUsed = 0.193, tripId = 70,
                createdAt = t0, updatedAt = t1
            ),
            Transaction(
                id = 101, amount = 10.0, description = "Refund", date = t1, type = TransactionType.INCOME,
                accountType = AccountType.BANK, toAccountType = AccountType.CASH, startsNewPeriod = true,
                carriedForwardBalance = 12.0, allowNegativeBalance = true, createdAt = t0, updatedAt = t1
            )
        ),
        transactionSplits = listOf(TransactionSplit(200, 100, 10, "Groceries", 30.0, "milk", true, "Sara", false)),
        appAlerts = listOf(AppAlert(300, "BUDGET", "Over budget", "Food is at 90%", 1, "2026-11", false, t0)),
        trips = listOf(Trip(70, "Dubai", "UAE", "AED", "JOD", 2000.0, t0, t1, true, "family", t0)),
        currencyExchanges = listOf(
            CurrencyExchange(80, "JOD", 500.0, "USD", 705.0, 1.41, 2.0, "JOD", "Bank", t1, "travel", t0)
        ),
        itemPriceHistory = listOf(ItemPriceRecord(90, "Milk 1L", "milk 1l", 1.2, "JOD", 10, 100, "Carrefour", t1, null, t0)),
        pendingSmsTransactions = listOf(
            PendingSmsTransaction(95, "ArabBank", "Purchase 5 JOD", 5.0, "JOD", "Shop", "**1234", "Food", 1_701_000_000_000L, false, true)
        )
    )

    @Test
    fun `round trip keeps every table and every field`() {
        val json = BackupJsonCodec.encode(fullSnapshot, mapOf("theme" to "dark"), 21, "2026-11-25T10:00:00Z").toString()

        val decoded = BackupJsonCodec.decode(json)

        assertEquals(BackupJsonCodec.FORMAT_VERSION, decoded.formatVersion)
        assertEquals(fullSnapshot, decoded.snapshot)
        assertEquals(mapOf("theme" to "dark"), decoded.preferences)
    }

    @Test
    fun `format 1 backup from the old app decodes with defaults and leaves new tables untouched`() {
        val decoded = BackupJsonCodec.decode(FORMAT_1_BACKUP)

        assertEquals(1, decoded.formatVersion)
        val txn = decoded.snapshot.transactions.single()
        assertFalse(txn.excludeFromMonthExpenses)
        assertEquals("JOD", txn.currency)
        assertNull(txn.tripId)
        assertNull(decoded.snapshot.trips)
        assertNull(decoded.snapshot.currencyExchanges)
        assertNull(decoded.snapshot.itemPriceHistory)
        assertNull(decoded.snapshot.pendingSmsTransactions)
        assertEquals("ar", decoded.preferences?.get("language"))
    }

    @Test
    fun `orphan subcategories from the old app are skipped so restore cannot hit FK error 787`() {
        val decoded = BackupJsonCodec.decode(FORMAT_1_BACKUP)

        val sanitized = BackupSanitizer.sanitize(decoded.snapshot)

        assertEquals(listOf(10L), sanitized.snapshot.subCategories.map { it.id })
        assertEquals(2, sanitized.fixes.size)
        assertTrue(sanitized.fixes.any { "'Baby'" in it && "category 18" in it })
    }

    @Test
    fun `sanitizer mirrors the database delete rules for every foreign key`() {
        val broken = fullSnapshot.copy(
            subCategories = fullSnapshot.subCategories + SubCategory(11, 999, "Orphan"),
            budgets = fullSnapshot.budgets + Budget(21, 999, 1.0, t0, t1),
            recurringTransactions = fullSnapshot.recurringTransactions.map { it.copy(categoryId = 999) },
            transactions = fullSnapshot.transactions.map { if (it.id == 100L) it.copy(categoryId = 999) else it },
            transactionSplits = fullSnapshot.transactionSplits + TransactionSplit(201, 555, amount = 1.0),
            monthlyLoanPayments = fullSnapshot.monthlyLoanPayments + MonthlyLoanPayment(41, 999, "2026-10", 1.0)
        )

        val result = BackupSanitizer.sanitize(broken)

        assertEquals(fullSnapshot.subCategories, result.snapshot.subCategories)
        assertEquals(fullSnapshot.budgets, result.snapshot.budgets)
        assertEquals(fullSnapshot.transactionSplits, result.snapshot.transactionSplits)
        assertEquals(fullSnapshot.monthlyLoanPayments, result.snapshot.monthlyLoanPayments)
        assertNull(result.snapshot.recurringTransactions.single().categoryId)
        assertNull(result.snapshot.transactions.first { it.id == 100L }.categoryId)
        assertEquals(6, result.fixes.size)
    }

    @Test
    fun `clean backup needs no fixes`() {
        val result = BackupSanitizer.sanitize(fullSnapshot)

        assertFalse(result.hasFixes)
        assertEquals(fullSnapshot, result.snapshot)
    }

    @Test
    fun `bad row error names the table and index`() {
        val json = """{"version":2,"categories":[{"id":1,"name":"Food"}]}"""

        try {
            BackupJsonCodec.decode(json)
            fail("Expected a decode error")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().startsWith("Invalid entry in 'categories' at index 0"))
        }
    }

    private companion object {
        /** Shaped like a file from the old com.example.expensetracker exporter, with orphan subcategories. */
        val FORMAT_1_BACKUP = """
            {
              "version": 1,
              "databaseVersion": 17,
              "exportedAt": "2026-11-20T09:00:00Z",
              "preferences": {"language": "ar"},
              "categories": [
                {"id": 1, "name": "Food", "icon": "restaurant", "color": "#FF0000", "type": "EXPENSE", "isDefault": true, "createdAt": 1700000000000}
              ],
              "subCategories": [
                {"id": 10, "categoryId": 1, "name": "Vegetables", "createdAt": 1700000000000},
                {"id": 2, "categoryId": 20, "name": "Groceries", "createdAt": 1700000000000},
                {"id": 4, "categoryId": 18, "name": "Baby", "createdAt": 1700000000000}
              ],
              "budgets": [],
              "configuredLoans": [],
              "monthlyLoanPayments": [],
              "recurringTransactions": [],
              "transactions": [
                {"id": 100, "amount": 5.5, "description": "Bread", "subDescription": null, "date": 1702000000000,
                 "type": "EXPENSE", "categoryId": 1, "accountType": "CASH", "toAccountType": null,
                 "startsNewPeriod": false, "carriedForwardBalance": null, "allowNegativeBalance": false,
                 "linkedExpenseId": null, "debtorNote": null, "awaitingReimbursement": false, "debtType": null,
                 "isDebtSettled": false, "recurringId": null, "receiptImagePath": null,
                 "createdAt": 1702000000000, "updatedAt": 1702000000000}
              ],
              "transactionSplits": [],
              "appAlerts": []
            }
        """.trimIndent()
    }
}
