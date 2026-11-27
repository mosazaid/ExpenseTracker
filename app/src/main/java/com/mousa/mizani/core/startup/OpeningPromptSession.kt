package com.mousa.mizani.core.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A return after this long in the background counts as opening the app again. */
const val REOPEN_AFTER_MILLIS = 30 * 60 * 1000L

/**
 * What the user already answered since the app was opened. It lives as long as the process,
 * so it outlasts the screens that the app lock removes and rebuilds.
 * Times come from a clock that never jumps, such as SystemClock.elapsedRealtime().
 */
@Singleton
class OpeningPromptSession @Inject constructor() {
    private val _recurringDismissed = MutableStateFlow(false)
    val recurringDismissed: StateFlow<Boolean> = _recurringDismissed.asStateFlow()

    private var backgroundedAtMillis: Long? = null

    fun dismissRecurring() {
        _recurringDismissed.value = true
    }

    fun reopenRecurring() {
        _recurringDismissed.value = false
    }

    fun onBackgrounded(nowMillis: Long) {
        backgroundedAtMillis = nowMillis
    }

    fun onForegrounded(nowMillis: Long) {
        val since = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        if (nowMillis - since >= REOPEN_AFTER_MILLIS) reopenRecurring()
    }
}
