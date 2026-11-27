package com.mousa.mizani.presentation.common

/** What the activity shows. Only [CONTENT] holds user data or handles a launch request. */
enum class AppScreen { BLANK, ONBOARDING, LOCK, CONTENT }

/** A null state has not been read yet, so nothing that depends on it may be shown. */
fun appScreen(
    onboardingCompleted: Boolean?,
    lockEnabled: Boolean?,
    isUnlocked: Boolean
): AppScreen = when {
    onboardingCompleted == null -> AppScreen.BLANK
    !onboardingCompleted -> AppScreen.ONBOARDING
    lockEnabled == null -> AppScreen.BLANK
    lockEnabled && !isUnlocked -> AppScreen.LOCK
    else -> AppScreen.CONTENT
}
