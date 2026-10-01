package com.example.expensetracker.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class PrayerTimesCalculatorTest {

    @Test
    fun testPrayerTimesAndQiblahCalculation() {
        val amman = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.startsWith("Amman") }
        val schedule = PrayerTimesCalculator.calculateDaySchedule(amman, Date())

        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)

        // Amman Qiblah is southward towards Makkah (~160°-165°)
        assertTrue(schedule.qiblahBearingDegrees in 150f..175f)
        assertTrue(schedule.nextPrayerName.isNotBlank())
    }
}
