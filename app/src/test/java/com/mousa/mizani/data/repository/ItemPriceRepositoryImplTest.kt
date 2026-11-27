package com.mousa.mizani.data.repository

import com.mousa.mizani.data.database.dao.ItemPriceHistoryDao
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Locale

/** Stored item names must not depend on the language: Turkish lowercases "I" to a dotless i. */
class ItemPriceRepositoryImplTest {

    private val dao = mock(ItemPriceHistoryDao::class.java)
    private val repository = ItemPriceRepositoryImpl(dao)
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `history lookup normalizes independently of the locale`() = runTest {
        `when`(dao.getHistoryForItem("milk")).thenReturn(emptyFlow())

        repository.getHistoryForItem(" MILK ")

        verify(dao).getHistoryForItem("milk")
    }

    @Test
    fun `latest price lookup normalizes independently of the locale`() = runTest {
        repository.getLatestPriceForItem("MILK")

        verify(dao).getLatestPriceForItem("milk")
    }

    @Test
    fun `name search normalizes independently of the locale`() {
        `when`(dao.searchItemNames("mi")).thenReturn(emptyFlow())

        repository.searchItemNames("MI")

        verify(dao).searchItemNames("mi")
    }
}
