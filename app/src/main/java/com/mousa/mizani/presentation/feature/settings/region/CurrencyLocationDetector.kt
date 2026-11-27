package com.mousa.mizani.presentation.feature.settings.region

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.mousa.mizani.core.location.LocationHelper
import com.mousa.mizani.core.permission.PermissionHelper
import com.mousa.mizani.presentation.common.PermissionRationaleDialog
import com.mousa.mizani.presentation.common.PermissionType

sealed interface CurrencyDetection {
    data class Found(val currencyCode: String, val place: String) : CurrencyDetection
    data object Unavailable : CurrencyDetection
}

private fun LocationHelper.detectCurrency(): CurrencyDetection {
    val info = detectLocationInfo() ?: return CurrencyDetection.Unavailable
    return CurrencyDetection.Found(
        currencyCode = info.detectedCurrencyCode,
        place = info.cityName ?: info.countryCode.orEmpty()
    )
}

private fun Activity?.shouldExplainLocation(): Boolean =
    this != null && ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

/**
 * Handles the location permission, including its explanation dialog, and finds the currency for
 * the current location.
 *
 * @return the action that starts a detection; the outcome is passed to [onResult]. Nothing is
 * passed when the user refuses the permission.
 */
@Composable
fun rememberCurrencyLocationDetector(onResult: (CurrencyDetection) -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = context as? Activity
    val locationHelper = remember(context) { LocationHelper(context) }
    val currentOnResult by rememberUpdatedState(onResult)
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var isPermanentlyDenied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = PermissionHelper.LOCATION_PERMISSIONS.any { permissions[it] == true }
        if (isGranted) {
            currentOnResult(locationHelper.detectCurrency())
        } else {
            isPermanentlyDenied = activity != null && !activity.shouldExplainLocation()
            showRationale = true
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            permissionType = PermissionType.LOCATION,
            isPermanentlyDenied = isPermanentlyDenied,
            onConfirm = {
                showRationale = false
                if (isPermanentlyDenied) {
                    PermissionHelper.openAppSettings(context)
                } else {
                    permissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
                }
            },
            onDismiss = { showRationale = false }
        )
    }

    return {
        when {
            PermissionHelper.hasLocationPermission(context) ->
                currentOnResult(locationHelper.detectCurrency())

            activity.shouldExplainLocation() -> {
                isPermanentlyDenied = false
                showRationale = true
            }

            else -> permissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
        }
    }
}
