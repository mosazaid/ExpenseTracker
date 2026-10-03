package com.example.expensetracker.domain

import java.util.Calendar
import java.util.Date
import kotlin.math.*

data class CityLocation(
    val nameEn: String,
    val nameAr: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneOffsetHours: Double,
    val elevationMeters: Double = 0.0
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
    val calculationMethod: PrayerCalculationMethod = PrayerCalculationMethod.MWL,
    val is24Hour: Boolean = false,
    val fajr24: String = fajr,
    val sunrise24: String = sunrise,
    val dhuhr24: String = dhuhr,
    val asr24: String = asr,
    val maghrib24: String = maghrib,
    val isha24: String = isha
)

object PrayerTimesCalculator {

    // Kaaba Coordinates in Makkah
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    val PRESET_CITIES = listOf(
        // Jordan & Levant (with accurate elevations above sea level)
        CityLocation("Amman, Jordan", "عمان، الأردن", 31.9539, 35.9106, 3.0, 780.0),
        CityLocation("Irbid, Jordan", "إربد، الأردن", 32.5568, 35.8469, 3.0, 620.0),
        CityLocation("Zarqa, Jordan", "الزرقاء، الأردن", 32.0608, 36.0942, 3.0, 610.0),
        CityLocation("Aqaba, Jordan", "العقبة، الأردن", 29.5320, 35.0063, 3.0, 20.0),
        CityLocation("Jerusalem, Palestine", "القدس، فلسطين", 31.7683, 35.2137, 3.0, 750.0),
        CityLocation("Gaza, Palestine", "غزة، فلسطين", 31.5017, 34.4668, 3.0, 30.0),
        CityLocation("Ramallah, Palestine", "رام الله، فلسطين", 31.9038, 35.2034, 3.0, 870.0),
        CityLocation("Hebron, Palestine", "الخليل، فلسطين", 31.5326, 35.0998, 3.0, 930.0),
        CityLocation("Nablus, Palestine", "نابلس، فلسطين", 32.2227, 35.2621, 3.0, 570.0),
        CityLocation("Beirut, Lebanon", "بيروت، لبنان", 33.8938, 35.5018, 3.0, 35.0),
        CityLocation("Damascus, Syria", "دمشق، سوريا", 33.5138, 36.2765, 3.0, 680.0),
        CityLocation("Aleppo, Syria", "حلب، سوريا", 36.2021, 37.1343, 3.0, 380.0),
        CityLocation("Baghdad, Iraq", "بغداد، العراق", 33.3152, 44.3661, 3.0, 34.0),
        CityLocation("Erbil, Iraq", "أربيل، العراق", 36.1901, 44.0091, 3.0, 414.0),
        CityLocation("Basra, Iraq", "البصرة، العراق", 30.5085, 47.7804, 3.0, 5.0),

        // Arabian Peninsula & Gulf
        CityLocation("Makkah, Saudi Arabia", "مكة المكرمة، السعودية", 21.4225, 39.8262, 3.0, 277.0),
        CityLocation("Madinah, Saudi Arabia", "المدينة المنورة، السعودية", 24.5247, 39.5692, 3.0, 608.0),
        CityLocation("Riyadh, Saudi Arabia", "الرياض، السعودية", 24.7136, 46.6753, 3.0, 612.0),
        CityLocation("Jeddah, Saudi Arabia", "جدة، السعودية", 21.4858, 39.1925, 3.0, 12.0),
        CityLocation("Dammam, Saudi Arabia", "الدمام، السعودية", 26.4207, 50.0888, 3.0, 10.0),
        CityLocation("Dubai, UAE", "دبي، الإمارات", 25.2048, 55.2708, 4.0, 5.0),
        CityLocation("Abu Dhabi, UAE", "أبوظبي، الإمارات", 24.4539, 54.3773, 4.0, 7.0),
        CityLocation("Sharjah, UAE", "الشارقة، الإمارات", 25.3573, 55.4033, 4.0, 5.0),
        CityLocation("Kuwait City, Kuwait", "الكويت، الكويت", 29.3759, 47.9774, 3.0, 10.0),
        CityLocation("Doha, Qatar", "الدوحة، قطر", 25.2854, 51.5310, 3.0, 13.0),
        CityLocation("Manama, Bahrain", "المنامة، البحرين", 26.2285, 50.5860, 3.0, 6.0),
        CityLocation("Muscat, Oman", "مسقط، عمان", 23.5880, 58.3829, 4.0, 15.0),
        CityLocation("Salalah, Oman", "صلالة، عمان", 17.0151, 54.0924, 4.0, 18.0),
        CityLocation("Sana'a, Yemen", "صنعاء، اليمن", 15.3694, 44.1910, 3.0, 2250.0),
        CityLocation("Aden, Yemen", "عدن، اليمن", 12.7855, 45.0187, 3.0, 6.0),

        // North Africa
        CityLocation("Cairo, Egypt", "القاهرة، مصر", 30.0444, 31.2357, 2.0, 30.0),
        CityLocation("Alexandria, Egypt", "الإسكندرية، مصر", 31.2001, 29.9187, 2.0, 5.0),
        CityLocation("Tripoli, Libya", "طرابلس، ليبيا", 32.8872, 13.1913, 2.0, 8.0),
        CityLocation("Benghazi, Libya", "بنغازي، ليبيا", 32.1166, 20.0686, 2.0, 2.0),
        CityLocation("Tunis, Tunisia", "تونس، تونس", 36.8065, 10.1815, 1.0, 10.0),
        CityLocation("Algiers, Algeria", "الجزائر، الجزائر", 36.7538, 3.0588, 1.0, 15.0),
        CityLocation("Oran, Algeria", "وهران، الجزائر", 35.6987, -0.6349, 1.0, 60.0),
        CityLocation("Casablanca, Morocco", "الدار البيضاء، المغرب", 33.5731, -7.5898, 1.0, 25.0),
        CityLocation("Rabat, Morocco", "الرباط، المغرب", 34.0209, -6.8416, 1.0, 50.0),
        CityLocation("Marrakech, Morocco", "مراكش، المغرب", 31.6295, -7.9811, 1.0, 466.0),
        CityLocation("Khartoum, Sudan", "الخرطوم، السودان", 15.5007, 32.5599, 2.0, 382.0),
        CityLocation("Mogadishu, Somalia", "مقديشو، الصومال", 2.0469, 45.3182, 3.0, 9.0),
        CityLocation("Nouakchott, Mauritania", "نواكشوط، موريتانيا", 18.0735, -15.9582, 0.0, 7.0),

        // Austria (النمسا)
        CityLocation("Vienna, Austria", "فيينا، النمسا", 48.2082, 16.3738, 1.0, 190.0),
        CityLocation("Graz, Austria", "غراتس، النمسا", 47.0707, 15.4395, 1.0, 353.0),
        CityLocation("Salzburg, Austria", "سالزبورغ، النمسا", 47.8095, 13.0550, 1.0, 424.0),
        CityLocation("Innsbruck, Austria", "إنسبروك، النمسا", 47.2692, 11.4041, 1.0, 574.0),

        // Philippines (الفلبين)
        CityLocation("Manila, Philippines", "مانيلا، الفلبين", 14.5995, 120.9842, 8.0, 16.0),
        CityLocation("Cebu City, Philippines", "سيبو، الفلبين", 10.3157, 123.8854, 8.0, 17.0),
        CityLocation("Davao City, Philippines", "داباو، الفلبين", 7.1907, 125.4553, 8.0, 22.0),
        CityLocation("Zamboanga City, Philippines", "زامبوانجا، الفلبين", 6.9214, 122.0790, 8.0, 16.0),
        CityLocation("Cotabato City, Philippines", "كوتاباتو، الفلبين", 7.2236, 124.2464, 8.0, 9.0),

        // Malaysia (ماليزيا)
        CityLocation("Kuala Lumpur, Malaysia", "كوالالمبور، ماليزيا", 3.1390, 101.6869, 8.0, 50.0),
        CityLocation("Penang, Malaysia", "بينانغ، ماليزيا", 5.4141, 100.3288, 8.0, 15.0),
        CityLocation("Johor Bahru, Malaysia", "جوهر بهرو، ماليزيا", 1.4927, 103.7414, 8.0, 36.0),
        CityLocation("Kota Kinabalu, Malaysia", "كوتا كينابالو، ماليزيا", 5.9804, 116.0735, 8.0, 10.0),
        CityLocation("Kuching, Malaysia", "كوتشينغ، ماليزيا", 1.5535, 110.3592, 8.0, 8.0),

        // Southeast Asia & East Asia
        CityLocation("Singapore, Singapore", "سنغافورة، سنغافورة", 1.3521, 103.8198, 8.0, 15.0),
        CityLocation("Jakarta, Indonesia", "جاكرتا، إندونيسيا", -6.2088, 106.8456, 7.0, 8.0),
        CityLocation("Surabaya, Indonesia", "سورابايا، إندونيسيا", -7.2575, 112.7521, 7.0, 5.0),
        CityLocation("Bandung, Indonesia", "باندونغ، إندونيسيا", -6.9175, 107.6191, 7.0, 768.0),
        CityLocation("Medan, Indonesia", "ميدان، إندونيسيا", 3.5952, 98.6722, 7.0, 26.0),
        CityLocation("Bangkok, Thailand", "بانكوك، تايلاند", 13.7563, 100.5018, 7.0, 5.0),
        CityLocation("Tokyo, Japan", "طوكيو، اليابان", 35.6762, 139.6503, 9.0, 40.0),
        CityLocation("Osaka, Japan", "أوساكا، اليابان", 34.6937, 135.5023, 9.0, 20.0),
        CityLocation("Seoul, South Korea", "سيول، كوريا الجنوبية", 37.5665, 126.9780, 9.0, 38.0),
        CityLocation("Beijing, China", "بكين، الصين", 39.9042, 116.4074, 8.0, 43.0),

        // South Asia
        CityLocation("Islamabad, Pakistan", "إسلام آباد، باكستان", 33.6844, 73.0479, 5.0, 507.0),
        CityLocation("Karachi, Pakistan", "كراتشي، باكستان", 24.8607, 67.0011, 5.0, 10.0),
        CityLocation("Lahore, Pakistan", "لاهور، باكستان", 31.5204, 74.3587, 5.0, 217.0),
        CityLocation("New Delhi, India", "نيودلهي، الهند", 28.6139, 77.2090, 5.5, 216.0),
        CityLocation("Mumbai, India", "مومباي، الهند", 19.0760, 72.8777, 5.5, 14.0),
        CityLocation("Hyderabad, India", "حيدر آباد، الهند", 17.3850, 78.4867, 5.5, 542.0),
        CityLocation("Dhaka, Bangladesh", "دكا، بنغلاديش", 23.8103, 90.4125, 6.0, 4.0),

        // Europe (أوروبا)
        CityLocation("London, UK", "لندن، بريطانيا", 51.5074, -0.1278, 0.0, 11.0),
        CityLocation("Birmingham, UK", "برمنغهام، بريطانيا", 52.4862, -1.8904, 0.0, 140.0),
        CityLocation("Manchester, UK", "مانشستر، بريطانيا", 53.4808, -2.2426, 0.0, 38.0),
        CityLocation("Paris, France", "باريس، فرنسا", 48.8566, 2.3522, 1.0, 35.0),
        CityLocation("Marseille, France", "مارسيليا، فرنسا", 43.2965, 5.3698, 1.0, 12.0),
        CityLocation("Lyon, France", "ليون، فرنسا", 45.7640, 4.8357, 1.0, 173.0),
        CityLocation("Berlin, Germany", "برلين، ألمانيا", 52.5200, 13.4050, 1.0, 34.0),
        CityLocation("Munich, Germany", "ميونيخ، ألمانيا", 48.1351, 11.5820, 1.0, 519.0),
        CityLocation("Frankfurt, Germany", "فرانكفورت، ألمانيا", 50.1109, 8.6821, 1.0, 112.0),
        CityLocation("Cologne, Germany", "كولونيا، ألمانيا", 50.9375, 6.9603, 1.0, 53.0),
        CityLocation("Hamburg, Germany", "هامبورغ، ألمانيا", 53.5511, 9.9937, 1.0, 6.0),
        CityLocation("Madrid, Spain", "مدريد، إسبانيا", 40.4168, -3.7038, 1.0, 667.0),
        CityLocation("Barcelona, Spain", "برشلونة، إسبانيا", 41.3851, 2.1734, 1.0, 12.0),
        CityLocation("Cordoba, Spain", "قرطبة، إسبانيا", 37.8882, -4.7794, 1.0, 120.0),
        CityLocation("Granada, Spain", "غرناطة، إسبانيا", 37.1773, -3.5986, 1.0, 738.0),
        CityLocation("Rome, Italy", "روما، إيطاليا", 41.9028, 12.4964, 1.0, 21.0),
        CityLocation("Milan, Italy", "ميلانو، إيطاليا", 45.4642, 9.1900, 1.0, 120.0),
        CityLocation("Amsterdam, Netherlands", "أمستردام، هولندا", 52.3676, 4.9041, 1.0, 2.0),
        CityLocation("Rotterdam, Netherlands", "روتردام، هولندا", 51.9244, 4.4777, 1.0, 0.0),
        CityLocation("Brussels, Belgium", "بروكسل، بلجيكا", 50.8503, 4.3517, 1.0, 70.0),
        CityLocation("Zurich, Switzerland", "زيورخ، سويسرا", 47.3769, 8.5417, 1.0, 408.0),
        CityLocation("Geneva, Switzerland", "جنيف، سويسرا", 46.2044, 6.1432, 1.0, 375.0),
        CityLocation("Stockholm, Sweden", "ستوكهولم، السويد", 59.3293, 18.0686, 1.0, 15.0),
        CityLocation("Malmo, Sweden", "مالمو، السويد", 55.6050, 13.0038, 1.0, 12.0),
        CityLocation("Oslo, Norway", "أوسلو، النرويج", 59.9139, 10.7522, 1.0, 23.0),
        CityLocation("Copenhagen, Denmark", "كوبنهاغن، الدنمارك", 55.6761, 12.5683, 1.0, 14.0),
        CityLocation("Dublin, Ireland", "دبلن، أيرلندا", 53.3498, -6.2603, 0.0, 10.0),
        CityLocation("Sarajevo, Bosnia and Herzegovina", "سراييفو، البوسنة والهرسك", 43.8563, 18.4131, 1.0, 518.0),
        CityLocation("Athens, Greece", "أثينا، اليونان", 37.9838, 23.7275, 2.0, 70.0),
        CityLocation("Lisbon, Portugal", "لشبونة، البرتغال", 38.7223, -9.1393, 0.0, 15.0),
        CityLocation("Warsaw, Poland", "وارسو، بولندا", 52.2297, 21.0122, 1.0, 100.0),
        CityLocation("Prague, Czech Republic", "براغ، التشيك", 50.0755, 14.4378, 1.0, 235.0),
        CityLocation("Budapest, Hungary", "بودابست، المجر", 47.4979, 19.0402, 1.0, 102.0),
        CityLocation("Bucharest, Romania", "بوخارست، رومانيا", 44.4268, 26.1025, 2.0, 75.0),
        CityLocation("Tirana, Albania", "تيرانا، ألبانيا", 41.3275, 19.8187, 1.0, 110.0),
        CityLocation("Pristina, Kosovo", "بريشتينا، كوسوفو", 42.6629, 21.1655, 1.0, 652.0),
        CityLocation("Helsinki, Finland", "هلسنكي، فنلندا", 60.1699, 24.9384, 2.0, 25.0),
        CityLocation("Istanbul, Turkey", "إسطنبول، تركيا", 41.0082, 28.9784, 3.0, 40.0),
        CityLocation("Ankara, Turkey", "أنقرة، تركيا", 39.9334, 32.8597, 3.0, 938.0),
        CityLocation("Moscow, Russia", "موسكو، روسيا", 55.7558, 37.6173, 3.0, 156.0),

        // Americas, Australia & Africa
        CityLocation("New York, USA", "نيويورك، أمريكا", 40.7128, -74.0060, -5.0, 10.0),
        CityLocation("Chicago, USA", "شيكاغو، أمريكا", 41.8781, -87.6298, -6.0, 181.0),
        CityLocation("Los Angeles, USA", "لوس أنجلوس، أمريكا", 34.0522, -118.2437, -8.0, 89.0),
        CityLocation("Houston, USA", "هيوستن، أمريكا", 29.7604, -95.3698, -6.0, 15.0),
        CityLocation("Toronto, Canada", "تورونتو، كندا", 43.6532, -79.3832, -5.0, 76.0),
        CityLocation("Montreal, Canada", "مونتريال، كندا", 45.5017, -73.5673, -5.0, 36.0),
        CityLocation("Vancouver, Canada", "فانكوفر، كندا", 49.2827, -123.1207, -8.0, 70.0),
        CityLocation("São Paulo, Brazil", "ساو باولو، البرازيل", -23.5505, -46.6333, -3.0, 760.0),
        CityLocation("Sydney, Australia", "سيدني، أستراليا", -33.8688, 151.2093, 10.0, 19.0),
        CityLocation("Melbourne, Australia", "ملبورن، أستراليا", -37.8136, 144.9631, 10.0, 31.0),
        CityLocation("Auckland, New Zealand", "أوكلاند، نيوزيلندا", -36.8485, 174.7633, 12.0, 10.0),
        CityLocation("Johannesburg, South Africa", "جوهانسبرغ، جنوب أفريقيا", -26.2041, 28.0473, 2.0, 1753.0),
        CityLocation("Cape Town, South Africa", "كيب تاون، جنوب أفريقيا", -33.9249, 18.4241, 2.0, 12.0)
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

    // --- Astronomical Solar Calculations (Jean Meeus / NOAA standard) ---

    fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(angle: Double): Double {
        var res = angle - 360.0 * floor(angle / 360.0)
        if (res < 0.0) res += 360.0
        return res
    }

    private fun fixHour(hour: Double): Double {
        var res = hour - 24.0 * floor(hour / 24.0)
        if (res < 0.0) res += 24.0
        return res
    }

    data class SunCoordinates(val declination: Double, val equationOfTime: Double)

    fun sunPosition(jd: Double): SunCoordinates {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(Math.toRadians(g)) + 0.020 * sin(Math.toRadians(2.0 * g)))
        val e = 23.439 - 0.00000036 * d
        val dRad = asin(sin(Math.toRadians(e)) * sin(Math.toRadians(l)))
        var ra = Math.toDegrees(atan2(cos(Math.toRadians(e)) * sin(Math.toRadians(l)), cos(Math.toRadians(l)))) / 15.0
        ra = fixHour(ra)
        var eqt = (q / 15.0) - ra
        while (eqt > 12.0) eqt -= 24.0
        while (eqt < -12.0) eqt += 24.0
        return SunCoordinates(declination = Math.toDegrees(dRad), equationOfTime = eqt)
    }

    private fun hourAngle(altitudeDeg: Double, declinationDeg: Double, latitudeDeg: Double): Double {
        val altRad = Math.toRadians(altitudeDeg)
        val declRad = Math.toRadians(declinationDeg)
        val latRad = Math.toRadians(latitudeDeg)
        val cosHa = (sin(altRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        val clamped = cosHa.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clamped)) / 15.0
    }

    fun formatPrayerTime(time24: String, is24Hour: Boolean): String {
        if (is24Hour) return time24
        val parts = time24.split(":")
        if (parts.size < 2) return time24
        val h = parts[0].trim().toIntOrNull() ?: return time24
        val m = parts[1].trim().take(2).toIntOrNull() ?: return time24
        val isPm = h >= 12
        val displayH = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(java.util.Locale.US, "%d:%02d %s", displayH, m, amPm)
    }

    fun calculateDaySchedule(
        city: CityLocation,
        date: Date = Date(),
        method: PrayerCalculationMethod = PrayerCalculationMethod.JORDAN,
        is24Hour: Boolean = false
    ): PrayerSchedule {
        val cal = Calendar.getInstance().apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val jd = julianDate(year, month, day)

        fun computeMidDay(timeHour: Double): Double {
            val sun = sunPosition(jd + timeHour / 24.0)
            return fixHour(12.0 - sun.equationOfTime - (city.longitude / 15.0) + city.timezoneOffsetHours)
        }

        // Midday (solar noon)
        var noon = computeMidDay(12.0)
        val sunNoon = sunPosition(jd + noon / 24.0)
        val declNoon = sunNoon.declination

        // Sunrise & Sunset depression angle: -0.8333° (standard refraction + semi-diameter)
        // plus observer elevation dip: 0.0347 * sqrt(elevationMeters)
        val elevationDip = 0.0347 * sqrt(max(0.0, city.elevationMeters))
        val riseSetAngle = 0.8333 + elevationDip

        // 1. Initial estimates
        val estRiseSetHa = hourAngle(-riseSetAngle, declNoon, city.latitude)
        var sunriseHours = noon - estRiseSetHa
        var sunsetHours = noon + estRiseSetHa

        val estFajrHa = hourAngle(-method.fajrAngle, declNoon, city.latitude)
        var fajrHours = noon - estFajrHa

        val latRad = Math.toRadians(city.latitude)
        val declRad = Math.toRadians(declNoon)
        val noonShadow = tan(abs(latRad - declRad))
        val asrAltRad = atan(1.0 / (1.0 + noonShadow))
        val asrAltDeg = Math.toDegrees(asrAltRad)
        val estAsrHa = hourAngle(asrAltDeg, declNoon, city.latitude)
        var asrHours = noon + estAsrHa

        var ishaHours = if (method.ishaIntervalMinutes != null) {
            sunsetHours + (method.ishaIntervalMinutes / 60.0)
        } else {
            val ishaAngle = method.ishaAngle ?: 18.0
            val estIshaHa = hourAngle(-ishaAngle, declNoon, city.latitude)
            noon + estIshaHa
        }

        // 2. Refined iteration for true solar position at each prayer instant
        val sunFajr = sunPosition(jd + fajrHours / 24.0)
        val noonFajr = computeMidDay(fajrHours)
        fajrHours = noonFajr - hourAngle(-method.fajrAngle, sunFajr.declination, city.latitude)

        val sunRise = sunPosition(jd + sunriseHours / 24.0)
        val noonRise = computeMidDay(sunriseHours)
        sunriseHours = noonRise - hourAngle(-riseSetAngle, sunRise.declination, city.latitude)

        val sunSet = sunPosition(jd + sunsetHours / 24.0)
        val noonSet = computeMidDay(sunsetHours)
        sunsetHours = noonSet + hourAngle(-riseSetAngle, sunSet.declination, city.latitude)

        val sunAsr = sunPosition(jd + asrHours / 24.0)
        val noonAsr = computeMidDay(asrHours)
        val asrDeclRad = Math.toRadians(sunAsr.declination)
        val asrNoonShadow = tan(abs(latRad - asrDeclRad))
        val refinedAsrAlt = Math.toDegrees(atan(1.0 / (1.0 + asrNoonShadow)))
        asrHours = noonAsr + hourAngle(refinedAsrAlt, sunAsr.declination, city.latitude)

        if (method.ishaIntervalMinutes != null) {
            ishaHours = sunsetHours + (method.ishaIntervalMinutes / 60.0)
        } else {
            val ishaAngle = method.ishaAngle ?: 18.0
            val sunIsha = sunPosition(jd + ishaHours / 24.0)
            val noonIsha = computeMidDay(ishaHours)
            ishaHours = noonIsha + hourAngle(-ishaAngle, sunIsha.declination, city.latitude)
        }

        // Dhuhr: solar noon (midday)
        val dhuhrHours = noon

        fun toTime24(hours: Double): String {
            val totalMinutes = (hours * 60.0).roundToInt()
            val normalizedMin = (totalMinutes % (24 * 60) + (24 * 60)) % (24 * 60)
            val h = normalizedMin / 60
            val m = normalizedMin % 60
            return String.format(java.util.Locale.US, "%02d:%02d", h, m)
        }

        val fajr24 = toTime24(fajrHours)
        val sunrise24 = toTime24(sunriseHours)
        val dhuhr24 = toTime24(dhuhrHours)
        val asr24 = toTime24(asrHours)
        val maghrib24 = toTime24(sunsetHours)
        val isha24 = toTime24(ishaHours)

        val fajrDisplay = formatPrayerTime(fajr24, is24Hour)
        val sunriseDisplay = formatPrayerTime(sunrise24, is24Hour)
        val dhuhrDisplay = formatPrayerTime(dhuhr24, is24Hour)
        val asrDisplay = formatPrayerTime(asr24, is24Hour)
        val maghribDisplay = formatPrayerTime(maghrib24, is24Hour)
        val ishaDisplay = formatPrayerTime(isha24, is24Hour)

        // Calculate next upcoming prayer based on current time
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        fun minOf(h: Double): Int = (h * 60.0).roundToInt() % (24 * 60)

        val prayerList = listOf(
            "Fajr" to minOf(fajrHours),
            "Dhuhr" to minOf(dhuhrHours),
            "Asr" to minOf(asrHours),
            "Maghrib" to minOf(sunsetHours),
            "Isha" to minOf(ishaHours)
        )

        val next = prayerList.firstOrNull { it.second > currentMinutes } ?: prayerList.first()
        val nextTimeStr = when (next.first) {
            "Fajr" -> fajrDisplay
            "Dhuhr" -> dhuhrDisplay
            "Asr" -> asrDisplay
            "Maghrib" -> maghribDisplay
            else -> ishaDisplay
        }

        val qiblah = calculateQiblahBearing(city.latitude, city.longitude)

        return PrayerSchedule(
            cityName = city.nameEn,
            fajr = fajrDisplay,
            sunrise = sunriseDisplay,
            dhuhr = dhuhrDisplay,
            asr = asrDisplay,
            maghrib = maghribDisplay,
            isha = ishaDisplay,
            nextPrayerName = next.first,
            nextPrayerTime = nextTimeStr,
            qiblahBearingDegrees = qiblah,
            calculationMethod = method,
            is24Hour = is24Hour,
            fajr24 = fajr24,
            sunrise24 = sunrise24,
            dhuhr24 = dhuhr24,
            asr24 = asr24,
            maghrib24 = maghrib24,
            isha24 = isha24
        )
    }
}
