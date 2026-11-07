package com.example.expensetracker.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArithmeticEvaluatorTest {

    @Test
    fun testSimpleAddition() {
        val result = ArithmeticEvaluator.evaluate("12.5 + 4.2")
        assertEquals(16.7, result!!, 0.001)
    }

    @Test
    fun testSubtraction() {
        val result = ArithmeticEvaluator.evaluate("50 - 15.5")
        assertEquals(34.5, result!!, 0.001)
    }

    @Test
    fun testMultiplicationAndDivision() {
        val mulResult = ArithmeticEvaluator.evaluate("5 * 2.5")
        assertEquals(12.5, mulResult!!, 0.001)

        val divResult = ArithmeticEvaluator.evaluate("100 / 4")
        assertEquals(25.0, divResult!!, 0.001)
    }

    @Test
    fun testOperatorPrecedence() {
        val result = ArithmeticEvaluator.evaluate("10 + 5 * 2")
        assertEquals(20.0, result!!, 0.001)
    }

    @Test
    fun testUnicodeOperators() {
        val mulResult = ArithmeticEvaluator.evaluate("4 × 2.5")
        assertEquals(10.0, mulResult!!, 0.001)

        val divResult = ArithmeticEvaluator.evaluate("15 ÷ 3")
        assertEquals(5.0, divResult!!, 0.001)
    }

    @Test
    fun testIncompleteExpressionReturnsNull() {
        assertNull(ArithmeticEvaluator.evaluate("12.5 +"))
        assertNull(ArithmeticEvaluator.evaluate("10 *"))
    }

    @Test
    fun testHasOperator() {
        assertTrue(ArithmeticEvaluator.hasOperator("12 + 5"))
        assertTrue(ArithmeticEvaluator.hasOperator("10 * 2"))
        assertFalse(ArithmeticEvaluator.hasOperator("12.500"))
        assertFalse(ArithmeticEvaluator.hasOperator("-5.000"))
    }
}

