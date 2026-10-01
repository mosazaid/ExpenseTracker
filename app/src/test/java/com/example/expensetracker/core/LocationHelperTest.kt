package com.example.expensetracker.core

import com.example.expensetracker.core.location.LocationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationHelperTest {

    @Test
    fun testCountryToCurrencyMapping() {
        assertEquals("JOD", LocationHelper.mapCountryToCurrencyCode("JO"))
        assertEquals("SAR", LocationHelper.mapCountryToCurrencyCode("SA"))
        assertEquals("AED", LocationHelper.mapCountryToCurrencyCode("AE"))
        assertEquals("KWD", LocationHelper.mapCountryToCurrencyCode("KW"))
        assertEquals("QAR", LocationHelper.mapCountryToCurrencyCode("QA"))
        assertEquals("BHD", LocationHelper.mapCountryToCurrencyCode("BH"))
        assertEquals("OMR", LocationHelper.mapCountryToCurrencyCode("OM"))
        assertEquals("EGP", LocationHelper.mapCountryToCurrencyCode("EG"))
        assertEquals("USD", LocationHelper.mapCountryToCurrencyCode("US"))
        assertEquals("GBP", LocationHelper.mapCountryToCurrencyCode("GB"))
        assertEquals("EUR", LocationHelper.mapCountryToCurrencyCode("FR"))
        assertEquals("EUR", LocationHelper.mapCountryToCurrencyCode("DE"))
        assertEquals("JOD", LocationHelper.mapCountryToCurrencyCode(null))
    }

    @Test
    fun testHaversineDistance() {
        // Distance between Amman (31.95, 35.91) and Zarqa (~32.07, 36.09) is ~20-25 km
        val dist = LocationHelper.haversineDistanceKm(31.95, 35.91, 32.07, 36.09)
        assertTrue(dist in 15.0..30.0)

        // Same location should be 0 km
        val zeroDist = LocationHelper.haversineDistanceKm(31.95, 35.91, 31.95, 35.91)
        assertEquals(0.0, zeroDist, 0.001)
    }
}
