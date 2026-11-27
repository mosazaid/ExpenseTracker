package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.domain.calculator.planAutoAdd
import com.mousa.mizani.domain.repository.IRecurringRepository
import java.util.Date
import javax.inject.Inject

/** A due item that waits for the user. [isBlocked]: auto-add cannot record it. */
data class DueRecurringItem(val recurring: RecurringTransaction, val isBlocked: Boolean)

/**
 * The due items that need the user, oldest due first: every reminder-only item, and the
 * automatic ones whose account cannot cover even the oldest due period.
 * The phone notification switch does not apply to this list.
 */
class DueRecurringAttentionUseCase @Inject constructor(
    private val recurringRepository: IRecurringRepository,
    private val accountBalance: RecurringAccountBalance
) {
    suspend operator fun invoke(now: Date = Date()): List<DueRecurringItem> =
        recurringRepository.getDueReminders(now)
            .sortedBy { it.nextDueDate }
            .mapNotNull { attentionFor(it, now) }

    private suspend fun attentionFor(item: RecurringTransaction, now: Date): DueRecurringItem? {
        if (!item.autoAdd) return DueRecurringItem(item, isBlocked = false)
        val plan = planAutoAdd(item, now, accountBalance(item))
        val isBlocked = plan.toRecord.isEmpty() && plan.blocked != null
        return if (isBlocked) DueRecurringItem(item, isBlocked = true) else null
    }
}
