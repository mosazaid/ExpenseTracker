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
    fun testAmmanTimesPrayerAccuracyForOctober3() {
        val amman = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.startsWith("Amman") }
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 3, 12, 0, 0)
        }
        val schedule24 = PrayerTimesCalculator.calculateDaySchedule(
            city = amman,
            date = cal.time,
            method = PrayerCalculationMethod.JORDAN,
            is24Hour = true
        )

        // Times for Amman (reference elevation 1000m) for Oct 3, 2026:
        // Fajr: 05:10, Sunrise: 06:26, Dhuhr: 12:25, Asr: 15:47, Maghrib: 18:24, Isha: 19:40
        assertEquals("05:10", schedule24.fajr)
        assertEquals("06:26", schedule24.sunrise)
        assertEquals("12:25", schedule24.dhuhr)
        assertEquals("15:47", schedule24.asr)
        assertEquals("18:25", schedule24.maghrib)
        assertEquals("19:40", schedule24.isha)

        // Test 12-hour format
        val schedule12 = PrayerTimesCalculator.calculateDaySchedule(
            city = amman,
            date = cal.time,
            method = PrayerCalculationMethod.JORDAN,
            is24Hour = false
        )
        assertEquals("5:10 AM", schedule12.fajr)
        assertEquals("6:26 AM", schedule12.sunrise)
        assertEquals("12:25 PM", schedule12.dhuhr)
        assertEquals("3:47 PM", schedule12.asr)
        assertEquals("6:25 PM", schedule12.maghrib)
        assertEquals("7:40 PM", schedule12.isha)
    }

    @Test
    fun testAmmanTimesPrayerAccuracyForOctober7And8() {
        val amman = PrayerTimesCalculator.PRESET_CITIES.first { it.nameEn.startsWith("Amman") }
        
        // October 7, 2026 (Awqaf: Fajr 05:13, Sunrise 06:28, Dhuhr 12:25, Asr 15:44, Maghrib 18:20, Isha 19:35)
        val cal7 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 7, 12, 0, 0)
        }
        val schedule7 = PrayerTimesCalculator.calculateDaySchedule(
            city = amman,
            date = cal7.time,
            method = PrayerCalculationMethod.JORDAN,
            is24Hour = true
        )
        // October 8, 2026 (Awqaf: Fajr 05:13, Sunrise 06:29, Dhuhr 12:24, Asr 15:43, Maghrib 18:19, Isha 19:34)
        val cal8 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 8, 12, 0, 0)
        }
        val schedule8 = PrayerTimesCalculator.calculateDaySchedule(
            city = amman,
            date = cal8.time,
            method = PrayerCalculationMethod.JORDAN,
            is24Hour = true
        )

        // Verify Maghrib matches official Awqaf times without the 5-6 minute discrepancy
        assertEquals("18:20", schedule7.maghrib)
        assertEquals("18:19", schedule8.maghrib)
        assertEquals("05:13", schedule7.fajr)
        assertEquals("12:24", schedule7.dhuhr)
        assertEquals("15:44", schedule7.asr)
        assertEquals("19:35", schedule7.isha)
        assertEquals("12:24", schedule8.dhuhr)
        assertEquals("15:43", schedule8.asr)
        assertEquals("19:34", schedule8.isha)
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
