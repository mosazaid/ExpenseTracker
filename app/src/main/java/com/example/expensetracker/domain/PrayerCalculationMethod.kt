package com.example.expensetracker.domain

enum class PrayerCalculationMethod(
    val displayName: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double? = null,
    val ishaIntervalMinutes: Int? = null
) {
    JORDAN(
        displayName = "Jordan (General Iftaa' Department)",
        description = "Fajr: 18.0°, Isha: 18.0°",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    UMM_AL_QURA(
        displayName = "Umm al-Qura (Makkah, Saudi Arabia)",
        description = "Fajr: 18.5°, Isha: 90 min after Maghrib",
        fajrAngle = 18.5,
        ishaIntervalMinutes = 90
    ),
    EGYPT(
        displayName = "Egyptian General Authority of Survey",
        description = "Fajr: 19.5°, Isha: 17.5°",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    ),
    TURKEY(
        displayName = "Diyanet İşleri Başkanlığı (Turkey)",
        description = "Fajr: 18.0°, Isha: 17.0°",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    KARACHI(
        displayName = "University of Islamic Sciences (Karachi)",
        description = "Fajr: 18.0°, Isha: 18.0°",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    ISNA(
        displayName = "Islamic Society of North America (ISNA)",
        description = "Fajr: 15.0°, Isha: 15.0°",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    MWL(
        displayName = "Muslim World League (MWL)",
        description = "Fajr: 18.0°, Isha: 17.0°",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    DUBAI(
        displayName = "Dubai (UAE Islamic Affairs)",
        description = "Fajr: 18.2°, Isha: 18.2°",
        fajrAngle = 18.2,
        ishaAngle = 18.2
    ),
    KUWAIT(
        displayName = "Kuwait Ministry of Awqaf",
        description = "Fajr: 18.0°, Isha: 17.5°",
        fajrAngle = 18.0,
        ishaAngle = 17.5
    ),
    QATAR(
        displayName = "Qatar Ministry of Awqaf",
        description = "Fajr: 18.0°, Isha: 90 min after Maghrib",
        fajrAngle = 18.0,
        ishaIntervalMinutes = 90
    ),
    SINGAPORE(
        displayName = "MUIS / JAKIM (Southeast Asia: Singapore, Malaysia, Indonesia, Philippines)",
        description = "Fajr: 20.0°, Isha: 18.0°",
        fajrAngle = 20.0,
        ishaAngle = 18.0
    ),
    TEHRAN(
        displayName = "Institute of Geophysics (Tehran)",
        description = "Fajr: 17.7°, Isha: 14.0°",
        fajrAngle = 17.7,
        ishaAngle = 14.0
    );

    companion object {
        fun autoDetect(countryCode: String?): PrayerCalculationMethod {
            val code = countryCode?.uppercase() ?: return MWL
            return when (code) {
                "JO" -> JORDAN
                "SA" -> UMM_AL_QURA
                "EG" -> EGYPT
                "TR" -> TURKEY
                "PK", "IN", "BD", "AF" -> KARACHI
                "US", "CA" -> ISNA
                "AE" -> DUBAI
                "KW" -> KUWAIT
                "QA" -> QATAR
                "SG", "MY", "ID", "PH", "BN", "TH" -> SINGAPORE
                "IR" -> TEHRAN
                else -> MWL
            }
        }
    }
}
