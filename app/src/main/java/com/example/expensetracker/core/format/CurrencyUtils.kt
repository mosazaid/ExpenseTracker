package com.example.expensetracker.core.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val decimalFormat = DecimalFormat("#,##0.000", DecimalFormatSymbols(Locale.US))

    fun formatAmountOnly(amount: Double): String {
        return decimalFormat.format(amount)
    }

    fun formatCurrency(amount: Double): String {
        return "${decimalFormat.format(amount)} JOD"
    }

    fun formatAmount(amount: Double, includeCurrency: Boolean = true): String {
        return if (includeCurrency) formatCurrency(amount) else formatAmountOnly(amount)
    }

    /**
     * Sanitizes amount input by keeping only digits and at most one decimal point.
     */
    fun cleanDecimalInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val firstDotIndex = filtered.indexOf('.')
        return if (firstDotIndex == -1) {
            filtered
        } else {
            filtered.substring(0, firstDotIndex + 1) +
                filtered.substring(firstDotIndex + 1).filter { it.isDigit() }
        }
    }
}

/**
 * Extension functions for Double to format amounts with 3 decimal places and thousands grouping.
 */
fun Double.formatCurrency(): String = CurrencyUtils.formatCurrency(this)
fun Double.formatAmount(includeCurrency: Boolean = true): String = CurrencyUtils.formatAmount(this, includeCurrency)
