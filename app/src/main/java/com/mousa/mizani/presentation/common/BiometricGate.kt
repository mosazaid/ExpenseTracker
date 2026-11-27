package com.mousa.mizani.presentation.common

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
import com.mousa.mizani.R
import com.mousa.mizani.presentation.feature.settings.BiometricLockViewModel

/** Shows [content] only once onboarding is done and the lock is known to be off or opened. */
@Composable
fun BiometricGate(
    onboardingCompleted: Boolean?,
    onboarding: @Composable () -> Unit,
    viewModel: BiometricLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val screen = appScreen(onboardingCompleted, biometricEnabled, isUnlocked)
    val isLocked = screen == AppScreen.LOCK

    val title = stringResource(R.string.biometric_prompt_title)
    val subtitle = stringResource(R.string.biometric_prompt_subtitle)
    val notRecognized = stringResource(R.string.biometric_not_recognized)

    DisposableEffect(lifecycleOwner, biometricEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (biometricEnabled == true && event == Lifecycle.Event.ON_STOP) {
                viewModel.lock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(isLocked) {
        if (isLocked) {
            activity?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val requestUnlock = {
        if (activity != null) {
            viewModel.requestUnlock(activity, title, subtitle, notRecognized)
        }
    }

    LaunchedEffect(isLocked) {
        if (isLocked) requestUnlock()
    }

    when (screen) {
        AppScreen.BLANK -> Unit
        AppScreen.ONBOARDING -> onboarding()
        AppScreen.LOCK -> BiometricLockScreen(
            errorMessage = errorMessage,
            onUnlockClick = requestUnlock
        )
        AppScreen.CONTENT -> content()
    }
}
