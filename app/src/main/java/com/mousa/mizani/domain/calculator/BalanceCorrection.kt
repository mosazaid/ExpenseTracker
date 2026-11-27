package com.mousa.mizani.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode

/** What saving a typed balance would do. Only [Change] may be saved. */
sealed interface BalanceCorrection {
    data object Invalid : BalanceCorrection
    data object Unchanged : BalanceCorrection
    data class Change(val old: Double, val new: Double, val difference: Double) : BalanceCorrection
}

/**
 * [input] is the raw text of the field. Pass it unfiltered, and do not use
 * `CurrencyUtils.cleanDecimalInput`: dropping a character can change the amount ("12,5" would
 * become 125). Text that cannot be read as typed is [BalanceCorrection.Invalid].
 *
 * [decimals] is the number of decimals the balance is shown with: two amounts that are equal at
 * that precision are [BalanceCorrection.Unchanged].
 */
fun evaluateBalanceCorrection(input: String, current: Double, decimals: Int): BalanceCorrection {
    val new = parseAmount(input)
    if (new == null || !new.isFinite() || !current.isFinite()) return BalanceCorrection.Invalid
    if (new.asShown(decimals).compareTo(current.asShown(decimals)) == 0) {
        return BalanceCorrection.Unchanged
    }
    return BalanceCorrection.Change(old = current, new = new, difference = new - current)
}

private const val ARABIC_DECIMAL_SEPARATOR = '٫'
private val ARABIC_INDIC_DIGITS = '٠'..'٩'
private val EXTENDED_ARABIC_INDIC_DIGITS = '۰'..'۹'

// ASCII digits with at most one decimal point and at least one digit.
private val PLAIN_AMOUNT = Regex("""[0-9]+(\.[0-9]*)?|\.[0-9]+""")

private fun parseAmount(input: String): Double? {
    val text = input.trim().map(::toAsciiAmountChar).joinToString("")
    return if (PLAIN_AMOUNT.matches(text)) text.toDouble() else null
}

private fun toAsciiAmountChar(char: Char): Char = when (char) {
    in ARABIC_INDIC_DIGITS -> '0' + (char - ARABIC_INDIC_DIGITS.first)
    in EXTENDED_ARABIC_INDIC_DIGITS -> '0' + (char - EXTENDED_ARABIC_INDIC_DIGITS.first)
    ARABIC_DECIMAL_SEPARATOR -> '.'
    else -> char
}

// Rounds the shortest decimal form of the value (valueOf, not the exact binary expansion),
// so 1.015 rounds to 1.02.
private fun Double.asShown(decimals: Int): BigDecimal =
    BigDecimal.valueOf(this).setScale(decimals, RoundingMode.HALF_EVEN)
