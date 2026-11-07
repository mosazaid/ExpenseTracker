package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.SalaryWalletPeriodSummary
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.presentation.model.MonthSummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val recurringRepository: IRecurringRepository,
    private val userPreferences: UserPreferences,
    private val periodCalculator: PeriodCalculator,
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator,
    private val pendingSmsDao: com.example.expensetracker.data.database.dao.PendingSmsTransactionDao
) : ViewModel() {

    private val _refreshTrigger = kotlinx.coroutines.flow.MutableStateFlow(0)

    fun refresh() {
        _refreshTrigger.value += 1
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    suspend fun getSplitsForTransaction(transactionId: Long): List<com.example.expensetracker.data.database.entities.TransactionSplit> {
        return transactionRepository.getSplitsForTransactionSnapshot(transactionId)
    }

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, MonthMode.CALENDAR)

    val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: Flow<List<Category>> = categoryRepository.getCategoriesSortedByUsage()

    /**
     * Derives a [MonthSummaryUiState] reactively from [monthMode] + [allTransactions] + [_refreshTrigger].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthSummary: StateFlow<MonthSummaryUiState> =
        combine(monthMode, allTransactions, _refreshTrigger) { mode, txns, trigger ->
            Triple(mode, txns, trigger)
        }
            .flatMapLatest { (mode, _, _) ->
                flow { emit(buildMonthSummary(mode, Date())) }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                MonthSummaryUiState()
            )

    /**
     * Computes the month-scoped summary (income, expense, per-account breakdown, reminders).
     * Always uses the salary month (or calendar month) bounds regardless of list period.
     * Kept internal/suspend for backward-compatible one-off calls (e.g., tests).
     */
    suspend fun buildMonthSummary(
        mode: MonthMode,
        referenceDate: Date
    ): MonthSummaryUiState {
        val bounds = periodCalculator.getBounds(HistoryPeriod.MONTH, referenceDate, mode)
        val financials = balanceCalculator.buildMonthFinancialSummary(bounds.start, bounds.end)
        val anchor = if (mode == MonthMode.SALARY) {
            periodCalculator.getCurrentPeriodAnchor(referenceDate)
        } else {
            null
        }
        val broughtForward = anchor?.carriedForwardBalance ?: 0.0
        val walletBalance = if (mode == MonthMode.SALARY) {
            walletCalculator.getWalletBalance(bounds.start, bounds.end)
        } else {
            0.0
        }
        val dueReminders = recurringRepository.getDueReminders()
        val cumulativeWalletBalance = walletCalculator.getCumulativeWalletBalance()
        val currentRemainingIncome = financials.periodNet - walletBalance
        val totalRemaining = financials.currentBalances.total
        val previousMonthRemaining = totalRemaining - currentRemainingIncome

        return MonthSummaryUiState(
            monthBounds = bounds,
            monthIncome = financials.periodIncome,
            monthExpense = financials.periodExpense,
            monthNet = financials.periodNet,
            broughtForward = broughtForward,
            previousMonthRemaining = previousMonthRemaining,
            monthRemaining = currentRemainingIncome,
            totalRemaining = totalRemaining,
            periodChangeByAccount = financials.periodChangeByAccount,
            incomeExpenseByAccount = financials.incomeExpenseByAccount,
            transferImpact = financials.transferImpact,
            currentBalances = financials.currentBalances,
            walletBalance = walletBalance,
            cumulativeWalletBalance = cumulativeWalletBalance,
            dueReminders = dueReminders,
            periodLoansDeducted = financials.periodLoansDeducted,
            totalPaidLoans = financials.totalPaidLoans,
            paidLoans = financials.paidLoans
        )
    }

    suspend fun buildSalaryWalletYearSummary(year: Int): List<SalaryWalletPeriodSummary> {
        return walletCalculator.getSalaryPeriodSummariesForYear(year)
    }

    suspend fun isRecurringBannerDismissed(recurring: RecurringTransaction): Boolean {
        return userPreferences.isRecurringDismissed(recurring.id, recurring.nextDueDate.time)
    }

    suspend fun dismissRecurringBanner(recurring: RecurringTransaction) {
        userPreferences.dismissRecurringBanner(recurring.id, recurring.nextDueDate.time)
    }

    val pendingSmsList: StateFlow<List<com.example.expensetracker.data.database.entities.PendingSmsTransaction>> =
        pendingSmsDao.getPendingSms()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun confirmAndSavePendingSms(item: com.example.expensetracker.data.database.entities.PendingSmsTransaction) {
        viewModelScope.launch {
            val allCats = categoryRepository.getAllCategoriesSnapshot()
            val matchedCat = allCats.firstOrNull { it.name.equals(item.suggestedCategory, ignoreCase = true) }
                ?: allCats.firstOrNull { it.type == com.example.expensetracker.data.database.entities.TransactionType.EXPENSE }

            val transaction = Transaction(
                amount = item.amount,
                description = item.merchant.ifBlank { "Card Purchase" },
                date = Date(item.timestamp),
                type = com.example.expensetracker.data.database.entities.TransactionType.EXPENSE,
                categoryId = matchedCat?.id,
                accountType = com.example.expensetracker.data.database.entities.AccountType.BANK,
                currency = item.currency
            )
            transactionRepository.insertTransaction(transaction)
            pendingSmsDao.markProcessed(item.id)
        }
    }

    fun dismissPendingSms(id: Long) {
        viewModelScope.launch {
            pendingSmsDao.markDismissed(id)
        }
    }

    fun addPastedSms(rawText: String) {
        viewModelScope.launch {
            val parsed = com.example.expensetracker.domain.sms.BankSmsParser.parse(rawText) ?: return@launch
            val pending = com.example.expensetracker.data.database.entities.PendingSmsTransaction(
                sender = parsed.bankName ?: "Bank SMS",
                rawBody = parsed.rawText,
                amount = parsed.amount,
                currency = parsed.currency,
                merchant = parsed.merchant,
                cardOrAccount = parsed.cardOrAccountEnding,
                suggestedCategory = parsed.suggestedCategoryName,
                timestamp = parsed.date.time
            )
            pendingSmsDao.insertPendingSms(pending)
        }
    }
}
