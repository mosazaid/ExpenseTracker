package com.mousa.mizani

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mousa.mizani.core.launch.LaunchRequest
import com.mousa.mizani.core.launch.launchRequestFromExtras
import com.mousa.mizani.core.launch.launchRequestOnCreate
import com.mousa.mizani.core.locale.LocaleHelper
import com.mousa.mizani.core.startup.OpeningPromptSession
import com.mousa.mizani.data.preferences.ThemeMode
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.presentation.common.BiometricGate
import com.mousa.mizani.presentation.feature.main.MainScreen
import com.mousa.mizani.presentation.theme.MizaniTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.mousa.mizani.presentation.feature.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var openingPromptSession: OpeningPromptSession

    private val pendingLaunch = mutableStateOf(LaunchRequest())

    // The activity does not inherit the application's wrapped context, so apply the language here.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingLaunch.value = launchRequestOnCreate(
            isRecreation = savedInstanceState != null,
            isFromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0,
            unhandled = savedInstanceState?.readLaunchRequest() ?: LaunchRequest(),
            fromIntent = intent.toLaunchRequest()
        )
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val themePalette by userPreferences.themePalette.collectAsState(initial = com.mousa.mizani.data.preferences.ThemePalette.EMERALD)
            val onboardingCompleted by userPreferences.onboardingCompleted
                .collectAsState<Boolean, Boolean?>(initial = null)
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MizaniTheme(
                darkTheme = isDark,
                palette = themePalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    NotificationPermissionRequester()

                    BiometricGate(
                        onboardingCompleted = onboardingCompleted,
                        onboarding = {
                            OnboardingScreen(
                                // Edge-to-edge: keep the bottom buttons above the system navigation bar.
                                // (The in-app route gets these insets from MainScreen's Scaffold.)
                                modifier = Modifier.safeDrawingPadding(),
                                onFinish = {
                                    lifecycleScope.launch {
                                        userPreferences.setOnboardingCompleted(true)
                                    }
                                }
                            )
                        }
                    ) {
                        MainScreen(
                            initialRecurringId = pendingLaunch.value.recurringId,
                            initialRecurringDueMillis = pendingLaunch.value.recurringDueMillis,
                            onRecurringHandled = {
                                pendingLaunch.value = pendingLaunch.value.withoutRecurring()
                            },
                            initialNavigateTo = pendingLaunch.value.navigationTarget,
                            onNavigateHandled = {
                                pendingLaunch.value = pendingLaunch.value.copy(navigationTarget = null)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        openingPromptSession.onForegrounded(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        super.onStop()
        // A rotation or language change stops the activity without the user leaving the app.
        if (!isChangingConfigurations) {
            openingPromptSession.onBackgrounded(SystemClock.elapsedRealtime())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLaunch.value = pendingLaunch.value.updatedWith(intent.toLaunchRequest())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingLaunch.value.recurringId?.let { outState.putLong(STATE_RECURRING_ID, it) }
        pendingLaunch.value.recurringDueMillis?.let { outState.putLong(STATE_RECURRING_DUE, it) }
        outState.putString(STATE_NAVIGATION_TARGET, pendingLaunch.value.navigationTarget)
    }

    private fun Intent.toLaunchRequest() = launchRequestFromExtras(
        longExtra = { key -> if (hasExtra(key)) getLongExtra(key, -1L) else null },
        navigationTarget = getStringExtra("navigate_to")
            ?: getStringExtra("nav_destination")
            ?: getStringExtra("action")
    )

    private fun Bundle.readLaunchRequest() = LaunchRequest(
        recurringId = if (containsKey(STATE_RECURRING_ID)) getLong(STATE_RECURRING_ID) else null,
        navigationTarget = getString(STATE_NAVIGATION_TARGET),
        recurringDueMillis = if (containsKey(STATE_RECURRING_DUE)) getLong(STATE_RECURRING_DUE) else null
    )

    private companion object {
        const val STATE_RECURRING_ID = "pending_launch_recurring_id"
        const val STATE_RECURRING_DUE = "pending_launch_recurring_due"
        const val STATE_NAVIGATION_TARGET = "pending_launch_navigation_target"
    }
}

@Composable
private fun NotificationPermissionRequester() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted state handled by system */ }

        LaunchedEffect(Unit) {
            val isGranted = com.mousa.mizani.core.permission.PermissionHelper.hasNotificationPermission(context)
            if (!isGranted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

