package com.example.expensetracker.glance

import com.example.expensetracker.domain.PacingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlanceWidgetTest {

    @Test
    fun testPaydayPacingWidgetData_properties() {
        val data = PaydayPacingWidgetData(
            liquidCash = 1420.500,
            daysToPayday = 12,
            safeDailySpend = 35.000,
            todaySpend = 12.350,
            pacingStatus = PacingStatus.OPTIMAL,
            pacingLabel = "On Track",
            cycleLabel = "Oct 2026"
        )

        assertEquals(1420.500, data.liquidCash, 0.001)
        assertEquals(12, data.daysToPayday)
        assertEquals(35.000, data.safeDailySpend, 0.001)
        assertEquals(12.350, data.todaySpend, 0.001)
        assertEquals(PacingStatus.OPTIMAL, data.pacingStatus)
        assertEquals("On Track", data.pacingLabel)
        assertEquals("Oct 2026", data.cycleLabel)
    }

    @Test
    fun testQuickAddWidgetData_properties() {
        val data = QuickAddWidgetData(
            todaySpend = 14.500,
            availableLiquid = 850.000
        )

        assertEquals(14.500, data.todaySpend, 0.001)
        assertEquals(850.000, data.availableLiquid, 0.001)
        assertTrue(data.availableLiquid > data.todaySpend)
    }
}

