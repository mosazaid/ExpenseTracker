package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    val allTransactions = transactionRepository.getAllTransactions()
    val allCategories = categoryRepository.getAllCategories()

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsBetweenDates(startDate, endDate)
    }

    suspend fun getTransactionsById(id: Long): Transaction? {
        return transactionRepository.getTransactionById(id)
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsByType(type)
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionRepository.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionRepository.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction)
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSelectedCategory(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateTransactionType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(
            transactionType = type,
            selectedCategory = null // Reset category when type changes
        )
    }

    fun updateAccountType(type: AccountType) {
        _uiState.value = _uiState.value.copy(accountType = type)
    }

    fun updateSelectedDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun resetForm() {
        _uiState.value = TransactionUiState()
    }

    suspend fun getTotalIncomeForPeriod(startDate: Date, endDate: Date): Double {
        return transactionRepository.getTotalAmountByTypeAndDateRange(
            TransactionType.INCOME, startDate, endDate
        )
    }

    suspend fun getTotalExpenseForPeriod(startDate: Date, endDate: Date): Double {
        return transactionRepository.getTotalAmountByTypeAndDateRange(
            TransactionType.EXPENSE, startDate, endDate
        )
    }
}

data class TransactionUiState(
    val amount: String = "",
    val description: String = "",
    val selectedCategory: Category? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accountType: AccountType = AccountType.CASH,
    val selectedDate: Date = Date(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
