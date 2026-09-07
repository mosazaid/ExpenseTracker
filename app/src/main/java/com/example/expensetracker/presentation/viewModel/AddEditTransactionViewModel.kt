package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.RecurringRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.CategorySystemKey
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.matchesSystemKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

import com.example.expensetracker.data.database.entities.SubCategory
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val balanceCalculator: BalanceCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AddEditUiEvent>()
    val events = _events.asSharedFlow()

    private var originalTransaction: Transaction? = null

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val allCategories = categoryRepository.getAllCategories()

    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        return categoryRepository.getSubCategories(categoryId)
    }

    suspend fun saveSubCategory(categoryId: Long, name: String): Long {
        return categoryRepository.saveSubCategory(categoryId, name)
    }

    suspend fun loadTransactionForEdit(id: Long): Boolean {
        val transaction = transactionRepository.getTransactionById(id) ?: return false
        if (transaction.type == TransactionType.TRANSFER) return false
        originalTransaction = transaction
        val category = transaction.categoryId?.let { categoryRepository.getCategoryById(it) }
        _uiState.value = AddEditTransactionUiState(
            editingTransactionId = id,
            amount = transaction.amount.toString(),
            description = transaction.description,
            subDescription = transaction.subDescription.orEmpty(),
            selectedCategory = category,
            transactionType = transaction.type,
            accountType = transaction.accountType,
            selectedDate = transaction.date,
            debtorNote = transaction.debtorNote.orEmpty(),
            linkedExpenseId = transaction.linkedExpenseId,
            awaitingReimbursement = transaction.awaitingReimbursement,
            startsNewPeriod = transaction.startsNewPeriod,
            carriedForwardBalance = transaction.carriedForwardBalance,
            allowNegativeBalance = transaction.allowNegativeBalance,
            createdAt = transaction.createdAt
        )
        return true
    }

    suspend fun loadRecurringForPrefill(recurringId: Long): Boolean {
        val recurring = recurringRepository.getRecurringById(recurringId) ?: return false
        val category = recurring.categoryId?.let { categoryRepository.getCategoryById(it) }
        _uiState.value = AddEditTransactionUiState(
            amount = recurring.amount.toString(),
            description = recurring.description,
            selectedCategory = category,
            transactionType = recurring.type,
            accountType = recurring.accountType,
            selectedDate = Date(),
            pendingRecurringId = recurring.id
        )
        return true
    }

    suspend fun getAvailableBalanceForEdit(account: AccountType): Double {
        val editingId = _uiState.value.editingTransactionId
        return if (editingId != null) {
            balanceCalculator.getAvailableBalanceExcluding(account, editingId)
        } else {
            balanceCalculator.getAvailableBalance(account)
        }
    }

    suspend fun getAvailableBalanceForAccount(account: AccountType, excludeId: Long?): Double {
        return if (excludeId != null) {
            balanceCalculator.getAvailableBalanceExcluding(account, excludeId)
        } else {
            balanceCalculator.getAvailableBalance(account)
        }
    }

    suspend fun getUnreimbursedExpensesForCurrentMonth(): List<Transaction> {
        val mode = monthMode.first()
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, Date(), mode)
        return transactionRepository.getUnreimbursedExpenses(bounds.start, bounds.end)
    }

    suspend fun saveTransaction(transaction: Transaction): Long {
        return if (transaction.id == 0L) {
            transactionRepository.insertTransaction(transaction)
        } else {
            val preserved = originalTransaction
            transactionRepository.updateTransaction(
                transaction.copy(
                    createdAt = preserved?.createdAt ?: transaction.createdAt,
                    updatedAt = Date()
                )
            )
            transaction.id
        }
    }

    suspend fun advanceRecurringAfterSave(recurringId: Long) {
        recurringRepository.getRecurringById(recurringId)?.let {
            recurringRepository.advanceRecurringDueDate(it)
            userPreferences.clearDismissedRecurring(it.id)
        }
    }

    suspend fun createSalaryReminder(
        amount: Double,
        accountType: AccountType,
        dayOfMonth: Int
    ): Long {
        val salaryCategory = categoryRepository.getCategoryByName(
            CategorySystemKey.SALARY.displayName,
            CategorySystemKey.SALARY.type
        )
            ?: return -1L
        return recurringRepository.createSalaryReminder(
            amount = amount,
            accountType = accountType,
            salaryCategoryId = salaryCategory.id,
            dayOfMonth = dayOfMonth
        )
    }

    suspend fun getPreviousPeriodSavedAmount(beforeNewSalaryDate: Date): Double {
        val bounds = periodCalculator.getPreviousSalaryPeriodBounds(beforeNewSalaryDate)
            ?: return 0.0
        return balanceCalculator.buildMonthFinancialSummary(bounds.start, bounds.end).periodNet
    }

    suspend fun isSalaryCategory(category: Category?): Boolean {
        return category.matchesSystemKey(CategorySystemKey.SALARY)
    }

    suspend fun isDeptCategory(category: Category?): Boolean {
        return category.matchesSystemKey(CategorySystemKey.DEPT)
    }

    fun shouldPromptSalaryMonth(category: Category?, isEdit: Boolean): Boolean {
        if (!isEdit) return true
        val original = originalTransaction ?: return true
        val state = _uiState.value
        return original.startsNewPeriod != state.startsNewPeriod ||
            original.carriedForwardBalance != state.carriedForwardBalance ||
            original.date != state.selectedDate ||
            original.categoryId != category?.id ||
            original.accountType != state.accountType
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSubDescription(subDescription: String) {
        _uiState.value = _uiState.value.copy(subDescription = subDescription)
    }

    fun updateSelectedCategory(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateTransactionType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(
            transactionType = type,
            selectedCategory = null,
            linkedExpenseId = null,
            awaitingReimbursement = false,
            debtorNote = ""
        )
    }

    fun updateAccountType(type: AccountType) {
        _uiState.value = _uiState.value.copy(accountType = type)
    }

    fun updateSelectedDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun updateDebtorNote(note: String) {
        _uiState.value = _uiState.value.copy(debtorNote = note)
    }

    fun updateAwaitingReimbursement(awaiting: Boolean) {
        _uiState.value = _uiState.value.copy(
            awaitingReimbursement = awaiting,
            debtorNote = if (awaiting) _uiState.value.debtorNote else ""
        )
    }

    fun updateLinkedExpenseId(expenseId: Long?) {
        _uiState.value = _uiState.value.copy(linkedExpenseId = expenseId)
    }

    fun resetForm() {
        originalTransaction = null
        _uiState.value = AddEditTransactionUiState()
    }

    suspend fun emitNavigateHistory() {
        _events.emit(AddEditUiEvent.NavigateHistory)
    }
}

data class AddEditTransactionUiState(
    val editingTransactionId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val subDescription: String = "",
    val selectedCategory: Category? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val accountType: AccountType = AccountType.BANK,
    val selectedDate: Date = Date(),
    val debtorNote: String = "",
    val linkedExpenseId: Long? = null,
    val awaitingReimbursement: Boolean = false,
    val startsNewPeriod: Boolean = false,
    val carriedForwardBalance: Double? = null,
    val allowNegativeBalance: Boolean = false,
    val createdAt: Date? = null,
    val pendingRecurringId: Long? = null
)

sealed interface AddEditUiEvent {
    data object NavigateHistory : AddEditUiEvent
}
