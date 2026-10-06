package com.example.expensetracker.presentation.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.location.LocationHelper
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.CityLocation
import com.example.expensetracker.domain.PrayerCalculationMethod
import com.example.expensetracker.domain.PrayerSchedule
import com.example.expensetracker.domain.PrayerTimesCalculator
import com.example.expensetracker.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

data class PrayerUiState(
    val selectedCity: CityLocation = PrayerTimesCalculator.PRESET_CITIES.first(),
    val isUsingGps: Boolean = false,
    val gpsStatusMessage: String? = null,
    val calculationMethod: PrayerCalculationMethod = PrayerCalculationMethod.JORDAN,
    val isAutoMethod: Boolean = true,
    val detectedCountryCode: String? = null,
    val is24Hour: Boolean = false,
    val schedule: PrayerSchedule = PrayerTimesCalculator.calculateDaySchedule(
        city = PrayerTimesCalculator.PRESET_CITIES.first(),
        date = Date(),
        method = PrayerCalculationMethod.JORDAN,
        is24Hour = false
    ),
    val prayerAlertsEnabled: Boolean = true,
    val fajrAlertEnabled: Boolean = true,
    val dhuhrAlertEnabled: Boolean = true,
    val asrAlertEnabled: Boolean = true,
    val maghribAlertEnabled: Boolean = true,
    val ishaAlertEnabled: Boolean = true
)

@HiltViewModel
class PrayerQiblahViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        loadPreferencesAndLocation()
        observeTimeFormat()
    }

    private fun observeTimeFormat() {
        viewModelScope.launch {
            userPreferences.timeFormat.collect { formatPref ->
                val is24 = formatPref == com.example.expensetracker.data.preferences.TimeFormatPreference.H24
                if (_uiState.value.is24Hour != is24) {
                    val newSchedule = PrayerTimesCalculator.calculateDaySchedule(
                        city = _uiState.value.selectedCity,
                        date = Date(),
                        method = _uiState.value.calculationMethod,
                        is24Hour = is24
                    )
                    _uiState.value = _uiState.value.copy(
                        is24Hour = is24,
                        schedule = newSchedule
                    )
                }
            }
        }
    }

    private fun loadPreferencesAndLocation() {
        viewModelScope.launch {
            val savedMethodKey = userPreferences.prayerCalculationMethod.first()
            val timeFormatPref = userPreferences.timeFormat.first()
            val is24 = timeFormatPref == com.example.expensetracker.data.preferences.TimeFormatPreference.H24
            val locationHelper = LocationHelper(context)
            val detected = locationHelper.detectLocationInfo()

            val effectiveMethod = if (savedMethodKey != null) {
                try {
                    PrayerCalculationMethod.valueOf(savedMethodKey)
                } catch (e: Exception) {
                    PrayerCalculationMethod.autoDetect(detected?.countryCode)
                }
            } else {
                PrayerCalculationMethod.autoDetect(detected?.countryCode)
            }

            val isAuto = savedMethodKey == null

            val initialCity = if (detected != null) {
                val tzOffset = (TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0)
                CityLocation(
                    nameEn = detected.cityName ?: "Current Location",
                    nameAr = "موقعي الحالي",
                    latitude = detected.latitude,
                    longitude = detected.longitude,
                    timezoneOffsetHours = tzOffset
                )
            } else {
                PrayerTimesCalculator.PRESET_CITIES.first()
            }

            val alertsEnabled = userPreferences.prayerAlertsEnabled.first()
            val fajr = userPreferences.fajrAlertEnabled.first()
            val dhuhr = userPreferences.dhuhrAlertEnabled.first()
            val asr = userPreferences.asrAlertEnabled.first()
            val maghrib = userPreferences.maghribAlertEnabled.first()
            val isha = userPreferences.ishaAlertEnabled.first()

            val newSchedule = PrayerTimesCalculator.calculateDaySchedule(
                city = initialCity,
                date = Date(),
                method = effectiveMethod,
                is24Hour = is24
            )

            _uiState.value = _uiState.value.copy(
                selectedCity = initialCity,
                isUsingGps = detected != null,
                gpsStatusMessage = if (detected != null) "Location: ${detected.cityName ?: "Detected"} (${detected.countryCode ?: ""})" else null,
                calculationMethod = effectiveMethod,
                isAutoMethod = isAuto,
                detectedCountryCode = detected?.countryCode,
                is24Hour = is24,
                schedule = newSchedule,
                prayerAlertsEnabled = alertsEnabled,
                fajrAlertEnabled = fajr,
                dhuhrAlertEnabled = dhuhr,
                asrAlertEnabled = asr,
                maghribAlertEnabled = maghrib,
                ishaAlertEnabled = isha
            )
            syncAllAlarms()
        }
    }

    fun selectCity(city: CityLocation) {
        val newSchedule = PrayerTimesCalculator.calculateDaySchedule(
            city = city,
            date = Date(),
            method = _uiState.value.calculationMethod,
            is24Hour = _uiState.value.is24Hour
        )
        _uiState.value = _uiState.value.copy(
            selectedCity = city,
            isUsingGps = false,
            gpsStatusMessage = null,
            schedule = newSchedule
        )
        syncAllAlarms()
    }

    fun onGpsLocationDetected(lat: Double, lng: Double, cityName: String?, countryCode: String?) {
        val tzOffset = (TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0)
        val city = CityLocation(
            nameEn = cityName ?: "Current Location",
            nameAr = "موقعي الحالي",
            latitude = lat,
            longitude = lng,
            timezoneOffsetHours = tzOffset
        )

        // If method is auto, update it based on detected country
        val method = if (_uiState.value.isAutoMethod) {
            PrayerCalculationMethod.autoDetect(countryCode)
        } else {
            _uiState.value.calculationMethod
        }

        val newSchedule = PrayerTimesCalculator.calculateDaySchedule(
            city = city,
            date = Date(),
            method = method,
            is24Hour = _uiState.value.is24Hour
        )

        _uiState.value = _uiState.value.copy(
            selectedCity = city,
            isUsingGps = true,
            gpsStatusMessage = "GPS active: ${String.format(java.util.Locale.US, "%.3f", lat)}, ${String.format(java.util.Locale.US, "%.3f", lng)}",
            calculationMethod = method,
            detectedCountryCode = countryCode,
            schedule = newSchedule
        )
        syncAllAlarms()
    }

    fun setCalculationMethod(method: PrayerCalculationMethod, isAuto: Boolean = false) {
        viewModelScope.launch {
            if (isAuto) {
                userPreferences.setPrayerCalculationMethod(null)
            } else {
                userPreferences.setPrayerCalculationMethod(method.name)
            }

            val newSchedule = PrayerTimesCalculator.calculateDaySchedule(
                city = _uiState.value.selectedCity,
                date = Date(),
                method = method,
                is24Hour = _uiState.value.is24Hour
            )

            _uiState.value = _uiState.value.copy(
                calculationMethod = method,
                isAutoMethod = isAuto,
                schedule = newSchedule
            )
            syncAllAlarms()
        }
    }

    fun togglePrayerAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setPrayerAlertsEnabled(enabled)
            _uiState.value = _uiState.value.copy(prayerAlertsEnabled = enabled)
            syncAllAlarms()
        }
    }

    fun toggleIndividualPrayer(prayerName: String, enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setPrayerReminderEnabled(prayerName, enabled)
            _uiState.value = when (prayerName.lowercase(java.util.Locale.US)) {
                "fajr" -> _uiState.value.copy(fajrAlertEnabled = enabled)
                "dhuhr" -> _uiState.value.copy(dhuhrAlertEnabled = enabled)
                "asr" -> _uiState.value.copy(asrAlertEnabled = enabled)
                "maghrib" -> _uiState.value.copy(maghribAlertEnabled = enabled)
                "isha" -> _uiState.value.copy(ishaAlertEnabled = enabled)
                else -> _uiState.value
            }

            if (enabled && _uiState.value.prayerAlertsEnabled) {
                val timeStr = when (prayerName.lowercase(java.util.Locale.US)) {
                    "fajr" -> _uiState.value.schedule.fajr24
                    "dhuhr" -> _uiState.value.schedule.dhuhr24
                    "asr" -> _uiState.value.schedule.asr24
                    "maghrib" -> _uiState.value.schedule.maghrib24
                    "isha" -> _uiState.value.schedule.isha24
                    else -> ""
                }
                schedulePrayerAlarm(prayerName, timeStr)
            } else {
                alarmScheduler.cancelPrayerAlarm(prayerName)
            }
        }
    }

    private fun schedulePrayerAlarm(prayerName: String, timeStr: String) {
        try {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                var hour = parts[0].trim().toInt()
                val minute = parts[1].trim().take(2).toInt()
                if (timeStr.contains("PM", ignoreCase = true) && hour < 12) hour += 12
                if (timeStr.contains("AM", ignoreCase = true) && hour == 12) hour = 0
                alarmScheduler.schedulePrayerAlarm(prayerName, hour, minute)
            }
        } catch (_: Exception) {}
    }

    fun syncAllAlarms() {
        val state = _uiState.value
        if (!state.prayerAlertsEnabled) {
            listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach {
                alarmScheduler.cancelPrayerAlarm(it)
            }
            return
        }

        if (state.fajrAlertEnabled) schedulePrayerAlarm("Fajr", state.schedule.fajr24) else alarmScheduler.cancelPrayerAlarm("Fajr")
        if (state.dhuhrAlertEnabled) schedulePrayerAlarm("Dhuhr", state.schedule.dhuhr24) else alarmScheduler.cancelPrayerAlarm("Dhuhr")
        if (state.asrAlertEnabled) schedulePrayerAlarm("Asr", state.schedule.asr24) else alarmScheduler.cancelPrayerAlarm("Asr")
        if (state.maghribAlertEnabled) schedulePrayerAlarm("Maghrib", state.schedule.maghrib24) else alarmScheduler.cancelPrayerAlarm("Maghrib")
        if (state.ishaAlertEnabled) schedulePrayerAlarm("Isha", state.schedule.isha24) else alarmScheduler.cancelPrayerAlarm("Isha")
    }
}
