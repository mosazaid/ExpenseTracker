package com.example.expensetracker.domain

import java.util.Calendar
import java.util.Date
import kotlin.math.*

data class CityLocation(
    val nameEn: String,
    val nameAr: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffsetHours: Double
)

data class PrayerSchedule(
    val cityName: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val nextPrayerName: String,
    val nextPrayerTime: String,
    val qiblahBearingDegrees: Float,
    val calculationMethod: PrayerCalculationMethod = PrayerCalculationMethod.MWL
)

object PrayerTimesCalculator {

    // Kaaba Coordinates in Makkah
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    val PRESET_CITIES = listOf(
        CityLocation("Amman, Jordan", "عمان، الأردن", 31.9539, 35.9106, 3.0),
        CityLocation("Riyadh, Saudi Arabia", "الرياض، السعودية", 24.7136, 46.6753, 3.0),
        CityLocation("Dubai, UAE", "دبي، الإمارات", 25.2048, 55.2708, 4.0),
        CityLocation("Cairo, Egypt", "القاهرة، مصر", 30.0444, 31.2357, 2.0),
        CityLocation("Kuwait City, Kuwait", "الكويت، الكويت", 29.3759, 47.9774, 3.0),
        CityLocation("Doha, Qatar", "الدوحة، قطر", 25.2854, 51.5310, 3.0),
        CityLocation("Manama, Bahrain", "المنامة، البحرين", 26.2285, 50.5860, 3.0),
        CityLocation("Muscat, Oman", "مسقط، عمان", 23.5880, 58.3829, 4.0),
        CityLocation("Jerusalem, Palestine", "القدس، فلسطين", 31.7683, 35.2137, 3.0),
        CityLocation("Baghdad, Iraq", "بغداد، العراق", 33.3152, 44.3661, 3.0),
        CityLocation("Beirut, Lebanon", "بيروت، لبنان", 33.8938, 35.5018, 3.0),
        CityLocation("Casablanca, Morocco", "الدار البيضاء، المغرب", 33.5731, -7.5898, 1.0),
        CityLocation("Algiers, Algeria", "الجزائر، الجزائر", 36.7538, 3.0588, 1.0),
        CityLocation("Tunis, Tunisia", "تونس، تونس", 36.8065, 10.1815, 1.0),
        CityLocation("Istanbul, Turkey", "إسطنبول، تركيا", 41.0082, 28.9784, 3.0),
        CityLocation("London, UK", "لندن، بريطانيا", 51.5074, -0.1278, 1.0),
        CityLocation("Paris, France", "باريس، فرنسا", 48.8566, 2.3522, 2.0),
        CityLocation("Berlin, Germany", "برلين، ألمانيا", 52.5200, 13.4050, 2.0),
        CityLocation("New York, USA", "نيويورك، أمريكا", 40.7128, -74.0060, -4.0)
    )

    fun calculateQiblahBearing(lat: Double, lng: Double): Float {
        val latRad = Math.toRadians(lat)
        val lngRad = Math.toRadians(lng)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val kaabaLngRad = Math.toRadians(KAABA_LNG)

        val deltaLng = kaabaLngRad - lngRad

        val y = sin(deltaLng) * cos(kaabaLatRad)
        val x = cos(latRad) * sin(kaabaLatRad) - sin(latRad) * cos(kaabaLatRad) * cos(deltaLng)

        var bearing = Math.toDegrees(atan2(y, x))
        if (bearing < 0) {
            bearing += 360.0
        }
        return bearing.toFloat()
    }

    fun calculateDaySchedule(
        city: CityLocation,
        date: Date = Date(),
        method: PrayerCalculationMethod = PrayerCalculationMethod.MWL
    ): PrayerSchedule {
        val cal = Calendar.getInstance().apply { time = date }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        // Solar Declination & Equation of Time approximations
        val b = 2.0 * Math.PI * (dayOfYear - 81) / 365.0
        val eotMinutes = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
        val declinationDeg = 23.45 * sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
        val declinationRad = Math.toRadians(declinationDeg)
        val latRad = Math.toRadians(city.latitude)

        // Solar Noon (Dhuhr)
        val timeZoneMeridian = city.timezoneOffsetHours * 15.0
        val solarNoonUtc = 12.0 - (city.longitude - timeZoneMeridian) / 15.0 - (eotMinutes / 60.0)

        // Hour angle helper for sun altitude angle
        fun hourAngle(altitudeDeg: Double): Double {
            val altRad = Math.toRadians(altitudeDeg)
            val cosHa = (sin(altRad) - sin(latRad) * sin(declinationRad)) / (cos(latRad) * cos(declinationRad))
            val clamped = cosHa.coerceIn(-1.0, 1.0)
            return Math.toDegrees(acos(clamped)) / 15.0
        }

        // Sunrise & Sunset (approx -0.833° for refraction & sun disc)
        val sunRiseSetHa = hourAngle(-0.833)
        val sunriseHours = solarNoonUtc - sunRiseSetHa
        val sunsetHours = solarNoonUtc + sunRiseSetHa

        // Fajr (based on calculation method angle)
        val fajrHa = hourAngle(-method.fajrAngle)
        val fajrHours = solarNoonUtc - fajrHa

        // Isha (based on method angle or fixed interval after Maghrib)
        val ishaHours = if (method.ishaIntervalMinutes != null) {
            sunsetHours + (method.ishaIntervalMinutes / 60.0)
        } else {
            val ishaAngle = method.ishaAngle ?: 17.0
            val ishaHa = hourAngle(-ishaAngle)
            solarNoonUtc + ishaHa
        }

        // Asr (Shafi'i / standard: shadow length = object + noon shadow)
        val noonShadow = tan(abs(latRad - declinationRad))
        val asrAltitudeRad = atan(1.0 / (1.0 + noonShadow))
        val asrAltitudeDeg = Math.toDegrees(asrAltitudeRad)
        val asrHa = hourAngle(asrAltitudeDeg)
        val asrHours = solarNoonUtc + asrHa

        fun formatHours(hours: Double): String {
            val totalMinutes = (hours * 60).roundToInt()
            val normalizedMin = (totalMinutes % (24 * 60) + (24 * 60)) % (24 * 60)
            val h = normalizedMin / 60
            val m = normalizedMin % 60
            return String.format(java.util.Locale.US, "%02d:%02d", h, m)
        }

        val fajrStr = formatHours(fajrHours)
        val sunriseStr = formatHours(sunriseHours)
        val dhuhrStr = formatHours(solarNoonUtc)
        val asrStr = formatHours(asrHours)
        val maghribStr = formatHours(sunsetHours)
        val ishaStr = formatHours(ishaHours)

        // Calculate next upcoming prayer
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val prayerList = listOf(
            "Fajr" to (fajrHours * 60).toInt(),
            "Dhuhr" to (solarNoonUtc * 60).toInt(),
            "Asr" to (asrHours * 60).toInt(),
            "Maghrib" to (sunsetHours * 60).toInt(),
            "Isha" to (ishaHours * 60).toInt()
        )

        val next = prayerList.firstOrNull { it.second > currentMinutes } ?: prayerList.first()
        val nextTimeStr = when (next.first) {
            "Fajr" -> fajrStr
            "Dhuhr" -> dhuhrStr
            "Asr" -> asrStr
            "Maghrib" -> maghribStr
            else -> ishaStr
        }

        val qiblah = calculateQiblahBearing(city.latitude, city.longitude)

        return PrayerSchedule(
            cityName = city.nameEn,
            fajr = fajrStr,
            sunrise = sunriseStr,
            dhuhr = dhuhrStr,
            asr = asrStr,
            maghrib = maghribStr,
            isha = ishaStr,
            nextPrayerName = next.first,
            nextPrayerTime = nextTimeStr,
            qiblahBearingDegrees = qiblah,
            calculationMethod = method
        )
    }
}
