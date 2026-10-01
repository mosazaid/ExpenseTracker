package com.example.expensetracker.domain

object ZakahCalculator {

    const val DEFAULT_GOLD_GRAMS_NISAB = 85.0
    const val DEFAULT_GOLD_PRICE_PER_GRAM = 55.0 // Approximate default reference in local currency

    data class ZakahAssessment(
        val totalCashAndBank: Double,
        val shortTermLiabilities: Double,
        val netZakatableWealth: Double,
        val nisabThreshold: Double,
        val isNisabReached: Boolean,
        val zakahDue: Double
    )

    fun calculateZakah(
        cashBalance: Double,
        bankBalance: Double,
        otherAssets: Double = 0.0,
        immediateDebts: Double = 0.0,
        goldPricePerGram: Double = DEFAULT_GOLD_PRICE_PER_GRAM,
        nisabGrams: Double = DEFAULT_GOLD_GRAMS_NISAB
    ): ZakahAssessment {
        val totalAssets = (cashBalance.coerceAtLeast(0.0) + bankBalance.coerceAtLeast(0.0) + otherAssets.coerceAtLeast(0.0))
        val netWealth = (totalAssets - immediateDebts.coerceAtLeast(0.0)).coerceAtLeast(0.0)
        val nisab = (goldPricePerGram * nisabGrams).coerceAtLeast(1.0)
        val isReached = netWealth >= nisab
        val zakahDue = if (isReached) netWealth * 0.025 else 0.0

        return ZakahAssessment(
            totalCashAndBank = totalAssets,
            shortTermLiabilities = immediateDebts,
            netZakatableWealth = netWealth,
            nisabThreshold = nisab,
            isNisabReached = isReached,
            zakahDue = zakahDue
        )
    }
}
