package com.mousa.mizani.presentation.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.core.time.DateUtils
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.domain.calculator.anchorDayFor
import com.mousa.mizani.domain.calculator.skippingMissedBefore
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: IRecurringRepository,
    private val categoryRepository: ICategoryRepository,
    private val transactionRepository: ITransactionRepository
) : ViewModel() {

    fun getHistoryForRecurringFlow(recurring: RecurringTransaction): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsForRecurringFlow(
            recurringId = recurring.id,
            description = recurring.description,
            type = recurring.type
        )
    }

    val recurringList: StateFlow<List<RecurringTransaction>> = recurringRepository
        .getAllRecurringTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryMap: StateFlow<Map<Long, Category>> = categoryRepository
        .getAllCategories()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allCategories: StateFlow<List<Category>> = categoryRepository
        .getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleActive(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            val resumed = recurringRepository.getRecurringById(id)?.takeIf { !currentActive && it.autoAdd }
            if (resumed == null) {
                recurringRepository.setRecurringActive(id, !currentActive)
            } else {
                // Resuming must not back-fill the periods that passed while paused.
                recurringRepository.updateRecurring(
                    resumed.skippingMissedBefore(DateUtils.getStartOfDay(Date())).copy(isActive = true)
                )
            }
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringById(id)
        }
    }

    /** Turning auto-add on starts from today; it never back-fills older periods. */
    private fun RecurringTransaction.readyToSave(wasAutoAdd: Boolean): RecurringTransaction =
        if (autoAdd && !wasAutoAdd) skippingMissedBefore(DateUtils.getStartOfDay(Date())) else this

    fun saveRecurringTransaction(
        id: Long = 0,
        amount: Double,
        description: String,
        type: com.mousa.mizani.data.database.entities.TransactionType,
        categoryId: Long?,
        accountType: com.mousa.mizani.data.database.entities.AccountType,
        frequency: com.mousa.mizani.data.database.entities.RecurrenceFrequency,
        nextDueDate: java.util.Date,
        isActive: Boolean = true,
        autoAdd: Boolean = false
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                val created = RecurringTransaction(
                        amount = amount,
                        description = description,
                        type = type,
                        categoryId = categoryId,
                        accountType = accountType,
                        frequency = frequency,
                        nextDueDate = nextDueDate,
                        isActive = isActive,
                        autoAdd = autoAdd,
                        anchorDay = anchorDayFor(null, nextDueDate, frequency)
                    )
                recurringRepository.insertRecurringTransaction(created.readyToSave(wasAutoAdd = false))
            } else {
                val existing = recurringRepository.getRecurringById(id)
                if (existing != null) {
                    val updated = existing.copy(
                            amount = amount,
                            description = description,
                            type = type,
                            categoryId = categoryId,
                            accountType = accountType,
                            frequency = frequency,
                            nextDueDate = nextDueDate,
                            isActive = isActive,
                            autoAdd = autoAdd,
                            anchorDay = anchorDayFor(existing, nextDueDate, frequency)
                        )
                    recurringRepository.updateRecurring(updated.readyToSave(wasAutoAdd = existing.autoAdd))
                }
            }
        }
    }
}
