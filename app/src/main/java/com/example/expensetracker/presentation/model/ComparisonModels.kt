package com.example.expensetracker.presentation.model

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.domain.PeriodBounds
import java.util.Date

enum class ComparisonMode(val label: String) {
    DAY_NORMALIZED("Day-to-Day Normalized"),
    FULL_PERIOD("Full Month")
}

data class PeriodOption(
    val key: String,
    val label: String,
    val bounds: PeriodBounds
)

data class SubcategoryComparisonItem(
    val name: String,
    val baseAmount: Double,
    val compareAmount: Double,
    val deltaAmount: Double,
    val deltaPercent: Float?,
    val baseTxnCount: Int,
    val varianceInsight: String? = null
)

data class CategoryComparisonItem(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val baseTotal: Double,
    val compareTotal: Double,
    val deltaAmount: Double,
    val deltaPercent: Float?,
    val subcategories: List<SubcategoryComparisonItem>,
    val topSpikeInsight: String? = null
)

data class DrillDownTransactionItem(
    val id: Long,
    val title: String,
    val note: String?,
    val amount: Double,
    val date: Date,
    val accountType: AccountType,
    val isSplit: Boolean,
    val parentAmount: Double? = null
)

data class SubcategoryDrillDownData(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val subcategoryName: String,
    val periodLabel: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val transactions: List<DrillDownTransactionItem>
)

data class AdvancedComparisonData(
    val basePeriod: PeriodBounds,
    val comparePeriod: PeriodBounds,
    val mode: ComparisonMode,
    val cutoffDay: Int?,
    val baseTotalExpense: Double,
    val compareTotalExpense: Double,
    val overallDeltaPercent: Float?,
    val categories: List<CategoryComparisonItem>,
    val availablePeriods: List<PeriodOption> = emptyList()
)

