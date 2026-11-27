package com.mousa.mizani.domain

import com.mousa.mizani.core.export.CsvImportFormat
import com.mousa.mizani.core.export.CsvImporter
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class CsvImportTemplateTest {

    @Test
    fun `template has exactly one non-comment line and it is the header`() {
        val lines = CsvImportFormat.template().lines()

        assertEquals(
            listOf(CsvImportFormat.HEADER),
            lines.filter { it.isNotBlank() && !it.startsWith("#") }
        )
    }

    @Test
    fun `example row is present as a comment`() {
        val lines = CsvImportFormat.template().lines()

        assertTrue(lines.contains("# " + CsvImportFormat.EXAMPLE_ROW))
    }

    @Test
    fun `importing the unchanged template adds nothing`() = runTest {
        val transactionRepository = Mockito.mock(ITransactionRepository::class.java)
        val categoryRepository = Mockito.mock(ICategoryRepository::class.java)
        Mockito.`when`(categoryRepository.getAllCategoriesSnapshot()).thenReturn(emptyList())
        val importer = CsvImporter(transactionRepository, categoryRepository)

        val result = importer.importTransactions(CsvImportFormat.template())

        assertEquals(0, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(emptyList<String>(), result.errors)
        // No interaction at all, so insertTransaction was never called.
        Mockito.verifyNoInteractions(transactionRepository)
    }

    @Test
    fun `importing the template with a byte order mark adds nothing`() = runTest {
        val transactionRepository = Mockito.mock(ITransactionRepository::class.java)
        val categoryRepository = Mockito.mock(ICategoryRepository::class.java)
        Mockito.`when`(categoryRepository.getAllCategoriesSnapshot()).thenReturn(emptyList())
        val importer = CsvImporter(transactionRepository, categoryRepository)

        // The shared file starts with a BOM.
        val result = importer.importTransactions("\uFEFF" + CsvImportFormat.template())

        assertEquals(0, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(emptyList<String>(), result.errors)
        Mockito.verifyNoInteractions(transactionRepository)
    }
}
