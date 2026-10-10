package com.example.expensetracker.domain

/**
 * Adhan call to prayer audio options curated from Assabile (https://www.assabile.com/adhan-call-prayer)
 */
enum class AdhanSoundOption(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val reciterEn: String,
    val reciterAr: String,
    val referenceUrl: String
) {
    MAKKAH(
        id = "MAKKAH",
        titleEn = "Adhan Makkah Al-Mukarramah",
        titleAr = "أذان مكة المكرمة",
        reciterEn = "Sheikh Ali Mulla",
        reciterAr = "الشيخ علي ملا",
        referenceUrl = "https://www.assabile.com/adhan-call-prayer/ali-ahmed-mulla-8.htm"
    ),
    MADINAH(
        id = "MADINAH",
        titleEn = "Adhan Madinah Al-Munawwarah",
        titleAr = "أذان المدينة المنورة",
        reciterEn = "Sheikh Essam Bukhari",
        reciterAr = "الشيخ عصام بخاري",
        referenceUrl = "https://www.assabile.com/adhan-call-prayer/essam-boukhari-14.htm"
    ),
    AL_AQSA(
        id = "AL_AQSA",
        titleEn = "Adhan Al-Aqsa Mosque",
        titleAr = "أذان المسجد الأقصى المبارك",
        reciterEn = "Jerusalem Al-Quds",
        reciterAr = "القدس الشريف",
        referenceUrl = "https://www.assabile.com/adhan-call-prayer/al-quds-10.htm"
    ),
    ABDUL_BASIT(
        id = "ABDUL_BASIT",
        titleEn = "Adhan Abdul Basit Abdus Samad",
        titleAr = "أذان عبد الباسط عبد الصمد",
        reciterEn = "Egypt (Classic)",
        reciterAr = "مصر (التراث الخالد)",
        referenceUrl = "https://www.assabile.com/adhan-call-prayer/abdelbasset-abdessamad-4.htm"
    ),
    TAKBEER(
        id = "TAKBEER",
        titleEn = "Gentle Takbeer Call",
        titleAr = "تكبيرات الأذان الهادئة",
        reciterEn = "Short Tone Reminder",
        reciterAr = "تنبيه تكبيرات هادئ",
        referenceUrl = "https://www.assabile.com/adhan-call-prayer"
    ),
    SYSTEM_DEFAULT(
        id = "SYSTEM_DEFAULT",
        titleEn = "Standard System Tone",
        titleAr = "نغمة الجهاز الافتراضية",
        reciterEn = "Device Notification Chime",
        reciterAr = "نغمة الإشعارات القياسية",
        referenceUrl = ""
    );

    companion object {
        fun fromId(id: String?): AdhanSoundOption {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: MAKKAH
        }
    }
}
