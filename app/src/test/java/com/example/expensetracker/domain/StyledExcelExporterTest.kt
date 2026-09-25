package com.example.expensetracker.domain

import android.content.Context
import com.example.expensetracker.R
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito
import java.io.File
import java.util.Date

class StyledExcelExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var exporter: StyledExcelExporter

    @Before
    fun setup() {
        context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.getString(R.string.app_name)).thenReturn("ExpenseTracker")
        Mockito.`when`(context.resources).thenReturn(Mockito.mock(android.content.res.Resources::class.java))
        exporter = StyledExcelExporter(context)
    }

    @Test
    fun testExportEmptyRowsWritesValidHtml() {
        val outputFile = tempFolder.newFile("empty_statement.html")
        exporter.exportToFile(emptyList(), "All transactions", outputFile)

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("<!DOCTYPE html>"))
        assertTrue(content.contains("ExpenseTracker"))
        assertTrue(content.contains("No transactions found in this period."))
    }

    @Test
    fun testExportPopulatedRowsWritesDataCorrectly() {
        val outputFile = tempFolder.newFile("statement.html")
        val now = Date()
        val rows = listOf(
            TransactionExportRow(
                id = 1L,
                date = now,
                type = TransactionType.INCOME,
                categoryName = "Salary",
                amount = 2500.0,
                account = AccountType.BANK,
                toAccount = null,
                description = "September Salary",
                subDescription = "Bonus",
                startsNewPeriod = false,
                carriedForwardBalance = null,
                linkedExpenseId = null,
                debtorNote = "",
                awaitingReimbursement = false,
                allowNegativeBalance = false,
                createdAt = now,
                updatedAt = now
            ),
            TransactionExportRow(
                id = 2L,
                date = now,
                type = TransactionType.EXPENSE,
                categoryName = "Groceries",
                amount = 120.50,
                account = AccountType.CASH,
                toAccount = null,
                description = "سوبرماركت",
                subDescription = "",
                startsNewPeriod = false,
                carriedForwardBalance = null,
                linkedExpenseId = null,
                debtorNote = "",
                awaitingReimbursement = false,
                allowNegativeBalance = false,
                createdAt = now,
                updatedAt = now
            )
        )

        exporter.exportToFile(rows, "September 2026", outputFile)

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("September Salary"))
        assertTrue(content.contains("سوبرماركت"))
        assertTrue(content.contains("2,500.00"))
        assertTrue(content.contains("120.50"))
    }
}
