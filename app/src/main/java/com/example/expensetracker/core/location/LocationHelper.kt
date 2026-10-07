package com.example.expensetracker.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.expensetracker.core.format.CurrencyUtils
import com.example.expensetracker.domain.CityLocation
import com.example.expensetracker.domain.PrayerTimesCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class DeviceLocationInfo(
    val latitude: Double,
    val longitude: Double,
    val countryCode: String?,
    val cityName: String?,
    val detectedCurrencyCode: String,
    val nearestPresetCity: CityLocation,
    val elevationMeters: Double = nearestPresetCity.elevationMeters
)

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                            bestLocation = location
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return bestLocation
    }

    fun detectLocationInfo(): DeviceLocationInfo? {
        val location = getLastKnownLocation() ?: return null
        val alt = if (location.hasAltitude() && location.altitude > 0.0) location.altitude else null
        return resolveLocationDetails(location.latitude, location.longitude, alt)
    }

    fun resolveLocationDetails(lat: Double, lng: Double, altitudeMeters: Double? = null): DeviceLocationInfo {
        var countryCode: String? = null
        var cityName: String? = null

        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var resultList: List<android.location.Address>? = null
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        resultList = addresses
                    }
                    resultList
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)
                }

                val address = addresses?.firstOrNull()
                if (address != null) {
                    countryCode = address.countryCode
                    cityName = address.locality ?: address.subAdminArea ?: address.adminArea
                }
            }
        } catch (_: Exception) {}

        val nearestCity = findNearestPresetCity(lat, lng)

        val resolvedCountry = countryCode ?: mapCityToCountryCode(nearestCity.nameEn)
        val currencyCode = mapCountryToCurrencyCode(resolvedCountry)

        val resolvedElevation = if (altitudeMeters != null && altitudeMeters > 0.0) {
            altitudeMeters
        } else {
            nearestCity.elevationMeters
        }

        return DeviceLocationInfo(
            latitude = lat,
            longitude = lng,
            countryCode = resolvedCountry,
            cityName = cityName ?: nearestCity.nameEn,
            detectedCurrencyCode = currencyCode,
            nearestPresetCity = nearestCity,
            elevationMeters = resolvedElevation
        )
    }

    fun findNearestPresetCity(lat: Double, lng: Double): CityLocation {
        return PrayerTimesCalculator.PRESET_CITIES.minByOrNull { city ->
            haversineDistanceKm(lat, lng, city.latitude, city.longitude)
        } ?: PrayerTimesCalculator.PRESET_CITIES.first()
    }

    companion object {
        fun mapCountryToCurrencyCode(countryCode: String?): String {
            if (countryCode == null) return "JOD"
            return when (countryCode.uppercase(Locale.US)) {
                "JO" -> "JOD"
                "SA" -> "SAR"
                "AE" -> "AED"
                "KW" -> "KWD"
                "QA" -> "QAR"
                "BH" -> "BHD"
                "OM" -> "OMR"
                "EG" -> "EGP"
                "IQ" -> "IQD"
                "LB" -> "LBP"
                "SY" -> "SYP"
                "PS", "IL" -> "ILS"
                "LY" -> "LYD"
                "TN" -> "TND"
                "DZ" -> "DZD"
                "MA" -> "MAD"
                "YE" -> "YER"
                "SD" -> "SDG"
                "TR" -> "TRY"
                "US" -> "USD"
                "GB" -> "GBP"
                "CA" -> "CAD"
                "AU" -> "AUD"
                "CH" -> "CHF"
                "SE" -> "SEK"
                "NO" -> "NOK"
                "DK" -> "DKK"
                "PL" -> "PLN"
                "JP" -> "JPY"
                "CN" -> "CNY"
                "IN" -> "INR"
                "PK" -> "PKR"
                "MY" -> "MYR"
                "SG" -> "SGD"
                "PH" -> "PHP"
                "ID" -> "IDR"
                "TH" -> "THB"
                "BA" -> "BAM"
                "CZ" -> "CZK"
                "HU" -> "HUF"
                "RO" -> "RON"
                "BR" -> "BRL"
                "ZA" -> "ZAR"
                "DE", "FR", "IT", "ES", "NL", "BE", "AT", "GR", "PT", "FI", "IE" -> "EUR"
                else -> "JOD"
            }
        }

        private fun mapCityToCountryCode(cityNameEn: String): String {
            return when {
                cityNameEn.contains("Jordan", ignoreCase = true) -> "JO"
                cityNameEn.contains("Saudi", ignoreCase = true) -> "SA"
                cityNameEn.contains("UAE", ignoreCase = true) -> "AE"
                cityNameEn.contains("Kuwait", ignoreCase = true) -> "KW"
                cityNameEn.contains("Qatar", ignoreCase = true) -> "QA"
                cityNameEn.contains("Bahrain", ignoreCase = true) -> "BH"
                cityNameEn.contains("Oman", ignoreCase = true) -> "OM"
                cityNameEn.contains("Egypt", ignoreCase = true) -> "EG"
                cityNameEn.contains("Palestine", ignoreCase = true) -> "PS"
                cityNameEn.contains("Iraq", ignoreCase = true) -> "IQ"
                cityNameEn.contains("Lebanon", ignoreCase = true) -> "LB"
                cityNameEn.contains("Syria", ignoreCase = true) -> "SY"
                cityNameEn.contains("Yemen", ignoreCase = true) -> "YE"
                cityNameEn.contains("Sudan", ignoreCase = true) -> "SD"
                cityNameEn.contains("Libya", ignoreCase = true) -> "LY"
                cityNameEn.contains("Morocco", ignoreCase = true) -> "MA"
                cityNameEn.contains("Algeria", ignoreCase = true) -> "DZ"
                cityNameEn.contains("Tunisia", ignoreCase = true) -> "TN"
                cityNameEn.contains("Turkey", ignoreCase = true) -> "TR"
                cityNameEn.contains("Austria", ignoreCase = true) -> "AT"
                cityNameEn.contains("Philippines", ignoreCase = true) -> "PH"
                cityNameEn.contains("Malaysia", ignoreCase = true) -> "MY"
                cityNameEn.contains("Indonesia", ignoreCase = true) -> "ID"
                cityNameEn.contains("Germany", ignoreCase = true) -> "DE"
                cityNameEn.contains("France", ignoreCase = true) -> "FR"
                cityNameEn.contains("Spain", ignoreCase = true) -> "ES"
                cityNameEn.contains("Italy", ignoreCase = true) -> "IT"
                cityNameEn.contains("Netherlands", ignoreCase = true) -> "NL"
                cityNameEn.contains("Belgium", ignoreCase = true) -> "BE"
                cityNameEn.contains("Switzerland", ignoreCase = true) -> "CH"
                cityNameEn.contains("Sweden", ignoreCase = true) -> "SE"
                cityNameEn.contains("Norway", ignoreCase = true) -> "NO"
                cityNameEn.contains("Denmark", ignoreCase = true) -> "DK"
                cityNameEn.contains("Ireland", ignoreCase = true) -> "IE"
                cityNameEn.contains("Bosnia", ignoreCase = true) -> "BA"
                cityNameEn.contains("Pakistan", ignoreCase = true) -> "PK"
                cityNameEn.contains("India", ignoreCase = true) -> "IN"
                cityNameEn.contains("Bangladesh", ignoreCase = true) -> "BD"
                cityNameEn.contains("Japan", ignoreCase = true) -> "JP"
                cityNameEn.contains("Australia", ignoreCase = true) -> "AU"
                cityNameEn.contains("Canada", ignoreCase = true) -> "CA"
                cityNameEn.contains("USA", ignoreCase = true) -> "US"
                cityNameEn.contains("UK", ignoreCase = true) -> "GB"
                else -> "JO"
            }
        }

        fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
