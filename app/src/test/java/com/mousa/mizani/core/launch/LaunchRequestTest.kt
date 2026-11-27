package com.mousa.mizani.core.launch

import com.mousa.mizani.presentation.navigation.AddTransaction
import com.mousa.mizani.presentation.navigation.recurringFormRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRequestTest {

    private val fromIntent = LaunchRequest(recurringId = 7L, navigationTarget = "loans")
    private val nothing = LaunchRequest()

    @Test
    fun `a fresh start handles the launch extras`() {
        val result = launchRequestOnCreate(
            isRecreation = false, isFromHistory = false, unhandled = nothing, fromIntent = fromIntent
        )

        assertEquals(fromIntent, result)
    }

    @Test
    fun `a recreation does not replay handled extras`() {
        val result = launchRequestOnCreate(
            isRecreation = true, isFromHistory = false, unhandled = nothing, fromIntent = fromIntent
        )

        assertEquals(nothing, result)
    }

    @Test
    fun `a recreation keeps a request that was not handled yet`() {
        val unhandled = LaunchRequest(navigationTarget = "categories")

        val result = launchRequestOnCreate(
            isRecreation = true, isFromHistory = false, unhandled = unhandled, fromIntent = fromIntent
        )

        assertEquals(unhandled, result)
    }

    @Test
    fun `reopening from recent apps does not replay the old extras`() {
        val result = launchRequestOnCreate(
            isRecreation = false, isFromHistory = true, unhandled = nothing, fromIntent = fromIntent
        )

        assertEquals(nothing, result)
    }

    @Test
    fun `a new intent adds its request`() {
        assertEquals(fromIntent, nothing.updatedWith(fromIntent))
        assertEquals(
            LaunchRequest(recurringId = 7L, navigationTarget = "stats"),
            fromIntent.updatedWith(LaunchRequest(navigationTarget = "stats"))
        )
    }

    @Test
    fun `a recurring notification's extras reach the form's route with its due date`() {
        val extras = recurringLaunchExtras(recurringId = 7L, dueMillis = 123_000L)

        val request = launchRequestFromExtras(extras::get, navigationTarget = null)

        assertEquals(LaunchRequest(recurringId = 7L, recurringDueMillis = 123_000L), request)
        assertEquals(
            AddTransaction(recurringId = 7L, expectedDueMillis = 123_000L),
            recurringFormRoute(7L, request.recurringDueMillis)
        )
    }

    @Test
    fun `an older notification without a due date still opens the form`() {
        val extras = mapOf(EXTRA_RECURRING_ID to 7L)

        val request = launchRequestFromExtras(extras::get, navigationTarget = null)

        assertEquals(LaunchRequest(recurringId = 7L), request)
        assertEquals(AddTransaction(recurringId = 7L), recurringFormRoute(7L, request.recurringDueMillis))
    }

    @Test
    fun `a launch without extras asks for nothing`() {
        assertEquals(nothing, launchRequestFromExtras({ null }, navigationTarget = " "))
    }

    @Test
    fun `a newer item never keeps an older item's due date`() {
        val older = LaunchRequest(recurringId = 7L, recurringDueMillis = 1L)

        assertEquals(LaunchRequest(recurringId = 8L), older.updatedWith(LaunchRequest(recurringId = 8L)))
        assertEquals(older, older.updatedWith(nothing))
        assertEquals(nothing, older.withoutRecurring())
    }

    @Test
    fun `a new intent without extras keeps the unhandled request`() {
        assertEquals(fromIntent, fromIntent.updatedWith(nothing))
    }
}
