package com.example.expensetracker.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.location.LocationHelper
import com.example.expensetracker.domain.CityLocation
import com.example.expensetracker.domain.PrayerTimesCalculator
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerQiblahScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }

    var selectedCity by remember { mutableStateOf(PrayerTimesCalculator.PRESET_CITIES.first()) }
    var cityMenuExpanded by remember { mutableStateOf(false) }
    var isUsingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            val detected = locationHelper.detectLocationInfo()
            if (detected != null) {
                val tzOffset = (TimeZone.getDefault().rawOffset / 3600000.0)
                selectedCity = CityLocation(
                    nameEn = detected.cityName ?: "Current Location",
                    nameAr = "موقعي الحالي",
                    latitude = detected.latitude,
                    longitude = detected.longitude,
                    timezoneOffsetHours = tzOffset
                )
                isUsingGps = true
                gpsStatusMessage = "GPS active: ${String.format(java.util.Locale.US, "%.4f", detected.latitude)}, ${String.format(java.util.Locale.US, "%.4f", detected.longitude)}"
            } else {
                gpsStatusMessage = "GPS location unavailable, using preset city."
            }
        } else {
            gpsStatusMessage = "Location permission denied. You can select a city manually."
        }
    }

    val schedule = remember(selectedCity) {
        PrayerTimesCalculator.calculateDaySchedule(selectedCity, Date())
    }

    val animatedBearing by animateFloatAsState(
        targetValue = schedule.qiblahBearingDegrees,
        animationSpec = tween(durationMillis = 600),
        label = "qiblahBearing"
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.prayer_qiblah_title),
                navController = navController,
                canNavigateBack = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Location Selector & GPS Auto-detect Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.select_city_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // GPS Button
                        TextButton(
                            onClick = {
                                if (locationHelper.hasLocationPermission()) {
                                    val detected = locationHelper.detectLocationInfo()
                                    if (detected != null) {
                                        val tzOffset = (TimeZone.getDefault().rawOffset / 3600000.0)
                                        selectedCity = CityLocation(
                                            nameEn = detected.cityName ?: "Current Location",
                                            nameAr = "موقعي الحالي",
                                            latitude = detected.latitude,
                                            longitude = detected.longitude,
                                            timezoneOffsetHours = tzOffset
                                        )
                                        isUsingGps = true
                                        gpsStatusMessage = "GPS active: ${String.format(java.util.Locale.US, "%.3f", detected.latitude)}, ${String.format(java.util.Locale.US, "%.3f", detected.longitude)}"
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isUsingGps) Icons.Outlined.GpsFixed else Icons.Outlined.MyLocation,
                                contentDescription = null,
                                tint = if (isUsingGps) FinancePositive else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUsingGps) "GPS Active" else "Use Device GPS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUsingGps) FinancePositive else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = cityMenuExpanded,
                        onExpandedChange = { cityMenuExpanded = !cityMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = "${selectedCity.nameEn} • ${selectedCity.nameAr}",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isUsingGps) Icons.Outlined.GpsFixed else Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = if (isUsingGps) FinancePositive else MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = cityMenuExpanded,
                            onDismissRequest = { cityMenuExpanded = false }
                        ) {
                            PrayerTimesCalculator.PRESET_CITIES.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text("${city.nameEn} • ${city.nameAr}") },
                                    onClick = {
                                        selectedCity = city
                                        isUsingGps = false
                                        gpsStatusMessage = null
                                        cityMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (gpsStatusMessage != null) {
                        Text(
                            text = gpsStatusMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Next Prayer Hero Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.upcoming_prayer_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = schedule.nextPrayerName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = schedule.nextPrayerTime,
                        style = MaterialTheme.typography.headlineLarge.withTabularNums(),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 5 Daily Prayers Schedule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.daily_prayer_schedule),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    listOf(
                        "Fajr" to schedule.fajr,
                        "Sunrise" to schedule.sunrise,
                        "Dhuhr" to schedule.dhuhr,
                        "Asr" to schedule.asr,
                        "Maghrib" to schedule.maghrib,
                        "Isha" to schedule.isha
                    ).forEach { (name, time) ->
                        val isNext = schedule.nextPrayerName == name
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (name == "Sunrise") Icons.Outlined.WbTwilight
                                        else if (name == "Maghrib" || name == "Isha") Icons.Outlined.NightsStay
                                        else Icons.Outlined.WbSunny,
                                        contentDescription = null,
                                        tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                Text(
                                    text = time,
                                    style = MaterialTheme.typography.bodyLarge.withTabularNums(),
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Qiblah Compass Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.qiblah_direction_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", schedule.qiblahBearingDegrees)}° from True North",
                        style = MaterialTheme.typography.bodyMedium.withTabularNums(),
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Compass Canvas
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(180.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.minDimension / 2f - 12.dp.toPx()

                            // Outer circle
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.2f),
                                radius = radius,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )

                            // North indicator marker
                            drawLine(
                                color = Color.Red,
                                start = center,
                                end = Offset(center.x, center.y - radius),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Rotated Qiblah Needle pointing to Kaaba
                            rotate(degrees = animatedBearing, pivot = center) {
                                drawLine(
                                    color = Color(0xFF10B981),
                                    start = center,
                                    end = Offset(center.x, center.y - radius + 8.dp.toPx()),
                                    strokeWidth = 6.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = Color(0xFF10B981),
                                    radius = 7.dp.toPx(),
                                    center = Offset(center.x, center.y - radius + 8.dp.toPx())
                                )
                            }

                            // Center pivot circle
                            drawCircle(
                                color = Color.DarkGray,
                                radius = 6.dp.toPx(),
                                center = center
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FinancePositive.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Kaaba Direction (Makkah)",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinancePositive,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
