package com.mousa.mizani.glance

import com.mousa.mizani.domain.calculator.PacingStatus

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

