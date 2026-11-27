package com.mousa.mizani.presentation.common

import com.mousa.mizani.core.launch.LaunchRequest
import com.mousa.mizani.core.launch.launchRequestOnCreate
import org.junit.Assert.assertEquals
import org.junit.Test

class AppScreenTest {

    private val either = listOf(true, false)
    private val unknownOrEither = listOf(null, true, false)

    @Test
    fun `nothing is shown while the onboarding state is unknown`() {
        unknownOrEither.forEach { lock ->
            either.forEach { unlocked ->
                assertEquals(AppScreen.BLANK, appScreen(null, lock, unlocked))
            }
        }
    }

    @Test
    fun `nothing is shown while the lock state is unknown`() {
        either.forEach { unlocked ->
            assertEquals(AppScreen.BLANK, appScreen(true, null, unlocked))
        }
    }

    @Test
    fun `onboarding comes first and does not wait for the lock state`() {
        unknownOrEither.forEach { lock ->
            either.forEach { unlocked ->
                assertEquals(AppScreen.ONBOARDING, appScreen(false, lock, unlocked))
            }
        }
    }

    @Test
    fun `an enabled lock hides the content until unlocked`() {
        assertEquals(AppScreen.LOCK, appScreen(true, true, false))
        assertEquals(AppScreen.CONTENT, appScreen(true, true, true))
    }

    @Test
    fun `content is shown when the lock is off`() {
        either.forEach { unlocked ->
            assertEquals(AppScreen.CONTENT, appScreen(true, false, unlocked))
        }
    }

    /** Only the content handles a launch request, so nothing before it may drop the request. */
    @Test
    fun `a pending request survives until content is shown`() {
        val request = LaunchRequest(recurringId = 7L)
        var pending = request
        val handled = mutableListOf<LaunchRequest>()
        fun show(screen: AppScreen) {
            if (screen == AppScreen.CONTENT) {
                handled += pending
                pending = LaunchRequest()
            }
        }

        show(appScreen(null, null, false))
        show(appScreen(true, null, false))
        show(appScreen(true, true, false))
        pending = launchRequestOnCreate(
            isRecreation = true, isFromHistory = false, unhandled = pending, fromIntent = request
        )
        show(appScreen(true, true, false))
        assertEquals(request, pending)

        show(appScreen(true, true, true))

        assertEquals(listOf(request), handled)
    }
}
