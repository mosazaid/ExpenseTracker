package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
            recurringRepository.setRecurringActive(id, !currentActive)
        }
    }

    fun deleteRecurring(id: Long) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringById(id)
        }
    }

    fun saveRecurringTransaction(
        id: Long = 0,
        amount: Double,
        description: String,
        type: com.example.expensetracker.data.database.entities.TransactionType,
        categoryId: Long?,
        accountType: com.example.expensetracker.data.database.entities.AccountType,
        frequency: com.example.expensetracker.data.database.entities.RecurrenceFrequency,
        nextDueDate: java.util.Date,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                recurringRepository.insertRecurringTransaction(
                    RecurringTransaction(
                        amount = amount,
                        description = description,
                        type = type,
                        categoryId = categoryId,
                        accountType = accountType,
                        frequency = frequency,
                        nextDueDate = nextDueDate,
                        isActive = isActive
                    )
                )
            } else {
                val existing = recurringRepository.getRecurringById(id)
                if (existing != null) {
                    recurringRepository.updateRecurring(
                        existing.copy(
                            amount = amount,
                            description = description,
                            type = type,
                            categoryId = categoryId,
                            accountType = accountType,
                            frequency = frequency,
                            nextDueDate = nextDueDate,
                            isActive = isActive
                        )
                    )
                }
            }
        }
    }
}
