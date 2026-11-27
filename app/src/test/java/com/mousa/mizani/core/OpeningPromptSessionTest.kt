package com.mousa.mizani.core

import com.mousa.mizani.core.startup.OpeningPromptSession
import com.mousa.mizani.core.startup.REOPEN_AFTER_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningPromptSessionTest {

    private val session = OpeningPromptSession()

    private fun minutes(count: Int) = count * 60_000L

    @Test
    fun `a fresh start asks and later dismisses`() {
        assertFalse(session.recurringDismissed.value)

        session.dismissRecurring()
        assertTrue(session.recurringDismissed.value)

        session.reopenRecurring()
        assertFalse(session.recurringDismissed.value)
    }

    @Test
    fun `later stays dismissed after 29 minutes`() {
        session.dismissRecurring()

        session.onBackgrounded(1_000L)
        session.onForegrounded(1_000L + minutes(29))

        assertTrue(session.recurringDismissed.value)
    }

    @Test
    fun `returns after 30 minutes`() {
        assertEquals(minutes(30), REOPEN_AFTER_MILLIS)
        session.dismissRecurring()

        session.onBackgrounded(1_000L)
        session.onForegrounded(1_000L + minutes(30))

        assertFalse(session.recurringDismissed.value)
    }

    @Test
    fun `foreground without a background keeps state`() {
        session.dismissRecurring()

        session.onForegrounded(minutes(90))

        assertTrue(session.recurringDismissed.value)
    }

    @Test
    fun `one background period counts only once`() {
        session.onBackgrounded(0L)
        session.onForegrounded(minutes(1))
        session.dismissRecurring()

        // A recreation starts the activity again without a new background period.
        session.onForegrounded(minutes(90))

        assertTrue(session.recurringDismissed.value)
    }
}
