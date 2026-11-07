package com.example.expensetracker.domain.islamic

import org.junit.Assert.assertEquals
import org.junit.Test

class MurabahaCalculatorTest {

    @Test
    fun testMurabahaStandardFinancing() {
        val assetPrice = 15000.0 // 15,000 JOD car
        val downPayment = 3000.0  // 3,000 JOD down
        val profitRate = 5.0      // 5% per year
        val tenorYears = 5        // 5 years

        val result = MurabahaCalculator.calculate(
            assetPrice = assetPrice,
            downPayment = downPayment,
            profitRatePercent = profitRate,
            tenorYears = tenorYears
        )

        // Financed Principal = 15,000 - 3,000 = 12,000 JOD
        assertEquals(12000.0, result.financedPrincipal, 0.001)

        // Total Profit = 12,000 * 5% * 5 years = 3,000 JOD
        assertEquals(3000.0, result.totalProfit, 0.001)

        // Total Repayment = 12,000 + 3,000 = 15,000 JOD
        assertEquals(15000.0, result.totalRepayment, 0.001)

        // Monthly Installment = 15,000 / 60 months = 250.0 JOD
        assertEquals(250.0, result.monthlyInstallment, 0.001)

        // Profit Ratio = 3,000 / 12,000 = 25.0%
        assertEquals(25.0, result.profitRatioPercent, 0.001)
    }

    @Test
    fun testMurabahaZeroDownPayment() {
        val assetPrice = 1000.0
        val downPayment = 0.0
        val profitRate = 6.0
        val tenorYears = 1 // 12 months

        val result = MurabahaCalculator.calculate(
            assetPrice = assetPrice,
            downPayment = downPayment,
            profitRatePercent = profitRate,
            tenorYears = tenorYears
        )

        assertEquals(1000.0, result.financedPrincipal, 0.001)
        assertEquals(60.0, result.totalProfit, 0.001)
        assertEquals(1060.0, result.totalRepayment, 0.001)
        assertEquals(1060.0 / 12.0, result.monthlyInstallment, 0.01)
    }
}

