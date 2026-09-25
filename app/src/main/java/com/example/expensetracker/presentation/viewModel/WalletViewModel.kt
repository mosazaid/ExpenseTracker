package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.MonthMode
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.BalanceCalculator
import com.example.expensetracker.domain.HistoryPeriod
import com.example.expensetracker.domain.PeriodCalculator
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import java.util.Date
import javax.inject.Inject

enum class WalletDirection {
    TO_WALLET,
    FROM_WALLET
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator,
    private val periodCalculator: PeriodCalculator,
    userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<WalletUiEvent>()
    val events = _events.asSharedFlow()

    val monthMode = userPreferences.monthMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthMode.CALENDAR)

    suspend fun loadWalletMoveForEdit(id: Long): Boolean {
        val transaction = transactionRepository.getTransactionById(id) ?: return false
        if (transaction.type != TransactionType.WALLET_MOVE) return false
        val direction = if (transaction.accountType == AccountType.WALLET) {
            WalletDirection.FROM_WALLET
        } else {
            WalletDirection.TO_WALLET
        }
        val liquidAccount = if (direction == WalletDirection.TO_WALLET) {
            transaction.accountType
        } else {
            transaction.toAccountType ?: AccountType.CASH
        }
        _uiState.value = WalletUiState(
            editingTransactionId = transaction.id,
            amount = transaction.amount.toString(),
            description = transaction.description,
            selectedDate = transaction.date,
            direction = direction,
            liquidAccount = liquidAccount,
            createdAt = transaction.createdAt
        )
        return true
    }

    suspend fun isSalaryMode(): Boolean = monthMode.first() == MonthMode.SALARY

    suspend fun isSelectedDateInOpenPeriod(): Boolean {
        return walletCalculator.isDateInOpenSalaryPeriod(_uiState.value.selectedDate)
    }

    suspend fun getWalletBalanceForSelectedPeriod(): Double {
        val bounds = periodCalculator.getBounds(
            HistoryPeriod.MONTH,
            _uiState.value.selectedDate,
            MonthMode.SALARY
        )
        if (bounds.isSalaryFallback) return 0.0
        val excludeId = _uiState.value.editingTransactionId
        return walletCalculator.getWalletBalance(bounds.start, bounds.end, excludeId)
    }

    suspend fun getAvailableLiquidBalance(account: AccountType): Double {
        val editingId = _uiState.value.editingTransactionId
        return if (editingId != null) {
            balanceCalculator.getAvailableBalanceExcluding(account, editingId)
        } else {
            balanceCalculator.getAvailableBalance(account)
        }
    }

    suspend fun canSave(): Boolean {
        if (!isSalaryMode()) return false
        if (!isSelectedDateInOpenPeriod()) return false
        val amount = _uiState.value.amount.toDoubleOrNull() ?: return false
        if (amount <= 0) return false
        return true
    }

    suspend fun validateWalletMove(): WalletMoveValidation {
        if (!isSalaryMode()) {
            return WalletMoveValidation.Invalid("Wallet is only available in salary month mode.")
        }
        if (!isSelectedDateInOpenPeriod()) {
            return WalletMoveValidation.Invalid("This salary month is closed.")
        }
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
            ?: return WalletMoveValidation.Invalid("Enter a valid amount.")
        if (amount <= 0) {
            return WalletMoveValidation.Invalid("Amount must be greater than zero.")
        }
        return when (state.direction) {
            WalletDirection.TO_WALLET -> {
                val available = getAvailableLiquidBalance(state.liquidAccount)
                if (amount > available) {
                    WalletMoveValidation.Invalid(
                        "Not enough in ${state.liquidAccount.name.lowercase()}. " +
                            "Available: ${formatAmount(available)}."
                    )
                } else {
                    WalletMoveValidation.Valid
                }
            }
            WalletDirection.FROM_WALLET -> {
                val walletAvailable = getWalletBalanceForSelectedPeriod()
                if (amount > walletAvailable) {
                    WalletMoveValidation.Invalid(
                        "Not enough in wallet. Available: ${formatAmount(walletAvailable)}."
                    )
                } else {
                    WalletMoveValidation.Valid
                }
            }
        }
    }

    suspend fun getMaxAllowedAmount(): Double {
        val state = _uiState.value
        return when (state.direction) {
            WalletDirection.TO_WALLET -> getAvailableLiquidBalance(state.liquidAccount).coerceAtLeast(0.0)
            WalletDirection.FROM_WALLET -> getWalletBalanceForSelectedPeriod().coerceAtLeast(0.0)
        }
    }

    suspend fun saveWalletMove(): Long {
        if (validateWalletMove() !is WalletMoveValidation.Valid) return -1L

        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return -1L
        val (from, to) = when (state.direction) {
            WalletDirection.TO_WALLET -> state.liquidAccount to AccountType.WALLET
            WalletDirection.FROM_WALLET -> AccountType.WALLET to state.liquidAccount
        }
        if (!walletCalculator.validateWalletMove(from, to)) return -1L

        val transaction = Transaction(
            id = state.editingTransactionId ?: 0L,
            amount = amount,
            description = state.description.ifBlank {
                if (state.direction == WalletDirection.TO_WALLET) "To wallet" else "From wallet"
            },
            date = state.selectedDate,
            type = TransactionType.WALLET_MOVE,
            categoryId = null,
            accountType = from,
            toAccountType = to,
            allowNegativeBalance = false,
            createdAt = state.createdAt ?: Date()
        )
        return if (transaction.id == 0L) {
            transactionRepository.insertTransaction(transaction)
        } else {
            transactionRepository.updateTransaction(transaction)
            transaction.id
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSelectedDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun updateDirection(direction: WalletDirection) {
        _uiState.value = _uiState.value.copy(direction = direction)
    }

    fun updateLiquidAccount(account: AccountType) {
        _uiState.value = _uiState.value.copy(liquidAccount = account)
    }

    fun resetForm() {
        _uiState.value = WalletUiState()
    }

    suspend fun emitNavigateBack() {
        _events.emit(WalletUiEvent.NavigateBack)
    }

    private fun formatAmount(amount: Double): String {
        return String.format(java.util.Locale.US, "%.3f", amount)
    }
}

sealed interface WalletMoveValidation {
    data object Valid : WalletMoveValidation
    data class Invalid(val message: String) : WalletMoveValidation
}

data class WalletUiState(
    val editingTransactionId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val selectedDate: Date = Date(),
    val direction: WalletDirection = WalletDirection.TO_WALLET,
    val liquidAccount: AccountType = AccountType.CASH,
    val createdAt: Date? = null
)

sealed interface WalletUiEvent {
    data object NavigateBack : WalletUiEvent
}

val LIQUID_ACCOUNTS = listOf(AccountType.CASH, AccountType.BANK)
