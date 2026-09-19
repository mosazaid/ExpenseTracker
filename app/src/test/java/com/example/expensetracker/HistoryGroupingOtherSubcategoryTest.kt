package com.example.expensetracker

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.screens.HistoryGrouping
import com.example.expensetracker.presentation.screens.SUBCATEGORY_OTHER
import com.example.expensetracker.presentation.screens.TransactionTypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class HistoryGroupingOtherSubcategoryTest {

    @Test
    fun testSubcategoryOtherFilter_includesNullBlankAndOtherTransactions() {
        val now = Date()
        val catId = 10L

        val txnNoSubDesc = Transaction(
            id = 1L,
            amount = 50.0,
            type = TransactionType.EXPENSE,
            categoryId = catId,
            date = now,
            description = "Groceries no sub",
            subDescription = null,
            accountType = AccountType.CASH
        )

        val txnBlankSubDesc = Transaction(
            id = 2L,
            amount = 30.0,
            type = TransactionType.EXPENSE,
            categoryId = catId,
            date = now,
            description = "Groceries blank sub",
            subDescription = "   ",
            accountType = AccountType.CASH
        )

        val txnExplicitOther = Transaction(
            id = 3L,
            amount = 20.0,
            type = TransactionType.EXPENSE,
            categoryId = catId,
            date = now,
            description = "Groceries explicit other",
            subDescription = "Other",
            accountType = AccountType.CASH
        )

        val txnSpecificSub = Transaction(
            id = 4L,
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = catId,
            date = now,
            description = "Groceries Fruits",
            subDescription = "Fruits",
            accountType = AccountType.CASH
        )

        val allTxns = listOf(txnNoSubDesc, txnBlankSubDesc, txnExplicitOther, txnSpecificSub)

        val sections = HistoryGrouping.group(
            transactions = allTxns,
            period = HistoryPeriod.MONTH,
            typeFilter = TransactionTypeFilter.EXPENSE,
            categoryIdFilter = catId,
            subCategoryFilter = SUBCATEGORY_OTHER
        )

        val groupedTxns = sections.flatMap { it.transactions }

        // Must contain 1, 2, 3
        assertEquals(3, groupedTxns.size)
        assertTrue(groupedTxns.any { it.id == 1L })
        assertTrue(groupedTxns.any { it.id == 2L })
        assertTrue(groupedTxns.any { it.id == 3L })
        // Must NOT contain 4 ("Fruits")
        assertTrue(groupedTxns.none { it.id == 4L })
    }

    @Test
    fun testNamedSubcategoryFilter_onlyIncludesMatchingSubcategory() {
        val now = Date()
        val catId = 10L

        val txnFruits = Transaction(
            id = 1L,
            amount = 45.0,
            type = TransactionType.EXPENSE,
            categoryId = catId,
            date = now,
            description = "Bananas",
            subDescription = "Fruits",
            accountType = AccountType.CASH
        )

        val txnNoSub = Transaction(
            id = 2L,
            amount = 20.0,
            type = TransactionType.EXPENSE,
            categoryId = catId,
            date = now,
            description = "Paper towels",
            subDescription = null,
            accountType = AccountType.CASH
        )

        val sections = HistoryGrouping.group(
            transactions = listOf(txnFruits, txnNoSub),
            period = HistoryPeriod.MONTH,
            typeFilter = TransactionTypeFilter.EXPENSE,
            categoryIdFilter = catId,
            subCategoryFilter = "Fruits"
        )

        val groupedTxns = sections.flatMap { it.transactions }
        assertEquals(1, groupedTxns.size)
        assertEquals(1L, groupedTxns.first().id)
    }
}
