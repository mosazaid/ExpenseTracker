package com.mousa.mizani.domain.usecase

import com.mousa.mizani.domain.calculator.MonthFinancialSummary
import com.mousa.mizani.domain.calculator.BalanceCalculator
import java.util.Date
import javax.inject.Inject

/**
 * Clean Architecture Use Case: Get Financial Summary
 * Orchestrates period bounds, balances, and loan impact.
 */
class GetFinancialSummaryUseCase @Inject constructor(
    private val balanceCalculator: BalanceCalculator
) {
    suspend operator fun invoke(startDate: Date, endDate: Date): MonthFinancialSummary {
        return balanceCalculator.buildMonthFinancialSummary(startDate, endDate)
    }
}
