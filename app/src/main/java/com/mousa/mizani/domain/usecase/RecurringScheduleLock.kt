package com.mousa.mizani.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One lock for everything that moves a due date: skipping, saving the prefilled form and
 * auto-add. Each of them re-reads the item while holding it, so one due period is handled once.
 */
@Singleton
class RecurringScheduleLock @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}
