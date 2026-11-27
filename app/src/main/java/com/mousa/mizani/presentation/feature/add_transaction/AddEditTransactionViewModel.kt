package com.mousa.mizani.presentation.feature.add_transaction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.RecurrenceFrequency
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.SubCategory
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.BudgetProgressCalculator
import com.mousa.mizani.domain.constants.CategorySystemKey
import com.mousa.mizani.domain.constants.HistoryPeriod
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.domain.constants.isDept
import com.mousa.mizani.domain.constants.matchesSystemKey
import com.mousa.mizani.data.database.entities.Trip
import com.mousa.mizani.domain.currency.CurrencyRates
import com.mousa.mizani.domain.repository.IAlertRepository
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import com.mousa.mizani.domain.repository.ITripRepository
import com.mousa.mizani.domain.usecase.RecordRecurringOccurrenceUseCase
import com.mousa.mizani.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class BudgetWarningInfo(
    val categoryName: String,
    val currentSpent: Double,
    val projectedSpent: Double,
    val limit: Double,
    val isExceeded: Boolean,
    val transactionAmount: Double = 0.0
)

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val recurringRepository: IRecurringRepository,
    private val budgetAlert: com.mousa.mizani.domain.usecase.BudgetAlertUseCase,
    private val tripRepository: ITripRepository,
    private val itemPriceRepository: com.mousa.mizani.domain.repository.IItemPriceRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator,
    private val depositCalculator: com.mousa.mizani.domain.calculator.DepositCalculator,
    private val budgetProgressCalculator: BudgetProgressCalculator,
    private val alertRepository: IAlertRepository,
    private val notificationHelper: NotificationHelper,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val recordRecurringOccurrence: RecordRecurringOccurrenceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AddEditUiEvent>()
    val events = _events.asSharedFlow()

    private var originalTransaction: Transaction? = null

    private var prefilledRecurringId: Long? = null

    /** True from the first Save of a prefilled recurring form: it records one period only. */
    private var isRecurringFormSaved = false

    init {
        viewModelScope.launch {
            tripRepository.getAllTrips().collect { trips ->
                _uiState.value = _uiState.value.copy(availableTrips = trips)
            }
        }
        viewModelScope.launch {
            tripRepository.getActiveTrip().collect { activeTrip ->
                _uiState.value = _uiState.value.copy(activeTrip = activeTrip)
                // If creating a brand new transaction and an active trip is ongoing, pre-fill tripId and currency
                if (_uiState.value.editingTransactionId == null && _uiState.value.tripId == null && activeTrip != null) {
                    _uiState.value = _uiState.value.copy(
                        tripId = activeTrip.id,
                        currency = activeTrip.baseCurrency
                    )
                }
            }
        }
    }

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    val timeFormat: StateFlow<com.mousa.mizani.data.preferences.TimeFormatPreference> = userPreferences.timeFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.mousa.mizani.data.preferences.TimeFormatPreference.H12)

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
        val splits = transactionRepository.getSplitsForTransactionSnapshot(id)
        val splitItems = splits.map {
            TransactionSplitItem(
                id = it.id,
                subCategoryId = it.subCategoryId,
                subCategoryName = it.subCategoryName,
                amount = if (it.amount > 0) it.amount.toString() else "",
                note = it.note.orEmpty(),
                isDebt = it.isDebt,
                debtPersonName = it.debtPersonName.orEmpty(),
                isDebtSettled = it.isDebtSettled
            )
        }
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
            createdAt = transaction.createdAt,
            receiptImagePath = transaction.receiptImagePath,
            isSplitMode = splitItems.isNotEmpty(),
            splits = splitItems,
            excludeFromMonthExpenses = transaction.excludeFromMonthExpenses,
            currency = transaction.currency,
            originalAmount = transaction.originalAmount?.toString().orEmpty(),
            exchangeRateUsed = transaction.exchangeRateUsed,
            tripId = transaction.tripId,
            availableTrips = _uiState.value.availableTrips,
            activeTrip = _uiState.value.activeTrip
        )
        return true
    }

    /**
     * Fills the form from a recurring item, once. A rotation asks again, and reloading then
     * could offer the following period right after this one was saved.
     *
     * [expectedDueMillis] is the period the sender meant, as in an old notification or a form
     * restored after the process ended. If the item has moved past it, the form stays empty.
     * Returns whether the form was filled.
     */
    suspend fun prefillRecurring(recurringId: Long, expectedDueMillis: Long? = null): Boolean {
        if (prefilledRecurringId == recurringId) return _uiState.value.pendingRecurringId != null
        resetForm()
        return loadRecurringForPrefill(recurringId, expectedDueMillis)
    }

    private suspend fun loadRecurringForPrefill(recurringId: Long, expectedDueMillis: Long?): Boolean {
        val recurring = recurringRepository.getRecurringById(recurringId) ?: return false
        if (expectedDueMillis != null && recurring.nextDueDate.time != expectedDueMillis) {
            // Remembered, so a rotation neither repeats the message nor clears what was typed.
            prefilledRecurringId = recurringId
            _events.emit(AddEditUiEvent.RecurringAlreadyHandled)
            return false
        }
        val category = recurring.categoryId?.let { categoryRepository.getCategoryById(it) }
        _uiState.value = AddEditTransactionUiState(
            amount = recurring.amount.toString(),
            description = recurring.description,
            selectedCategory = category,
            transactionType = recurring.type,
            accountType = recurring.accountType,
            selectedDate = Date(),
            pendingRecurringId = recurring.id,
            pendingRecurringDueMillis = recurring.nextDueDate.time
        )
        prefilledRecurringId = recurring.id
        isRecurringFormSaved = false
        return true
    }

    fun setSplitMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSplitMode = enabled)
        if (enabled && _uiState.value.splits.isEmpty()) {
            val mainAmount = _uiState.value.amount
            _uiState.value = _uiState.value.copy(
                splits = listOf(
                    TransactionSplitItem(
                        amount = mainAmount,
                        note = _uiState.value.subDescription
                    )
                )
            )
        }
    }

    fun setReceiptImagePath(path: String?) {
        _uiState.value = _uiState.value.copy(receiptImagePath = path)
    }

    fun addSplitItem(split: TransactionSplitItem = TransactionSplitItem()) {
        val current = _uiState.value.splits
        _uiState.value = _uiState.value.copy(splits = current + split)
    }

    fun updateSplitItem(index: Int, split: TransactionSplitItem) {
        val current = _uiState.value.splits.toMutableList()
        if (index in current.indices) {
            current[index] = split
            _uiState.value = _uiState.value.copy(splits = current)
        }
    }

    fun removeSplitItem(index: Int) {
        val current = _uiState.value.splits.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uiState.value = _uiState.value.copy(splits = current)
        }
    }

    suspend fun getAvailableBalanceForAccount(account: AccountType, excludeId: Long?): Double {
        return when (account) {
            AccountType.CASH, AccountType.BANK -> {
                if (excludeId != null) {
                    balanceCalculator.getAvailableBalanceExcluding(account, excludeId)
                } else {
                    balanceCalculator.getAvailableBalance(account)
                }
            }
            AccountType.WALLET -> walletCalculator.getCumulativeWalletBalance(excludeId)
            AccountType.DEPOSIT -> depositCalculator.getDepositBalance(excludeId)
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
                isExceeded = projected >= progress.limit,
                transactionAmount = amount
            )
        }
        return null
    }

    /** From a recurring item's form this also advances the item, so no other call is needed. */
    suspend fun saveTransaction(transaction: Transaction): SaveTransactionResult {
        // Checked before anything suspends, so a second tap on Save sees it.
        if (isRecurringFormSaved) return SaveTransactionResult.Ignored
        val recurringId = _uiState.value.pendingRecurringId
        val recurringDueMillis = _uiState.value.pendingRecurringDueMillis
        val receiptPath = _uiState.value.receiptImagePath
        val isExcluded = _uiState.value.excludeFromMonthExpenses
                || transaction.accountType == AccountType.DEPOSIT
                || transaction.accountType == AccountType.WALLET
        val txn = transaction.copy(
            recurringId = recurringId ?: transaction.recurringId,
            receiptImagePath = receiptPath,
            excludeFromMonthExpenses = isExcluded,
            currency = _uiState.value.currency,
            originalAmount = _uiState.value.originalAmount.toDoubleOrNull() ?: transaction.originalAmount,
            exchangeRateUsed = _uiState.value.exchangeRateUsed ?: transaction.exchangeRateUsed,
            tripId = _uiState.value.tripId ?: transaction.tripId
        )

        val splitEntities = if (_uiState.value.isSplitMode) {
            _uiState.value.splits.mapNotNull { item ->
                val amt = item.amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    com.mousa.mizani.data.database.entities.TransactionSplit(
                        id = item.id,
                        transactionId = txn.id,
                        subCategoryId = item.subCategoryId,
                        subCategoryName = item.subCategoryName,
                        amount = amt,
                        note = item.note.takeIf { it.isNotBlank() },
                        isDebt = item.isDebt,
                        debtPersonName = item.debtPersonName.takeIf { it.isNotBlank() },
                        isDebtSettled = item.isDebtSettled
                    )
                } else null
            }
        } else emptyList()

        val savedId = if (recurringId != null && recurringDueMillis != null) {
            val result = saveRecurringPeriod(recurringId, recurringDueMillis) {
                writeTransaction(txn, splitEntities)
            }
            if (result !is SaveTransactionResult.Saved) return result
            result.id
        } else {
            writeTransaction(txn, splitEntities)
        }

        // Trigger home screen widget updates in real time
        com.mousa.mizani.glance.WidgetUpdateHelper.updateAllWidgets(context)

        // Auto-record item prices from split items into price memory
        if (_uiState.value.isSplitMode) {
            _uiState.value.splits.forEach { item ->
                val amt = item.amount.toDoubleOrNull() ?: 0.0
                val name = item.note.takeIf { it.isNotBlank() } ?: item.subCategoryName
                if (amt > 0 && !name.isNullOrBlank()) {
                    runCatching {
                        itemPriceRepository.recordItemPrice(
                            itemName = name,
                            unitPrice = amt,
                            currency = txn.currency,
                            subCategoryId = item.subCategoryId,
                            transactionId = savedId
                        )
                    }
                }
            }
        }

        // Optional micro-charity Sadaqah Round-Up (new expenses only, so edits don't add it again)
        if (transaction.type == TransactionType.EXPENSE && transaction.id == 0L) {
            val isRoundUpEnabled = userPreferences.sadaqahRoundUpEnabled.first()
            if (isRoundUpEnabled && txn.amount > 0) {
                val ceiling = kotlin.math.ceil(txn.amount)
                val spareChange = ceiling - txn.amount
                if (spareChange > 0.001) {
                    userPreferences.addToSadaqahJar(spareChange)
                }
            }
        }

        // Post-save checks for Budget notifications & alert records
        if (transaction.type == TransactionType.EXPENSE && transaction.categoryId != null) {
            budgetAlert(transaction)

            // Dismiss daily expense alert if an expense was recorded today
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            alertRepository.dismissDailyAlertIfExpenseRecorded(todayKey)
        }

        return SaveTransactionResult.Saved(savedId)
    }

    private suspend fun writeTransaction(
        txn: Transaction,
        splits: List<com.mousa.mizani.data.database.entities.TransactionSplit>
    ): Long {
        if (txn.id == 0L) {
            return if (splits.isNotEmpty()) {
                transactionRepository.insertTransactionWithSplits(txn, splits)
            } else {
                transactionRepository.insertTransaction(txn)
            }
        }
        val updatedTxn = txn.copy(
            createdAt = originalTransaction?.createdAt ?: txn.createdAt,
            updatedAt = Date()
        )
        if (splits.isNotEmpty()) {
            transactionRepository.updateTransactionWithSplits(updatedTxn, splits)
        } else {
            transactionRepository.updateTransaction(updatedTxn)
        }
        return txn.id
    }

    /** [write] runs in one database transaction with the item's advance. */
    private suspend fun saveRecurringPeriod(
        recurringId: Long,
        dueMillis: Long,
        write: suspend () -> Long
    ): SaveTransactionResult {
        isRecurringFormSaved = true
        val savedId = try {
            recordRecurringOccurrence(recurringId, dueMillis, write)
        } catch (e: CancellationException) {
            // If the save had already committed, the due date has moved and a retry is refused.
            isRecurringFormSaved = false
            throw e
        } catch (e: Exception) {
            // The database transaction was rolled back, so Save may be tried again.
            isRecurringFormSaved = false
            Log.e("AddEditTransaction", "Saving a recurring period failed", e)
            _events.emit(AddEditUiEvent.RecurringSaveFailed)
            return SaveTransactionResult.Failed
        }
        if (savedId == null) {
            _events.emit(AddEditUiEvent.RecurringAlreadyHandled)
            return SaveTransactionResult.AlreadyHandled
        }
        // A preference write, so it stays outside the database transaction.
        userPreferences.clearDismissedRecurring(recurringId)
        return SaveTransactionResult.Saved(savedId)
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
        nextDueDate: java.util.Date,
        anchorDay: Int? = null
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
                isActive = true,
                anchorDay = anchorDay
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
        // Wallet and Deposit expenses are always excluded from monthly operational expenses.
        // The manual toggle is only relevant for CASH/BANK transactions.
        val shouldExclude = when (type) {
            AccountType.WALLET, AccountType.DEPOSIT -> true
            else -> _uiState.value.excludeFromMonthExpenses
        }
        _uiState.value = _uiState.value.copy(
            accountType = type,
            excludeFromMonthExpenses = shouldExclude
        )
    }

    fun updateExcludeFromMonthExpenses(exclude: Boolean) {
        _uiState.value = _uiState.value.copy(excludeFromMonthExpenses = exclude)
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

    fun updateCurrency(newCurrency: String) {
        val oldCurrency = _uiState.value.currency
        val rate = CurrencyRates.getDefaultRate(newCurrency, "JOD")
        _uiState.value = _uiState.value.copy(
            currency = newCurrency,
            exchangeRateUsed = rate
        )
        // If there's an originalAmount in foreign currency, recalculate home amount
        val foreign = _uiState.value.originalAmount.toDoubleOrNull()
        if (foreign != null && newCurrency != "JOD") {
            val converted = CurrencyRates.convert(foreign, newCurrency, "JOD")
            _uiState.value = _uiState.value.copy(
                amount = String.format(Locale.US, "%.3f", converted)
            )
        }
    }

    fun updateOriginalAmount(amountStr: String) {
        _uiState.value = _uiState.value.copy(originalAmount = amountStr)
        val foreign = amountStr.toDoubleOrNull()
        val curr = _uiState.value.currency
        if (foreign != null && curr != "JOD") {
            val converted = CurrencyRates.convert(foreign, curr, "JOD", _uiState.value.exchangeRateUsed)
            _uiState.value = _uiState.value.copy(
                amount = String.format(Locale.US, "%.3f", converted)
            )
        } else if (curr == "JOD") {
            _uiState.value = _uiState.value.copy(amount = amountStr)
        }
    }

    fun updateTripId(tripId: Long?) {
        _uiState.value = _uiState.value.copy(tripId = tripId)
        val matchedTrip = _uiState.value.availableTrips.find { it.id == tripId }
        if (matchedTrip != null && _uiState.value.currency == "JOD") {
            updateCurrency(matchedTrip.baseCurrency)
        }
    }

    fun fillFromBankSms(smsText: String, categories: List<Category>): Boolean {
        val parsed = com.mousa.mizani.domain.sms.BankSmsParser.parse(smsText) ?: return false
        updateAmount(parsed.amount.toString())
        updateDescription(parsed.merchant)
        updateAccountType(AccountType.BANK)
        if (parsed.currency != "JOD") {
            updateCurrency(parsed.currency)
        }
        if (!parsed.suggestedCategoryName.isNullOrBlank()) {
            val matched = categories.firstOrNull { it.name.equals(parsed.suggestedCategoryName, ignoreCase = true) }
            if (matched != null) {
                updateSelectedCategory(matched)
            }
        }
        return true
    }

    fun resetForm() {
        val trips = _uiState.value.availableTrips
        val active = _uiState.value.activeTrip
        originalTransaction = null
        _uiState.value = AddEditTransactionUiState(
            availableTrips = trips,
            activeTrip = active,
            tripId = active?.id,
            currency = active?.baseCurrency ?: "JOD"
        )
    }

    suspend fun emitNavigateHistory() {
        _events.emit(AddEditUiEvent.NavigateHistory)
    }
}

data class TransactionSplitItem(
    val id: Long = 0,
    val subCategoryId: Long? = null,
    val subCategoryName: String? = null,
    val amount: String = "",
    val note: String = "",
    val isDebt: Boolean = false,
    val debtPersonName: String = "",
    val isDebtSettled: Boolean = false
)

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
    val pendingRecurringId: Long? = null,
    /** The due date the form was filled for; saving is refused once the item has moved past it. */
    val pendingRecurringDueMillis: Long? = null,
    val receiptImagePath: String? = null,
    val isSplitMode: Boolean = false,
    val splits: List<TransactionSplitItem> = emptyList(),
    val excludeFromMonthExpenses: Boolean = false,
    val currency: String = "JOD",
    val originalAmount: String = "",
    val exchangeRateUsed: Double? = null,
    val tripId: Long? = null,
    val availableTrips: List<Trip> = emptyList(),
    val activeTrip: Trip? = null
)

sealed interface AddEditUiEvent {
    data object NavigateHistory : AddEditUiEvent
    data object RecurringAlreadyHandled : AddEditUiEvent
    data object RecurringSaveFailed : AddEditUiEvent
}

sealed interface SaveTransactionResult {
    data class Saved(val id: Long) : SaveTransactionResult

    /** This form's recurring period is already being saved or was saved; do nothing. */
    data object Ignored : SaveTransactionResult

    /** The recurring period was recorded or skipped elsewhere, so nothing was written. */
    data object AlreadyHandled : SaveTransactionResult

    /** Saving a recurring period failed; nothing was written and Save works again. */
    data object Failed : SaveTransactionResult
}
