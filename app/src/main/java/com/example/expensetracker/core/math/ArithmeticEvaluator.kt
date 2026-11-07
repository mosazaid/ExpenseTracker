package com.example.expensetracker.core.math

import java.util.Locale

object ArithmeticEvaluator {

    private val OPERATORS = setOf('+', '-', '*', '×', '/', '÷')

    fun hasOperator(input: String): Boolean {
        // Must contain an operator that isn't just a leading negative sign
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        val checkStr = if (trimmed.startsWith("-") || trimmed.startsWith("+")) trimmed.substring(1) else trimmed
        return checkStr.any { it in OPERATORS }
    }

    /**
     * Sanitizes arithmetic input, allowing digits, decimal points, spaces, and math operators.
     */
    fun cleanArithmeticInput(input: String): String {
        return input.filter { it.isDigit() || it == '.' || it in OPERATORS || it.isWhitespace() }
    }

    /**
     * Safely evaluates simple binary or chained arithmetic expressions.
     * Supports +, -, *, /, ×, ÷.
     * Returns null if the expression is invalid or incomplete (e.g. "12.5 +").
     */
    fun evaluate(expression: String): Double? {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isEmpty()) return null
        if (sanitized.last() in setOf('+', '-', '*', '/')) return null

        return try {
            parseExpression(sanitized)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Basic recursive descent parser for +, -, *, / with operator precedence.
     */
    private fun parseExpression(str: String): Double {
        var pos = -1
        var ch = ' '

        fun nextChar() {
            pos++
            ch = if (pos < str.length) str[pos] else '\u0000'
        }

        fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression(str)
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                val numStr = str.substring(startPos, pos)
                x = numStr.toDouble()
            } else {
                throw IllegalArgumentException("Unexpected: $ch")
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*')) x *= parseFactor()
                else if (eat('/')) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                } else return x
            }
        }

        fun parse(): Double {
            nextChar()
            var x = parseTerm()
            while (true) {
                if (eat('+')) x += parseTerm()
                else if (eat('-')) x -= parseTerm()
                else return x
            }
        }

        return parse()
    }

    fun formatResult(value: Double, decimals: Int = 3): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }
}

