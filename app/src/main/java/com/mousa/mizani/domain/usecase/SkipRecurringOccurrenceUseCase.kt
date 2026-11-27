package com.mousa.mizani.domain.usecase

import com.mousa.mizani.domain.repository.IRecurringRepository
import javax.inject.Inject

/** Moves an item past one due period without adding a transaction. */
class SkipRecurringOccurrenceUseCase @Inject constructor(
    private val recurringRepository: IRecurringRepository,
    private val scheduleLock: RecurringScheduleLock
) {
    /**
     * Returns false, writing nothing, if the item is gone or paused, or its due date is no
     * longer [expectedDueMillis]: that period was already handled.
     */
    suspend operator fun invoke(id: Long, expectedDueMillis: Long): Boolean = scheduleLock.withLock {
        val item = recurringRepository.getRecurringById(id)
        if (item == null || !item.isActive || item.nextDueDate.time != expectedDueMillis) {
            false
        } else {
            recurringRepository.advanceRecurringDueDate(item)
            true
        }
    }
}
