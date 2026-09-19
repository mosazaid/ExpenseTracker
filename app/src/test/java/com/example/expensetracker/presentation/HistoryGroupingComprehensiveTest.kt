package com.example.expensetracker.presentation

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.presentation.screens.HistoryGrouping
import com.example.expensetracker.presentation.screens.SUBCATEGORY_OTHER
import com.example.expensetracker.presentation.screens.TransactionSortOrder
import com.example.expensetracker.presentation.screens.TransactionTypeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class HistoryGroupingComprehensiveTest {

    @Test
    fun testTypeFilters() {
        val now = Date()
        val income = Transaction(id = 1L, amount = 100.0, type = TransactionType.INCOME, categoryId = null, accountType = AccountType.CASH, date = now, description = "Gift")
        val expense = Transaction(id = 2L, amount = 40.0, type = TransactionType.EXPENSE, categoryId = null, accountType = AccountType.CASH, date = now, description = "Lunch")
        val wallet = Transaction(id = 3L, amount = 20.0, type = TransactionType.WALLET_MOVE, categoryId = null, accountType = AccountType.CASH, toAccountType = AccountType.WALLET, date = now, description = "Snack")
        val owedUnreimbursed = Transaction(id = 4L, amount = 50.0, type = TransactionType.EXPENSE, categoryId = null, accountType = AccountType.CASH, date = now, description = "Lent", awaitingReimbursement = true)
        val owedReimbursed = Transaction(id = 5L, amount = 60.0, type = TransactionType.EXPENSE, categoryId = null, accountType = AccountType.CASH, date = now, description = "Paid back", awaitingReimbursement = true)

        val all = listOf(income, expense, wallet, owedUnreimbursed, owedReimbursed)
        val reimbursedIds = setOf(5L)

        // ALL
        val allGrouped = HistoryGrouping.group(all, HistoryPeriod.MONTH, TransactionTypeFilter.ALL, reimbursedExpenseIds = reimbursedIds).flatMap { it.transactions }
        assertEquals(5, allGrouped.size)

        // INCOME
        val incGrouped = HistoryGrouping.group(all, HistoryPeriod.MONTH, TransactionTypeFilter.INCOME, reimbursedExpenseIds = reimbursedIds).flatMap { it.transactions }
        assertEquals(1, incGrouped.size)
        assertEquals(1L, incGrouped.first().id)

        // EXPENSE
        val expGrouped = HistoryGrouping.group(all, HistoryPeriod.MONTH, TransactionTypeFilter.EXPENSE, reimbursedExpenseIds = reimbursedIds).flatMap { it.transactions }
        assertEquals(3, expGrouped.size) // 2, 4, 5

        // WALLET
        val walGrouped = HistoryGrouping.group(all, HistoryPeriod.MONTH, TransactionTypeFilter.WALLET, reimbursedExpenseIds = reimbursedIds).flatMap { it.transactions }
        assertEquals(1, walGrouped.size)
        assertEquals(3L, walGrouped.first().id)

        // OWED (only unreimbursed)
        val owedGrouped = HistoryGrouping.group(all, HistoryPeriod.MONTH, TransactionTypeFilter.OWED, reimbursedExpenseIds = reimbursedIds).flatMap { it.transactions }
        assertEquals(1, owedGrouped.size)
        assertEquals(4L, owedGrouped.first().id)
    }

    @Test
    fun testSortOrder_descendingVsAscending() {
        val date1 = Date(100000L)
        val date2 = Date(200000L)
        val date3 = Date(300000L)

        val txn1 = Transaction(id = 1L, amount = 10.0, type = TransactionType.EXPENSE, categoryId = null, accountType = AccountType.CASH, date = date1, description = "First")
        val txn2 = Transaction(id = 2L, amount = 20.0, type = TransactionType.EXPENSE, categoryId = null, accountType = AccountType.CASH, date = date2, description = "Second")
        val txn3 = Transaction(id = 3L, amount = 30.0, type = TransactionType.EXPENSE, categoryId = null, accountType = AccountType.CASH, date = date3, description = "Third")

        val txns = listOf(txn2, txn1, txn3)

        // DESC: newest first (date3, date2, date1)
        val descGrouped = HistoryGrouping.group(txns, HistoryPeriod.MONTH, TransactionTypeFilter.ALL, sortOrder = TransactionSortOrder.DESC).flatMap { it.transactions }
        assertEquals(3L, descGrouped[0].id)
        assertEquals(2L, descGrouped[1].id)
        assertEquals(1L, descGrouped[2].id)

        // ASC: oldest first (date1, date2, date3)
        val ascGrouped = HistoryGrouping.group(txns, HistoryPeriod.MONTH, TransactionTypeFilter.ALL, sortOrder = TransactionSortOrder.ASC).flatMap { it.transactions }
        assertEquals(1L, ascGrouped[0].id)
        assertEquals(2L, ascGrouped[1].id)
        assertEquals(3L, ascGrouped[2].id)
    }

    @Test
    fun testCategoryFilter() {
        val now = Date()
        val tCat1 = Transaction(id = 1L, amount = 10.0, type = TransactionType.EXPENSE, categoryId = 100L, accountType = AccountType.CASH, date = now, description = "Cat 1")
        val tCat2 = Transaction(id = 2L, amount = 20.0, type = TransactionType.EXPENSE, categoryId = 200L, accountType = AccountType.CASH, date = now, description = "Cat 2")

        val filtered = HistoryGrouping.group(listOf(tCat1, tCat2), HistoryPeriod.MONTH, TransactionTypeFilter.ALL, categoryIdFilter = 100L).flatMap { it.transactions }
        assertEquals(1, filtered.size)
        assertEquals(100L, filtered.first().categoryId)
    }
}
