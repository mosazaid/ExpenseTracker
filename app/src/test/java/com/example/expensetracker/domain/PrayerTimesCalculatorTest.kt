package com.example.expensetracker.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class PrayerTimesCalculatorTest {

    @Test
    fun testPrayerTimesAndQiblahCalculation() {
        val amman = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.startsWith("Amman") }
        val schedule = PrayerTimesCalculator.calculateDaySchedule(amman, Date(), PrayerCalculationMethod.JORDAN)

        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)

        // Amman Qiblah is southward towards Makkah (~155°-165°)
        assertTrue(schedule.qiblahBearingDegrees in 150f..175f)
        assertTrue(schedule.nextPrayerName.isNotBlank())
        assertEquals(PrayerCalculationMethod.JORDAN, schedule.calculationMethod)
    }

    @Test
    fun testAutoDetectCalculationMethod() {
        assertEquals(PrayerCalculationMethod.JORDAN, PrayerCalculationMethod.autoDetect("JO"))
        assertEquals(PrayerCalculationMethod.UMM_AL_QURA, PrayerCalculationMethod.autoDetect("SA"))
        assertEquals(PrayerCalculationMethod.EGYPT, PrayerCalculationMethod.autoDetect("EG"))
        assertEquals(PrayerCalculationMethod.TURKEY, PrayerCalculationMethod.autoDetect("TR"))
        assertEquals(PrayerCalculationMethod.KARACHI, PrayerCalculationMethod.autoDetect("PK"))
        assertEquals(PrayerCalculationMethod.ISNA, PrayerCalculationMethod.autoDetect("US"))
        assertEquals(PrayerCalculationMethod.ISNA, PrayerCalculationMethod.autoDetect("CA"))
        assertEquals(PrayerCalculationMethod.DUBAI, PrayerCalculationMethod.autoDetect("AE"))
        assertEquals(PrayerCalculationMethod.KUWAIT, PrayerCalculationMethod.autoDetect("KW"))
        assertEquals(PrayerCalculationMethod.QATAR, PrayerCalculationMethod.autoDetect("QA"))
        assertEquals(PrayerCalculationMethod.SINGAPORE, PrayerCalculationMethod.autoDetect("SG"))
        assertEquals(PrayerCalculationMethod.SINGAPORE, PrayerCalculationMethod.autoDetect("MY"))
        assertEquals(PrayerCalculationMethod.TEHRAN, PrayerCalculationMethod.autoDetect("IR"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect("GB"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect(null))
    }

    @Test
    fun testDifferentMethodsProduceValidTimes() {
        val amman = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.startsWith("Amman") }
        val date = Date()

        val methods = listOf(
            PrayerCalculationMethod.JORDAN,
            PrayerCalculationMethod.UMM_AL_QURA,
            PrayerCalculationMethod.EGYPT,
            PrayerCalculationMethod.MWL
        )

        methods.forEach { method ->
            val schedule = PrayerTimesCalculator.calculateDaySchedule(amman, date, method)
            assertNotNull(schedule.fajr)
            assertNotNull(schedule.isha)
            assertTrue(schedule.fajr.contains(":"))
            assertTrue(schedule.isha.contains(":"))
        }
    }
}
