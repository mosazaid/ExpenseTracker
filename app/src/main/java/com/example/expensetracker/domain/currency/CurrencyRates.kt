package com.example.expensetracker.domain.currency

import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String,
    val decimals: Int = 2
)

object CurrencyRates {

    val SUPPORTED_CURRENCIES: List<CurrencyInfo> = listOf(
        CurrencyInfo("JOD", "Jordanian Dinar", "د.أ", "🇯🇴", decimals = 3),
        CurrencyInfo("USD", "US Dollar", "$", "🇺🇸", decimals = 2),
        CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪", decimals = 2),
        CurrencyInfo("SAR", "Saudi Riyal", "ر.س", "🇸🇦", decimals = 2),
        CurrencyInfo("QAR", "Qatari Riyal", "ر.ق", "🇶🇦", decimals = 2),
        CurrencyInfo("KWD", "Kuwaiti Dinar", "د.ك", "🇰🇼", decimals = 3),
        CurrencyInfo("BHD", "Bahraini Dinar", "د.ب", "🇧🇭", decimals = 3),
        CurrencyInfo("OMR", "Omani Rial", "ر.ع", "🇴🇲", decimals = 3),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺", decimals = 2),
        CurrencyInfo("GBP", "British Pound", "£", "🇬🇧", decimals = 2),
        CurrencyInfo("TRY", "Turkish Lira", "₺", "🇹🇷", decimals = 2),
        CurrencyInfo("EGP", "Egyptian Pound", "E£", "🇪🇬", decimals = 2)
    )

    // Pegged / reference rates relative to USD (1.0 USD = X units of currency)
    // Jordan Dinars is pegged to USD at 0.709 JOD = 1 USD
    private val USD_BASE_RATES: Map<String, Double> = mapOf(
        "USD" to 1.0,
        "JOD" to 0.709,
        "AED" to 3.6725,
        "SAR" to 3.75,
        "QAR" to 3.64,
        "KWD" to 0.307,
        "BHD" to 0.376,
        "OMR" to 0.385,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "TRY" to 34.2,
        "EGP" to 49.0
    )

    fun getCurrencyInfo(code: String): CurrencyInfo {
        return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
            ?: CurrencyInfo(code.uppercase(), code.uppercase(), code.uppercase(), "🌐", decimals = 2)
    }

    /**
     * Calculates how many units of [toCurrency] you receive for 1 unit of [fromCurrency].
     * Example: getDefaultRate("JOD", "USD") ≈ 1.4104
     */
    fun getDefaultRate(fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return 1.0

        val fromRate = USD_BASE_RATES[fromCurrency.uppercase()] ?: 1.0
        val toRate = USD_BASE_RATES[toCurrency.uppercase()] ?: 1.0

        // 1 FromCurrency in USD = 1 / fromRate
        // in ToCurrency = (1 / fromRate) * toRate
        return toRate / fromRate
    }

    /**
     * Converts [amount] from [fromCurrency] to [toCurrency].
     * If [customRate] is provided, uses: amount * customRate.
     */
    fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        customRate: Double? = null
    ): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        val rate = customRate ?: getDefaultRate(fromCurrency, toCurrency)
        return amount * rate
    }

    /**
     * Format an amount with appropriate decimal places and symbol.
     */
    fun formatAmount(amount: Double, currencyCode: String): String {
        val info = getCurrencyInfo(currencyCode)
        val formattedNumber = String.format(Locale.US, "%.${info.decimals}f", amount)
        return "$formattedNumber ${info.symbol}"
    }
}

