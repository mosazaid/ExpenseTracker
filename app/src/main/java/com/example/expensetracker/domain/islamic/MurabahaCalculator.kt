package com.example.expensetracker.domain.islamic

import kotlin.math.max

data class MurabahaResult(
    val assetPrice: Double,
    val downPayment: Double,
    val financedPrincipal: Double,
    val profitRatePercent: Double,
    val tenorYears: Int,
    val totalProfit: Double,
    val totalRepayment: Double,
    val monthlyInstallment: Double,
    val profitRatioPercent: Double
)

object MurabahaCalculator {

    fun calculate(
        assetPrice: Double,
        downPayment: Double,
        profitRatePercent: Double,
        tenorYears: Int
    ): MurabahaResult {
        val safePrice = max(0.0, assetPrice)
        val safeDown = downPayment.coerceIn(0.0, safePrice)
        val financedPrincipal = safePrice - safeDown
        val safeTenor = max(1, tenorYears)

        // Flat Murabaha Profit calculation standard across Islamic banking in Jordan/GCC
        val totalProfit = financedPrincipal * (profitRatePercent / 100.0) * safeTenor
        val totalRepayment = financedPrincipal + totalProfit
        val monthlyInstallment = if (safeTenor > 0) totalRepayment / (safeTenor * 12) else 0.0

        val profitRatio = if (financedPrincipal > 0) (totalProfit / financedPrincipal) * 100.0 else 0.0

        return MurabahaResult(
            assetPrice = safePrice,
            downPayment = safeDown,
            financedPrincipal = financedPrincipal,
            profitRatePercent = profitRatePercent,
            tenorYears = safeTenor,
            totalProfit = totalProfit,
            totalRepayment = totalRepayment,
            monthlyInstallment = monthlyInstallment,
            profitRatioPercent = profitRatio
        )
    }
}

