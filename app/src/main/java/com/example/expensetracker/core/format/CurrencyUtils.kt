package com.example.expensetracker.core.format

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val symbolAr: String,
    val decimalPlaces: Int = 3
)

object CurrencyUtils {
    private val decimalFormat3 = DecimalFormat("#,##0.000", DecimalFormatSymbols(Locale.US))
    private val decimalFormat2 = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val decimalFormat0 = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    val SUPPORTED_CURRENCIES = listOf(
        // Middle East & North Africa (Arab Countries)
        CurrencyInfo("JOD", "JOD", "د.أ", 3),
        CurrencyInfo("SAR", "SAR", "ر.س", 2),
        CurrencyInfo("AED", "AED", "د.إ", 2),
        CurrencyInfo("KWD", "KWD", "د.ك", 3),
        CurrencyInfo("QAR", "QAR", "ر.ق", 2),
        CurrencyInfo("BHD", "BHD", "د.ب", 3),
        CurrencyInfo("OMR", "OMR", "ر.ع", 3),
        CurrencyInfo("EGP", "EGP", "ج.م", 2),
        CurrencyInfo("IQD", "IQD", "د.ع", 3),
        CurrencyInfo("LBP", "LBP", "ل.ل", 2),
        CurrencyInfo("SYP", "SYP", "ل.س", 2),
        CurrencyInfo("ILS", "ILS", "₪", 2),
        CurrencyInfo("LYD", "LYD", "د.ل", 3),
        CurrencyInfo("TND", "TND", "د.ت", 3),
        CurrencyInfo("DZD", "DZD", "د.ج", 2),
        CurrencyInfo("MAD", "MAD", "د.م.", 2),
        CurrencyInfo("YER", "YER", "ر.ي", 2),
        CurrencyInfo("SDG", "SDG", "ج.س", 2),

        // Turkey
        CurrencyInfo("TRY", "TL", "₺", 2),

        // Europe
        CurrencyInfo("EUR", "€", "€", 2),
        CurrencyInfo("GBP", "£", "£", 2),
        CurrencyInfo("CHF", "CHF", "CHF", 2),
        CurrencyInfo("SEK", "kr", "kr", 2),
        CurrencyInfo("NOK", "kr", "kr", 2),
        CurrencyInfo("DKK", "kr", "kr", 2),
        CurrencyInfo("PLN", "zł", "zł", 2),
        CurrencyInfo("BAM", "KM", "KM", 2),
        CurrencyInfo("CZK", "Kč", "Kč", 2),
        CurrencyInfo("HUF", "Ft", "Ft", 0),
        CurrencyInfo("RON", "lei", "lei", 2),

        // Americas, Asia & Oceania
        CurrencyInfo("USD", "$", "$", 2),
        CurrencyInfo("CAD", "CA$", "CA$", 2),
        CurrencyInfo("AUD", "A$", "A$", 2),
        CurrencyInfo("JPY", "¥", "¥", 0),
        CurrencyInfo("CNY", "CN¥", "CN¥", 2),
        CurrencyInfo("INR", "₹", "₹", 2),
        CurrencyInfo("PKR", "₨", "₨", 2),
        CurrencyInfo("MYR", "RM", "RM", 2),
        CurrencyInfo("SGD", "S$", "S$", 2),
        CurrencyInfo("PHP", "₱", "₱", 2),
        CurrencyInfo("IDR", "Rp", "Rp", 0),
        CurrencyInfo("THB", "฿", "฿", 2),
        CurrencyInfo("BRL", "R$", "R$", 2),
        CurrencyInfo("ZAR", "R", "R", 2)
    )

    @Volatile
    var activeCurrencyCode: String = "JOD"

    fun detectDefaultCurrency(): String {
        return try {
            when (Locale.getDefault().country.uppercase()) {
                "JO" -> "JOD"
                "SA" -> "SAR"
                "AE" -> "AED"
                "KW" -> "KWD"
                "QA" -> "QAR"
                "BH" -> "BHD"
                "OM" -> "OMR"
                "EG" -> "EGP"
                "IQ" -> "IQD"
                "LB" -> "LBP"
                "SY" -> "SYP"
                "PS", "IL" -> "ILS"
                "LY" -> "LYD"
                "TN" -> "TND"
                "DZ" -> "DZD"
                "MA" -> "MAD"
                "YE" -> "YER"
                "SD" -> "SDG"
                "TR" -> "TRY"
                "US" -> "USD"
                "GB" -> "GBP"
                "CA" -> "CAD"
                "AU" -> "AUD"
                "CH" -> "CHF"
                "SE" -> "SEK"
                "NO" -> "NOK"
                "DK" -> "DKK"
                "PL" -> "PLN"
                "JP" -> "JPY"
                "CN" -> "CNY"
                "IN" -> "INR"
                "PK" -> "PKR"
                "MY" -> "MYR"
                "SG" -> "SGD"
                "PH" -> "PHP"
                "ID" -> "IDR"
                "TH" -> "THB"
                "BA" -> "BAM"
                "CZ" -> "CZK"
                "HU" -> "HUF"
                "RO" -> "RON"
                "BR" -> "BRL"
                "ZA" -> "ZAR"
                "FR", "DE", "IT", "ES", "NL", "BE", "AT", "PT", "FI", "IE", "GR" -> "EUR"
                else -> if (Locale.getDefault().language == "ar") "JOD" else "USD"
            }
        } catch (_: Exception) {
            "JOD"
        }
    }

    fun getCurrencyInfo(code: String = activeCurrencyCode): CurrencyInfo {
        return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
            ?: CurrencyInfo(code, code, code, 2)
    }

    fun formatAmountOnly(amount: Double, code: String = activeCurrencyCode): String {
        val info = getCurrencyInfo(code)
        return when (info.decimalPlaces) {
            3 -> decimalFormat3.format(amount)
            0 -> decimalFormat0.format(amount)
            else -> decimalFormat2.format(amount)
        }
    }

    fun formatCurrency(amount: Double, code: String = activeCurrencyCode): String {
        val info = getCurrencyInfo(code)
        val formattedNumber = if (info.decimalPlaces == 3) {
            decimalFormat3.format(amount)
        } else {
            decimalFormat2.format(amount)
        }
        return "$formattedNumber ${info.code}"
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
 * Extension functions for Double to format amounts.
 */
fun Double.formatCurrency(): String = CurrencyUtils.formatCurrency(this)
fun Double.formatAmount(includeCurrency: Boolean = true): String = CurrencyUtils.formatAmount(this, includeCurrency)
