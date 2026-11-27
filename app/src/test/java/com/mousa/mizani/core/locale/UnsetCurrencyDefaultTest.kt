package com.mousa.mizani.core.locale

import com.mousa.mizani.core.format.CurrencyUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * The currency of a user who never picked one must not move with the phone's region:
 * it is a label on their existing amounts.
 */
class UnsetCurrencyDefaultTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ar-SA"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `without a saved language the currency stays USD on any phone`() {
        assertEquals("USD", CurrencyUtils.currencyWhenUnset(null))
    }

    @Test
    fun `a saved language keeps the currency it showed before`() {
        assertEquals("USD", CurrencyUtils.currencyWhenUnset("en"))
        assertEquals("JOD", CurrencyUtils.currencyWhenUnset("ar"))
    }
}
