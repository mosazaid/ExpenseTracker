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
        assertEquals(PrayerCalculationMethod.SINGAPORE, PrayerCalculationMethod.autoDetect("PH"))
        assertEquals(PrayerCalculationMethod.SINGAPORE, PrayerCalculationMethod.autoDetect("ID"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect("AT"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect("DE"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect("FR"))
        assertEquals(PrayerCalculationMethod.TEHRAN, PrayerCalculationMethod.autoDetect("IR"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect("GB"))
        assertEquals(PrayerCalculationMethod.MWL, PrayerCalculationMethod.autoDetect(null))
    }

    @Test
    fun testAustriaViennaCalculation() {
        val vienna = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.contains("Vienna, Austria") }
        val schedule = PrayerTimesCalculator.calculateDaySchedule(vienna, Date(), PrayerCalculationMethod.MWL)
        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)
        // Vienna Qiblah is South-East towards Makkah (~130° - 145°)
        assertTrue(schedule.qiblahBearingDegrees in 125f..150f)
    }

    @Test
    fun testPhilippinesManilaCalculation() {
        val manila = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.contains("Manila, Philippines") }
        val schedule = PrayerTimesCalculator.calculateDaySchedule(manila, Date(), PrayerCalculationMethod.SINGAPORE)
        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)
        // Manila Qiblah is West-Northwest towards Makkah (~285° - 300°)
        assertTrue(schedule.qiblahBearingDegrees in 280f..305f)
    }

    @Test
    fun testMalaysiaKualaLumpurCalculation() {
        val kl = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.contains("Kuala Lumpur, Malaysia") }
        val schedule = PrayerTimesCalculator.calculateDaySchedule(kl, Date(), PrayerCalculationMethod.SINGAPORE)
        assertNotNull(schedule.fajr)
        assertNotNull(schedule.dhuhr)
        assertNotNull(schedule.asr)
        assertNotNull(schedule.maghrib)
        assertNotNull(schedule.isha)
        // Kuala Lumpur Qiblah is West-Northwest towards Makkah (~290° - 300°)
        assertTrue(schedule.qiblahBearingDegrees in 285f..305f)
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
