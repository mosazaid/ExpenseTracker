package com.mousa.mizani.presentation.feature.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.core.startup.OpeningPromptSession
import com.mousa.mizani.domain.usecase.DueRecurringAttentionUseCase
import com.mousa.mizani.data.database.entities.Category
import com.mousa.mizani.data.database.entities.Transaction
import com.mousa.mizani.data.preferences.CalculationPreferences
import com.mousa.mizani.data.preferences.MonthMode
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.constants.HistoryPeriod
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.calculator.SalaryWalletPeriodSummary
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import com.mousa.mizani.presentation.feature.overview.model.MonthSummaryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
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
    private val pendingSmsDao: com.mousa.mizani.data.database.dao.PendingSmsTransactionDao,
    private val dueRecurringAttention: DueRecurringAttentionUseCase,
    private val openingPromptSession: OpeningPromptSession
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

    suspend fun getSplitsForTransaction(transactionId: Long): List<com.mousa.mizani.data.database.entities.TransactionSplit> {
        return transactionRepository.getSplitsForTransactionSnapshot(transactionId)
    }

    val monthMode: StateFlow<MonthMode> = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, MonthMode.CALENDAR)

    val calculationPreferences: StateFlow<CalculationPreferences> = userPreferences.calculationPreferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalculationPreferences())

    val allTransactions = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: Flow<List<Category>> = categoryRepository.getCategoriesSortedByUsage()

    /**
     * Derives a [MonthSummaryUiState] reactively from [monthMode] + [allTransactions] + [calculationPreferences] + [_refreshTrigger].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthSummary: StateFlow<MonthSummaryUiState> =
        combine(monthMode, allTransactions, calculationPreferences, _refreshTrigger) { mode, _, _, _ ->
            mode
        }
            .flatMapLatest { mode ->
                flow { emit(buildMonthSummary(mode, Date())) }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                MonthSummaryUiState()
            )

    /** How many due recurring items need the user; also follows a Skip, which adds no transaction. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val dueAttentionCount: StateFlow<Int> = combine(
        recurringRepository.getAllRecurringTransactions(), allTransactions, _refreshTrigger
    ) { _, _, _ -> }
        .mapLatest { countDueAttention() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private suspend fun countDueAttention(): Int = try {
        dueRecurringAttention().size
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // The banner is hidden; the sheet reports a failed load to the user.
        Log.e("OverviewViewModel", "Counting due recurring items failed", e)
        0
    }

    fun reopenRecurring() = openingPromptSession.reopenRecurring()

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

    val pendingSmsList: StateFlow<List<com.mousa.mizani.data.database.entities.PendingSmsTransaction>> =
        pendingSmsDao.getPendingSms()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun confirmAndSavePendingSms(item: com.mousa.mizani.data.database.entities.PendingSmsTransaction) {
        viewModelScope.launch {
            val allCats = categoryRepository.getAllCategoriesSnapshot()
            val matchedCat = allCats.firstOrNull { it.name.equals(item.suggestedCategory, ignoreCase = true) }
                ?: allCats.firstOrNull { it.type == com.mousa.mizani.data.database.entities.TransactionType.EXPENSE }

            val transaction = Transaction(
                amount = item.amount,
                description = item.merchant.ifBlank { "Card Purchase" },
                date = Date(item.timestamp),
                type = com.mousa.mizani.data.database.entities.TransactionType.EXPENSE,
                categoryId = matchedCat?.id,
                accountType = com.mousa.mizani.data.database.entities.AccountType.BANK,
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
            val parsed = com.mousa.mizani.domain.sms.BankSmsParser.parse(rawText) ?: return@launch
            val pending = com.mousa.mizani.data.database.entities.PendingSmsTransaction(
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
