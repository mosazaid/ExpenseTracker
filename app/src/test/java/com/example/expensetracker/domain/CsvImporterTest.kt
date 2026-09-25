package com.example.expensetracker.domain

import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.SubCategoryDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class CsvImporterTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var subCategoryDao: SubCategoryDao
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var csvImporter: CsvImporter

    @Before
    fun setup() {
        transactionDao = Mockito.mock(TransactionDao::class.java)
        categoryDao = Mockito.mock(CategoryDao::class.java)
        subCategoryDao = Mockito.mock(SubCategoryDao::class.java)
        transactionRepository = TransactionRepository(transactionDao)
        categoryRepository = CategoryRepository(categoryDao, subCategoryDao)
        csvImporter = CsvImporter(transactionRepository, categoryRepository)
    }

    @Test
    fun testEmptyCsvReturnsError() = runTest {
        val result = csvImporter.importTransactions("")
        assertEquals(0, result.inserted)
        assertEquals(0, result.updated)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun testInvalidHeaderReturnsError() = runTest {
        val result = csvImporter.importTransactions("foo,bar,baz\n1,2,3")
        assertEquals(0, result.inserted)
        assertTrue(result.errors.contains("Invalid CSV header"))
    }

    @Test
    fun testValidCsvImportsSuccessfully() = runTest {
        Mockito.`when`(categoryDao.getAllCategories()).thenReturn(
            flowOf(
                listOf(
                    Category(id = 1L, name = "Food", icon = "🍔", color = "#FF0000", type = TransactionType.EXPENSE)
                )
            )
        )

        val csv = """
            date,type,amount,description,subdescription,account,category
            2026-09-25 12:00:00,EXPENSE,25.50,Dinner,Italian,CASH,Food
        """.trimIndent()

        val result = csvImporter.importTransactions(csv)
        assertEquals(1, result.inserted)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun testArabicCsvContentImportsCorrectly() = runTest {
        Mockito.`when`(categoryDao.getAllCategories()).thenReturn(
            flowOf(
                listOf(
                    Category(id = 2L, name = "راتب", icon = "💰", color = "#00FF00", type = TransactionType.INCOME)
                )
            )
        )

        val csv = """
            date,type,amount,description,subdescription,account,category
            2026-09-25 12:00:00,INCOME,1500.00,راتب شهر سبتمبر,مكافأة,BANK,راتب
        """.trimIndent()

        val result = csvImporter.importTransactions(csv)
        assertEquals(1, result.inserted)
        assertEquals(0, result.errors.size)
    }
}
