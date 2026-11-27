package com.mousa.mizani.domain.usecase

import android.util.Log
import com.mousa.mizani.data.database.dao.RecurringTransactionDao
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.domain.calculator.planAutoAdd
import com.mousa.mizani.domain.repository.IAlertRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Due items the user opted in to have recorded automatically. */
fun List<RecurringTransaction>.itemsToAutoAdd(): List<RecurringTransaction> = filter { it.autoAdd }

/** Due items that only get a reminder; the user records them. */
fun List<RecurringTransaction>.itemsToRemind(): List<RecurringTransaction> = filterNot { it.autoAdd }

data class AutoDeductExecutionResult(
    /** One entry per period recorded, each carrying its own due date. */
    val recorded: List<RecurringTransaction> = emptyList(),
    /** Expenses held back because the account balance could not cover them. */
    val blocked: List<RecurringTransaction> = emptyList()
)

/**
 * Records every due period of every auto-add item, dated on its due date, then runs the same
 * follow-ups as a manual save (budget warning, daily-reminder dismissal).
 */
class ExecuteAutoRecurringUseCase @Inject constructor(
    private val recurringDao: RecurringTransactionDao,
    private val executeRecurringUseCase: ExecuteRecurringTransactionUseCase,
    private val accountBalance: RecurringAccountBalance,
    private val transactionRepository: ITransactionRepository,
    private val budgetAlert: BudgetAlertUseCase,
    private val alertRepository: IAlertRepository,
    private val scheduleLock: RecurringScheduleLock
) {
    suspend operator fun invoke(executionDate: Date = Date()): AutoDeductExecutionResult {
        val recorded = mutableListOf<RecurringTransaction>()
        val blocked = mutableListOf<RecurringTransaction>()
        val saved = mutableListOf<Transaction>()

        val dueList = recurringDao.getDueRecurringTransactions(executionDate)
            .itemsToAutoAdd()
            // Income first on equal dates, oldest first overall, so arriving money can cover expenses.
            .sortedWith(compareBy({ it.nextDueDate }, { it.type != TransactionType.INCOME }))
        for (listed in dueList) {
            scheduleLock.withLock {
                // Read again: the user may have recorded, skipped or changed it since the list was read.
                val item = recurringDao.getRecurringTransactionById(listed.id)
                    ?.takeIf { it.isActive && it.autoAdd }
                    ?: return@withLock
                // Read per item: earlier items in this run have already changed the balance.
                val plan = planAutoAdd(item, executionDate, accountBalance(item))
                for (occurrence in plan.toRecord) {
                    // Cancelling between the insert and the advance would record the period twice.
                    val id = withContext(NonCancellable) {
                        executeRecurringUseCase(occurrence, executionDate = occurrence.nextDueDate)
                    }
                    transactionRepository.getTransactionById(id)?.let { saved += it }
                    recorded += occurrence
                }
                plan.blocked?.let { blocked += it }
            }
        }

        runFollowUps(saved, executionDate)
        return AutoDeductExecutionResult(recorded.toList(), blocked.toList())
    }

    /** A failed warning must not undo or repeat transactions that are already saved. */
    private suspend fun runFollowUps(saved: List<Transaction>, now: Date) {
        val expenses = saved.filter { it.type == TransactionType.EXPENSE }
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        try {
            // One budget check per category and month, however many periods were caught up.
            expenses.distinctBy { it.categoryId to monthFormat.format(it.date) }.forEach { budgetAlert(it) }
            val today = dayFormat.format(now)
            if (expenses.any { dayFormat.format(it.date) == today }) {
                alertRepository.dismissDailyAlertIfExpenseRecorded(today)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AutoRecurring", "Follow-up after auto-add failed", e)
        }
    }
}
