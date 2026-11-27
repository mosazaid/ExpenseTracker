package com.mousa.mizani.core.launch

const val EXTRA_RECURRING_ID = "recurringId"

/** The due date of the period a recurring notification was posted for. */
const val EXTRA_RECURRING_DUE_MILLIS = "recurringDueMillis"

/** What a notification or widget asked the app to open, held until the UI has handled it. */
data class LaunchRequest(
    val recurringId: Long? = null,
    val navigationTarget: String? = null,
    /** Belongs to [recurringId]. Null when the sender named no period. */
    val recurringDueMillis: Long? = null
) {
    /** Adds a newer request; anything it does not ask for stays pending. */
    fun updatedWith(newer: LaunchRequest): LaunchRequest {
        // An item and its due date are replaced together, never mixed.
        val recurringSource = if (newer.recurringId != null) newer else this
        return LaunchRequest(
            recurringId = recurringSource.recurringId,
            navigationTarget = newer.navigationTarget ?: navigationTarget,
            recurringDueMillis = recurringSource.recurringDueMillis
        )
    }

    /** What stays pending once the recurring item's form has been opened. */
    fun withoutRecurring(): LaunchRequest = copy(recurringId = null, recurringDueMillis = null)
}

/** The extras a notification about one due period carries. */
fun recurringLaunchExtras(recurringId: Long, dueMillis: Long): Map<String, Long> =
    mapOf(EXTRA_RECURRING_ID to recurringId, EXTRA_RECURRING_DUE_MILLIS to dueMillis)

/** Reads a request from intent extras; [longExtra] returns null for an absent one. */
fun launchRequestFromExtras(longExtra: (String) -> Long?, navigationTarget: String?): LaunchRequest {
    val recurringId = longExtra(EXTRA_RECURRING_ID)?.takeIf { it != -1L }
    return LaunchRequest(
        recurringId = recurringId,
        navigationTarget = navigationTarget?.takeIf { it.isNotBlank() },
        recurringDueMillis = recurringId?.let { longExtra(EXTRA_RECURRING_DUE_MILLIS) }
    )
}

/**
 * The request to handle when the activity is created. The intent outlives both a recreation and
 * a reopen from recent apps, so its extras count only on a fresh start. A recreation carries
 * over what was still [unhandled], for example while the app was locked.
 */
fun launchRequestOnCreate(
    isRecreation: Boolean,
    isFromHistory: Boolean,
    unhandled: LaunchRequest,
    fromIntent: LaunchRequest
): LaunchRequest = when {
    isRecreation -> unhandled
    isFromHistory -> LaunchRequest()
    else -> fromIntent
}
