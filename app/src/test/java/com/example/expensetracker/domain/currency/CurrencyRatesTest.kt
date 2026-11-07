package com.example.expensetracker.domain.currency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyRatesTest {

    @Test
    fun testSameCurrencyConversion() {
        val converted = CurrencyRates.convert(100.0, "JOD", "JOD")
        assertEquals(100.0, converted, 0.001)
    }

    @Test
    fun testJODtoUSDConversion() {
        // 1 JOD should equal ~1.41 USD (1 / 0.709)
        val rate = CurrencyRates.getDefaultRate("JOD", "USD")
        assertEquals(1.4104, rate, 0.01)

        val usdAmount = CurrencyRates.convert(500.0, "JOD", "USD")
        assertEquals(705.21, usdAmount, 0.5)
    }

    @Test
    fun testAEDtoJODConversion() {
        // AED to JOD
        val rate = CurrencyRates.getDefaultRate("AED", "JOD")
        assertTrue("AED to JOD rate should be approx 0.193", rate in 0.19..0.20)

        val jodAmount = CurrencyRates.convert(1000.0, "AED", "JOD")
        assertTrue("1000 AED should be ~193 JOD", jodAmount in 190.0..196.0)
    }

    @Test
    fun testCustomRateConversion() {
        val customRate = 1.42
        val usdAmount = CurrencyRates.convert(500.0, "JOD", "USD", customRate = customRate)
        assertEquals(710.0, usdAmount, 0.001)
    }

    @Test
    fun testFormatCurrency() {
        val formattedJod = CurrencyRates.formatAmount(50.5, "JOD")
        assertTrue(formattedJod.contains("50.500") && formattedJod.contains("د.أ"))

        val formattedUsd = CurrencyRates.formatAmount(99.99, "USD")
        assertTrue(formattedUsd.contains("99.99") && formattedUsd.contains("$"))
    }
}

