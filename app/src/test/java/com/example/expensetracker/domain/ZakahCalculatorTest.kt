package com.example.expensetracker.domain

import org.junit.Assert.*
import org.junit.Test

class ZakahCalculatorTest {

    @Test
    fun testZakahCalculation_BelowNisab() {
        val result = ZakahCalculator.calculateZakah(
            cashBalance = 500.0,
            bankBalance = 1000.0,
            otherAssets = 0.0,
            immediateDebts = 200.0,
            goldPricePerGram = 55.0
        )

        // Nisab threshold = 85 * 55 = 4675.0
        // Net wealth = 1300.0
        assertEquals(4675.0, result.nisabThreshold, 0.01)
        assertEquals(1300.0, result.netZakatableWealth, 0.01)
        assertFalse(result.isNisabReached)
        assertEquals(0.0, result.zakahDue, 0.001)
    }

    @Test
    fun testZakahCalculation_AboveNisab() {
        val result = ZakahCalculator.calculateZakah(
            cashBalance = 2000.0,
            bankBalance = 5000.0,
            otherAssets = 1000.0,
            immediateDebts = 500.0,
            goldPricePerGram = 50.0
        )

        // Nisab = 85 * 50 = 4250.0
        // Net wealth = 7500.0
        // Zakah due = 7500.0 * 0.025 = 187.5
        assertEquals(4250.0, result.nisabThreshold, 0.01)
        assertEquals(7500.0, result.netZakatableWealth, 0.01)
        assertTrue(result.isNisabReached)
        assertEquals(187.5, result.zakahDue, 0.01)
    }
}
