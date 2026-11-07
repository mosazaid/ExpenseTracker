package com.example.expensetracker.glance

import com.example.expensetracker.domain.PacingStatus

data class PaydayPacingWidgetData(
    val liquidCash: Double,
    val daysToPayday: Int,
    val safeDailySpend: Double,
    val todaySpend: Double,
    val pacingStatus: PacingStatus,
    val pacingLabel: String,
    val cycleLabel: String
)

data class QuickAddWidgetData(
    val todaySpend: Double,
    val availableLiquid: Double
)

