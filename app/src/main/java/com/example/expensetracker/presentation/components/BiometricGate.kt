package com.example.expensetracker.presentation.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.expensetracker.R
import com.example.expensetracker.presentation.viewModel.BiometricLockViewModel

@Composable
fun BiometricGate(
    viewModel: BiometricLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    if (activity == null) {
        content()
        return
    }

    val title = stringResource(R.string.biometric_prompt_title)
    val subtitle = stringResource(R.string.biometric_prompt_subtitle)
    val notRecognized = stringResource(R.string.biometric_not_recognized)

    DisposableEffect(lifecycleOwner, biometricEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (biometricEnabled && event == Lifecycle.Event.ON_STOP) {
                viewModel.lock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(isUnlocked, biometricEnabled) {
        if (biometricEnabled && !isUnlocked) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(biometricEnabled, isUnlocked) {
        if (biometricEnabled && !isUnlocked) {
            viewModel.requestUnlock(activity, title, subtitle, notRecognized)
        }
    }

    when {
        !biometricEnabled || isUnlocked -> content()
        else -> {
            BiometricLockScreen(
                errorMessage = errorMessage,
                onUnlockClick = {
                    viewModel.requestUnlock(activity, title, subtitle, notRecognized)
                }
            )
        }
    }
}
