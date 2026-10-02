package com.example.expensetracker.presentation.screens

import android.Manifest
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.core.location.LocationHelper
import com.example.expensetracker.core.permission.PermissionHelper
import com.example.expensetracker.core.sensor.CompassReading
import com.example.expensetracker.core.sensor.CompassSensorManager
import com.example.expensetracker.domain.PrayerCalculationMethod
import com.example.expensetracker.domain.PrayerTimesCalculator
import com.example.expensetracker.presentation.components.AppTopBar
import com.example.expensetracker.presentation.components.PermissionRationaleDialog
import com.example.expensetracker.presentation.components.PermissionType
import com.example.expensetracker.presentation.theme.FinancePositive
import com.example.expensetracker.presentation.theme.withTabularNums
import com.example.expensetracker.presentation.viewModel.PrayerQiblahViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerQiblahScreen(
    navController: NavController,
    viewModel: PrayerQiblahViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val locationHelper = remember { LocationHelper(context) }
    val compassManager = remember { CompassSensorManager(context) }

    val uiState by viewModel.uiState.collectAsState()
    val compassReading by compassManager.reading.collectAsState()

    var showMethodDialog by remember { mutableStateOf(false) }
    var cityMenuExpanded by remember { mutableStateOf(false) }
    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDeniedLocation by remember { mutableStateOf(false) }

    // Start/Stop compass sensors with lifecycle
    DisposableEffect(Unit) {
        compassManager.start()
        onDispose {
            compassManager.stop()
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            val detected = locationHelper.detectLocationInfo()
            if (detected != null) {
                viewModel.onGpsLocationDetected(
                    lat = detected.latitude,
                    lng = detected.longitude,
                    cityName = detected.cityName,
                    countryCode = detected.countryCode
                )
            }
        } else {
            val isPermanently = (context as? android.app.Activity)?.let {
                !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
            isPermanentlyDeniedLocation = isPermanently
            showLocationRationaleDialog = true
        }
    }

    // Relative alignment angle between Qiblah and device heading
    val targetQiblahBearing = uiState.schedule.qiblahBearingDegrees
    val deviceAzimuth = compassReading.azimuth

    val relativeAngle = remember(targetQiblahBearing, deviceAzimuth) {
        var diff = (targetQiblahBearing - deviceAzimuth) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        diff
    }

    val isAligned = abs(relativeAngle) <= 3.5f

    // Trigger haptic feedback when user achieves alignment
    var prevAligned by remember { mutableStateOf(false) }
    LaunchedEffect(isAligned) {
        if (isAligned && !prevAligned) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
        prevAligned = isAligned
    }

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
            // Location Selector & GPS Card
            LocationSelectorCard(
                cityNameEn = uiState.selectedCity.nameEn,
                cityNameAr = uiState.selectedCity.nameAr,
                isUsingGps = uiState.isUsingGps,
                gpsStatusMessage = uiState.gpsStatusMessage,
                cityMenuExpanded = cityMenuExpanded,
                onMenuExpandChange = { cityMenuExpanded = it },
                onSelectCity = { city ->
                    viewModel.selectCity(city)
                    cityMenuExpanded = false
                },
                onGpsClick = {
                    if (PermissionHelper.hasLocationPermission(context)) {
                        val detected = locationHelper.detectLocationInfo()
                        if (detected != null) {
                            viewModel.onGpsLocationDetected(
                                lat = detected.latitude,
                                lng = detected.longitude,
                                cityName = detected.cityName,
                                countryCode = detected.countryCode
                            )
                        }
                    } else {
                        val shouldShowRationale = (context as? android.app.Activity)?.let {
                            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
                        } ?: false
                        if (shouldShowRationale) {
                            isPermanentlyDeniedLocation = false
                            showLocationRationaleDialog = true
                        } else {
                            locationPermissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
                        }
                    }
                }
            )

            // Dynamic Sensor-Driven Qiblah Compass Card
            QiblahCompassCard(
                compassReading = compassReading,
                qiblahBearing = targetQiblahBearing,
                relativeAngle = relativeAngle,
                isAligned = isAligned
            )

            // Calculation Method Card
            CalculationMethodCard(
                method = uiState.calculationMethod,
                isAuto = uiState.isAutoMethod,
                detectedCountryCode = uiState.detectedCountryCode,
                onChangeClick = { showMethodDialog = true }
            )

            // Upcoming Next Prayer Banner
            NextPrayerHeroCard(
                prayerName = uiState.schedule.nextPrayerName,
                prayerTime = uiState.schedule.nextPrayerTime
            )

            // 5 Daily Prayers Schedule with reminder toggles
            DailyPrayersScheduleCard(
                schedule = uiState.schedule,
                fajrAlert = uiState.fajrAlertEnabled,
                dhuhrAlert = uiState.dhuhrAlertEnabled,
                asrAlert = uiState.asrAlertEnabled,
                maghribAlert = uiState.maghribAlertEnabled,
                ishaAlert = uiState.ishaAlertEnabled,
                onTogglePrayerAlert = { prayerName, enabled ->
                    viewModel.toggleIndividualPrayer(prayerName, enabled)
                }
            )
        }
    }

    // Calculation Method Selection Dialog
    if (showMethodDialog) {
        CalculationMethodDialog(
            currentMethod = uiState.calculationMethod,
            isAuto = uiState.isAutoMethod,
            detectedCountryCode = uiState.detectedCountryCode,
            onSelectMethod = { method, isAuto ->
                viewModel.setCalculationMethod(method, isAuto)
                showMethodDialog = false
            },
            onDismiss = { showMethodDialog = false }
        )
    }

    if (showLocationRationaleDialog) {
        PermissionRationaleDialog(
            permissionType = PermissionType.LOCATION,
            isPermanentlyDenied = isPermanentlyDeniedLocation,
            onConfirm = {
                showLocationRationaleDialog = false
                if (isPermanentlyDeniedLocation) {
                    PermissionHelper.openAppSettings(context)
                } else {
                    locationPermissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
                }
            },
            onDismiss = { showLocationRationaleDialog = false }
        )
    }
}

// -------------------------------------------------------------
// Location Selector Card
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSelectorCard(
    cityNameEn: String,
    cityNameAr: String,
    isUsingGps: Boolean,
    gpsStatusMessage: String?,
    cityMenuExpanded: Boolean,
    onMenuExpandChange: (Boolean) -> Unit,
    onSelectCity: (com.example.expensetracker.domain.CityLocation) -> Unit,
    onGpsClick: () -> Unit
) {
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

                TextButton(
                    onClick = onGpsClick,
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
                onExpandedChange = onMenuExpandChange
            ) {
                OutlinedTextField(
                    value = "$cityNameEn • $cityNameAr",
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
                    onDismissRequest = { onMenuExpandChange(false) }
                ) {
                    PrayerTimesCalculator.PRESET_CITIES.forEach { city ->
                        DropdownMenuItem(
                            text = { Text("${city.nameEn} • ${city.nameAr}") },
                            onClick = { onSelectCity(city) }
                        )
                    }
                }
            }

            if (gpsStatusMessage != null) {
                Text(
                    text = gpsStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Interactive Dynamic Qiblah Compass Card
// -------------------------------------------------------------
@Composable
private fun QiblahCompassCard(
    compassReading: CompassReading,
    qiblahBearing: Float,
    relativeAngle: Float,
    isAligned: Boolean
) {
    val emeraldGreen = FinancePositive
    val goldAccent = Color(0xFFD4AF37)

    // Animated glow pulse when aligned
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dialBorderColor by animateColorAsState(
        targetValue = if (isAligned) emeraldGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        animationSpec = tween(300),
        label = "dialBorderColor"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (isAligned) 2.dp else 1.dp,
            color = if (isAligned) emeraldGreen.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and target bearing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.qiblah_direction_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Target: ${String.format(java.util.Locale.US, "%.1f", qiblahBearing)}° from True North",
                        style = MaterialTheme.typography.bodySmall.withTabularNums(),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Accuracy chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (compassReading.accuracy) {
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> emeraldGreen.copy(alpha = 0.12f)
                        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> goldAccent.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    }
                ) {
                    Text(
                        text = when (compassReading.accuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High Precision"
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Med Accuracy"
                            else -> "Calibrate (∞)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (compassReading.accuracy) {
                            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> emeraldGreen
                            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> goldAccent
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Real-Time Compass Dial
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp)
            ) {
                // Outer glowing halo when aligned
                if (isAligned) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        emeraldGreen.copy(alpha = pulseAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // Canvas drawing the rotating compass dial and needles
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val outlineColor = MaterialTheme.colorScheme.outline
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 14.dp.toPx()

                    // Background dial circle
                    drawCircle(
                        color = onSurfaceColor.copy(alpha = 0.04f),
                        radius = radius,
                        center = center
                    )

                    // Outer dial border
                    drawCircle(
                        color = dialBorderColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = if (isAligned) 3.5.dp.toPx() else 2.dp.toPx())
                    )

                    // Rotating Dial (rotates by -azimuth so True North stays North)
                    val dialRotation = -compassReading.azimuth
                    rotate(degrees = dialRotation, pivot = center) {
                        // Ticks around the dial (every 10° minor, 30° major)
                        for (degree in 0 until 360 step 10) {
                            val isMajor = degree % 30 == 0
                            val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                            val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                            val rad = Math.toRadians(degree.toDouble())

                            val startX = center.x + (radius - tickLength) * sin(rad).toFloat()
                            val startY = center.y - (radius - tickLength) * cos(rad).toFloat()
                            val endX = center.x + radius * sin(rad).toFloat()
                            val endY = center.y - radius * cos(rad).toFloat()

                            drawLine(
                                color = if (isMajor) outlineColor.copy(alpha = 0.8f) else outlineColor.copy(alpha = 0.35f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth
                            )
                        }

                        // Cardinal Directions
                        // North (Red indicator)
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = center,
                            end = Offset(center.x, center.y - radius + 12.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Qiblah Needle on the rotating dial pointing at target bearing
                        rotate(degrees = qiblahBearing, pivot = center) {
                            // Needle shaft
                            drawLine(
                                color = if (isAligned) emeraldGreen else goldAccent,
                                start = center,
                                end = Offset(center.x, center.y - radius + 14.dp.toPx()),
                                strokeWidth = 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )

                            // Pointer arrowhead (pointing at Kaaba)
                            val headPath = Path().apply {
                                val topY = center.y - radius + 4.dp.toPx()
                                moveTo(center.x, topY)
                                lineTo(center.x - 7.dp.toPx(), topY + 14.dp.toPx())
                                lineTo(center.x + 7.dp.toPx(), topY + 14.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = headPath,
                                color = if (isAligned) emeraldGreen else goldAccent
                            )
                        }
                    }

                    // Top Device Pointer (Fixed at 12 o'clock, showing phone heading)
                    val fixedPointerPath = Path().apply {
                        moveTo(center.x, 2.dp.toPx())
                        lineTo(center.x - 8.dp.toPx(), 16.dp.toPx())
                        lineTo(center.x + 8.dp.toPx(), 16.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = fixedPointerPath,
                        color = if (isAligned) emeraldGreen else primaryColor
                    )

                    // Center Hub
                    drawCircle(
                        color = if (isAligned) emeraldGreen else onSurfaceColor,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = goldAccent,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                // Center Kaaba Emblem
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { /* no-op center tap */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mosque,
                        contentDescription = "Kaaba",
                        tint = if (isAligned) emeraldGreen else goldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Real-time Guidance Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isAligned) emeraldGreen.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isAligned) emeraldGreen.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAligned) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = emeraldGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.qiblah_aligned),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = emeraldGreen
                        )
                    } else {
                        val turnAngle = abs(relativeAngle).roundToInt()
                        val isTurnRight = relativeAngle > 0
                        Icon(
                            imageVector = if (isTurnRight) Icons.AutoMirrored.Outlined.ArrowForward else Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTurnRight) stringResource(R.string.qiblah_turn_right, turnAngle)
                            else stringResource(R.string.qiblah_turn_left, turnAngle),
                            style = MaterialTheme.typography.titleSmall.withTabularNums(),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Calibration / Tilt Warning if needed
            val isTilted = abs(compassReading.pitch) > 35f || abs(compassReading.roll) > 35f
            if (isTilted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ScreenRotation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.qiblah_level_phone_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else if (compassReading.accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                Text(
                    text = stringResource(R.string.qiblah_calibrate_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Calculation Method Card
// -------------------------------------------------------------
@Composable
private fun CalculationMethodCard(
    method: PrayerCalculationMethod,
    isAuto: Boolean,
    detectedCountryCode: String?,
    onChangeClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prayer_calculation_method),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (isAuto) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Auto (${detectedCountryCode ?: "GPS"})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = method.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = method.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            OutlinedButton(
                onClick = onChangeClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.prayer_change_method),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Next Prayer Hero Card
// -------------------------------------------------------------
@Composable
private fun NextPrayerHeroCard(
    prayerName: String,
    prayerTime: String
) {
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
                    text = prayerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = prayerTime,
                style = MaterialTheme.typography.headlineLarge.withTabularNums(),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// -------------------------------------------------------------
// 5 Daily Prayers Schedule with Per-Prayer Reminder Toggles
// -------------------------------------------------------------
@Composable
private fun DailyPrayersScheduleCard(
    schedule: com.example.expensetracker.domain.PrayerSchedule,
    fajrAlert: Boolean,
    dhuhrAlert: Boolean,
    asrAlert: Boolean,
    maghribAlert: Boolean,
    ishaAlert: Boolean,
    onTogglePrayerAlert: (String, Boolean) -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.daily_prayer_schedule),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            val prayerItems = listOf(
                PrayerRowData("Fajr", "الفجر", schedule.fajr, fajrAlert, canRemind = true),
                PrayerRowData("Sunrise", "الشروق", schedule.sunrise, false, canRemind = false),
                PrayerRowData("Dhuhr", "الظهر", schedule.dhuhr, dhuhrAlert, canRemind = true),
                PrayerRowData("Asr", "العصر", schedule.asr, asrAlert, canRemind = true),
                PrayerRowData("Maghrib", "المغرب", schedule.maghrib, maghribAlert, canRemind = true),
                PrayerRowData("Isha", "العشاء", schedule.isha, ishaAlert, canRemind = true)
            )

            prayerItems.forEach { item ->
                val isNext = schedule.nextPrayerName == item.nameEn
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isNext) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = when (item.nameEn) {
                                    "Sunrise" -> Icons.Outlined.WbTwilight
                                    "Maghrib", "Isha" -> Icons.Outlined.NightsStay
                                    else -> Icons.Outlined.WbSunny
                                },
                                contentDescription = null,
                                tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "${item.nameEn} • ${item.nameAr}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = item.time,
                                style = MaterialTheme.typography.bodyLarge.withTabularNums(),
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            if (item.canRemind) {
                                IconButton(
                                    onClick = { onTogglePrayerAlert(item.nameEn, !item.alertEnabled) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.alertEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                                        contentDescription = "Toggle reminder for ${item.nameEn}",
                                        tint = if (item.alertEnabled) FinancePositive else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PrayerRowData(
    val nameEn: String,
    val nameAr: String,
    val time: String,
    val alertEnabled: Boolean,
    val canRemind: Boolean
)

// -------------------------------------------------------------
// Calculation Method Selection Dialog
// -------------------------------------------------------------
@Composable
private fun CalculationMethodDialog(
    currentMethod: PrayerCalculationMethod,
    isAuto: Boolean,
    detectedCountryCode: String?,
    onSelectMethod: (PrayerCalculationMethod, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val autoMethod = remember(detectedCountryCode) {
        PrayerCalculationMethod.autoDetect(detectedCountryCode)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.prayer_calculation_method),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Auto-detect item
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAuto) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isAuto) 2.dp else 1.dp,
                            color = if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMethod(autoMethod, true) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Auto-detect by Location",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isAuto) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Recommends: ${autoMethod.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                items(PrayerCalculationMethod.entries.toTypedArray()) { method ->
                    val isSelected = !isAuto && method == currentMethod
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMethod(method, false) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = method.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = method.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
