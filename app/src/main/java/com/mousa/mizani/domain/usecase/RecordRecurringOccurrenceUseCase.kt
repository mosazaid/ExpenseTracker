package com.mousa.mizani.domain.usecase

import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.TransactionRunner
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Saves the transaction the user confirmed for one due period, then advances the item. */
class RecordRecurringOccurrenceUseCase @Inject constructor(
    private val recurringRepository: IRecurringRepository,
    private val scheduleLock: RecurringScheduleLock,
    private val transactionRunner: TransactionRunner
) {
    /**
     * Runs [record] and returns its id. Returns null without running it when the item's due date
     * is no longer [expectedDueMillis]: that period was already recorded or skipped.
     * An item deleted meanwhile has no schedule left to protect, so the transaction is saved.
     * [record] and the advance share one database transaction, so [record] may only write to
     * the database. If either fails, nothing is saved.
     */
    suspend operator fun invoke(id: Long, expectedDueMillis: Long, record: suspend () -> Long): Long? =
        scheduleLock.withLock {
            val item = recurringRepository.getRecurringById(id)
            if (item != null && item.nextDueDate.time != expectedDueMillis) {
                null
            } else {
                // Leaving the screen must not abandon a save the user confirmed.
                withContext(NonCancellable) {
                    transactionRunner.run {
                        val savedId = record()
                        item?.let { recurringRepository.advanceRecurringDueDate(it) }
                        savedId
                    }
                }
            }
        }
}
