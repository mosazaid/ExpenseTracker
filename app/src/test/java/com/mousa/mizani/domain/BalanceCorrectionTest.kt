package com.mousa.mizani.domain

import com.mousa.mizani.domain.calculator.BalanceCorrection
import com.mousa.mizani.domain.calculator.evaluateBalanceCorrection
import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceCorrectionTest {

    @Test
    fun `empty, dot and 400 nines are Invalid`() {
        listOf("", ".", "9".repeat(400)).forEach { input ->
            assertEquals(BalanceCorrection.Invalid, evaluateBalanceCorrection(input, 5.0, 2))
        }
    }

    @Test
    fun `text that would have to be altered to be read is Invalid`() {
        listOf(
            "12,5", "1,000.50", "-50", "+5", "1 000", "1e5", "1.2.3", ".",
            "12،5", // Arabic comma
            "1٬000", // Arabic thousands separator
            "١٫٢.٥", // Arabic and ASCII decimal separators together
            "12a", "NaN", "Infinity", "5d", "0x1p3", "   ",
            "５" // full-width digit
        ).forEach { input ->
            assertEquals(input, BalanceCorrection.Invalid, evaluateBalanceCorrection(input, 5.0, 2))
        }
    }

    @Test
    fun `Arabic-Indic digits with the Arabic decimal separator parse`() {
        // 12.5 written as U+0661 U+0662 U+066B U+0665
        assertNewAmount(12.5, "١٢٫٥")
        assertNewAmount(9876543210.0, "٩٨٧٦٥٤٣٢١٠")
    }

    @Test
    fun `Extended Arabic-Indic digits parse`() {
        assertNewAmount(9876543210.0, "۹۸۷۶۵۴۳۲۱۰")
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertNewAmount(7.0, " 7 ")
    }

    @Test
    fun `a leading or trailing decimal point is accepted`() {
        assertNewAmount(0.5, ".5")
        assertNewAmount(12.0, "12.")
    }

    @Test
    fun `amounts are rounded from their shortest decimal form`() {
        assertEquals(BalanceCorrection.Unchanged, evaluateBalanceCorrection("1.02", 1.015, 2))
        assertEquals(
            BalanceCorrection.Change::class.java,
            evaluateBalanceCorrection("1.01", 1.015, 2).javaClass
        )
    }

    private fun assertNewAmount(expected: Double, input: String) {
        val result = evaluateBalanceCorrection(input, 5.0, 2)

        assertEquals(BalanceCorrection.Change(5.0, expected, expected - 5.0), result)
    }

    @Test
    fun `a non-finite current balance is Invalid`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { current ->
            assertEquals(BalanceCorrection.Invalid, evaluateBalanceCorrection("5", current, 2))
        }
    }

    @Test
    fun `007 against 5 is Change with new 7`() {
        assertEquals(
            BalanceCorrection.Change(old = 5.0, new = 7.0, difference = 2.0),
            evaluateBalanceCorrection("007", 5.0, 2)
        )
    }

    @Test
    fun `10_001 against 10 is Unchanged at 2 decimals and Change at 3`() {
        assertEquals(BalanceCorrection.Unchanged, evaluateBalanceCorrection("10.001", 10.0, 2))

        val atThree = evaluateBalanceCorrection("10.001", 10.0, 3) as BalanceCorrection.Change
        assertEquals(10.001, atThree.new, 0.0)
    }

    @Test
    fun `floating point noise in the current balance is Unchanged`() {
        assertEquals(BalanceCorrection.Unchanged, evaluateBalanceCorrection("0.3", 0.1 + 0.2, 3))
    }

    @Test
    fun `zero against a balance shown as negative zero is Unchanged`() {
        assertEquals(BalanceCorrection.Unchanged, evaluateBalanceCorrection("0", -0.0000001, 2))
    }

    @Test
    fun `current -50 with 20 has difference 70`() {
        val result = evaluateBalanceCorrection("20", -50.0, 2) as BalanceCorrection.Change

        assertEquals(-50.0, result.old, 0.0)
        assertEquals(20.0, result.new, 0.0)
        assertEquals(70.0, result.difference, 0.0)
    }
}
