package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurrenceFrequency
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.BudgetProgressCalculator
import com.example.expensetracker.domain.CategorySystemKey
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.domain.isDept
import com.example.expensetracker.domain.matchesSystemKey
import com.example.expensetracker.domain.repository.IAlertRepository
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BudgetWarningInfo(
    val categoryName: String,
    val currentSpent: Double,
    val projectedSpent: Double,
    val limit: Double,
    val isExceeded: Boolean
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val recurringRepository: IRecurringRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator,
    private val budgetProgressCalculator: BudgetProgressCalculator,
    private val alertRepository: IAlertRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AddEditUiEvent>()
    val events = _events.asSharedFlow()

    private var originalTransaction: Transaction? = null

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val allCategories = categoryRepository.getCategoriesSortedByUsage()

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
            debtType = transaction.debtType,
            isIncomeBorrowedDebt = transaction.debtType == "BORROWED",
            isDebtSettled = transaction.isDebtSettled,
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

    suspend fun checkBudgetWarning(category: Category?, amountStr: String): BudgetWarningInfo? {
        if (category == null || uiState.value.transactionType != TransactionType.EXPENSE) return null
        val amount = amountStr.toDoubleOrNull() ?: return null
        val mode = monthMode.first()
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, uiState.value.selectedDate, mode)
        val progress = budgetProgressCalculator.getProgressForCategory(category.id, bounds.start, bounds.end)
            ?: return null

        val currentSpent = progress.spent
        val projected = currentSpent + amount
        if (projected >= progress.limit * 0.85) {
            return BudgetWarningInfo(
                categoryName = category.name,
                currentSpent = currentSpent,
                projectedSpent = projected,
                limit = progress.limit,
                isExceeded = projected >= progress.limit
            )
        }
        return null
    }

    suspend fun saveTransaction(transaction: Transaction): Long {
        val recurringId = _uiState.value.pendingRecurringId
        val txn = if (recurringId != null && transaction.recurringId == null) {
            transaction.copy(recurringId = recurringId)
        } else {
            transaction
        }
        val savedId = if (txn.id == 0L) {
            transactionRepository.insertTransaction(txn)
        } else {
            val preserved = originalTransaction
            transactionRepository.updateTransaction(
                txn.copy(
                    createdAt = preserved?.createdAt ?: txn.createdAt,
                    updatedAt = Date()
                )
            )
            txn.id
        }

        // Post-save checks for Budget notifications & alert records
        if (transaction.type == TransactionType.EXPENSE && transaction.categoryId != null) {
            val mode = monthMode.first()
            val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, transaction.date, mode)
            val progress = budgetProgressCalculator.getProgressForCategory(
                transaction.categoryId,
                bounds.start,
                bounds.end
            )
            if (progress != null) {
                val category = categoryRepository.getCategoryById(transaction.categoryId)
                val catName = category?.name ?: "Category"
                val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(transaction.date)

                if (progress.isOverBudget) {
                    notificationHelper.showBudgetNotification(
                        catName,
                        progress.spent,
                        progress.limit,
                        isExceeded = true
                    )
                    alertRepository.postBudgetAlert(
                        transaction.categoryId,
                        catName,
                        progress.spent,
                        progress.limit,
                        isExceeded = true,
                        periodKey = monthKey
                    )
                } else if (progress.isNearLimit) {
                    notificationHelper.showBudgetNotification(
                        catName,
                        progress.spent,
                        progress.limit,
                        isExceeded = false
                    )
                    alertRepository.postBudgetAlert(
                        transaction.categoryId,
                        catName,
                        progress.spent,
                        progress.limit,
                        isExceeded = false,
                        periodKey = monthKey
                    )
                }
            }

            // Dismiss daily expense alert if an expense was recorded today
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            alertRepository.dismissDailyAlertIfExpenseRecorded(todayKey)
        }

        return savedId
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
            TransactionType.INCOME
        ) ?: return -1L
        return recurringRepository.createSalaryReminder(
            amount = amount,
            accountType = accountType,
            salaryCategoryId = salaryCategory.id,
            dayOfMonth = dayOfMonth
        )
    }

    suspend fun createRecurringTemplate(
        amount: Double,
        description: String,
        type: TransactionType,
        categoryId: Long?,
        accountType: AccountType,
        frequency: RecurrenceFrequency,
        nextDueDate: java.util.Date
    ): Long {
        return recurringRepository.insertRecurringTransaction(
            RecurringTransaction(
                amount = amount,
                description = description,
                type = type,
                categoryId = categoryId,
                accountType = accountType,
                frequency = frequency,
                nextDueDate = nextDueDate,
                isActive = true
            )
        )
    }

    suspend fun getPreviousPeriodSavedAmount(beforeNewSalaryDate: Date): Double {
        val mode = monthMode.first()
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, beforeNewSalaryDate, mode)
        val financials = balanceCalculator.buildMonthFinancialSummary(bounds.start, bounds.end)
        val walletBalance = if (mode == MonthMode.SALARY) {
            walletCalculator.getWalletBalance(bounds.start, bounds.end)
        } else {
            0.0
        }
        return financials.periodNet - walletBalance
    }

    suspend fun isSalaryCategory(category: Category?): Boolean {
        return category.matchesSystemKey(CategorySystemKey.SALARY)
    }

    suspend fun isDeptCategory(category: Category?): Boolean {
        return category.isDept()
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
        val isDeptCat = category.isDept()
        val defaultAwaiting = if (isDeptCat && _uiState.value.transactionType == TransactionType.EXPENSE) true else _uiState.value.awaitingReimbursement
        val defaultDebtType = if (isDeptCat) {
            if (_uiState.value.transactionType == TransactionType.EXPENSE) "LENT" else "REPAYMENT"
        } else null

        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            awaitingReimbursement = defaultAwaiting,
            debtType = defaultDebtType
        )
    }

    fun updateTransactionType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(
            transactionType = type,
            selectedCategory = null,
            linkedExpenseId = null,
            awaitingReimbursement = false,
            debtType = null,
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
            debtType = if (awaiting) "LENT" else null,
            debtorNote = if (awaiting) _uiState.value.debtorNote else ""
        )
    }

    fun updateIncomeDebtType(isBorrowed: Boolean) {
        _uiState.value = _uiState.value.copy(
            isIncomeBorrowedDebt = isBorrowed,
            debtType = if (isBorrowed) "BORROWED" else "REPAYMENT",
            linkedExpenseId = if (isBorrowed) null else _uiState.value.linkedExpenseId
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
    val debtType: String? = null,
    val isIncomeBorrowedDebt: Boolean = false,
    val isDebtSettled: Boolean = false,
    val startsNewPeriod: Boolean = false,
    val carriedForwardBalance: Double? = null,
    val allowNegativeBalance: Boolean = false,
    val createdAt: Date? = null,
    val pendingRecurringId: Long? = null
)

sealed interface AddEditUiEvent {
    data object NavigateHistory : AddEditUiEvent
}
