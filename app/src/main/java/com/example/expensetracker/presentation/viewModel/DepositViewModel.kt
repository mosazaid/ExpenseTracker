package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.DepositCalculator
import com.example.expensetracker.domain.DepositSummary
import com.example.expensetracker.domain.WalletCalculator
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

enum class DepositFilter {
    ALL,
    PURCHASES,
    INFLOWS,
    TRANSFERS
}

data class DepositUiState(
    val isUnlocked: Boolean = false,
    val isBalanceHidden: Boolean = false,
    val summary: DepositSummary = DepositSummary(0.0, 0.0, 0.0, 0.0),
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val walletBalance: Double = 0.0,
    val selectedFilter: DepositFilter = DepositFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface DepositUiEvent {
    data class ShowToast(val message: String) : DepositUiEvent
    data object TransactionSaved : DepositUiEvent
}

@HiltViewModel
class DepositViewModel @Inject constructor(
    private val depositCalculator: DepositCalculator,
    private val walletCalculator: WalletCalculator,
    private val transactionRepository: ITransactionRepository,
    private val categoryRepository: ICategoryRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositUiState())
    val uiState: StateFlow<DepositUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DepositUiEvent>()
    val events = _events.asSharedFlow()

    val enableDepositWalletTransfer: StateFlow<Boolean> = userPreferences.enableDepositWalletTransfer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allCategories = categoryRepository.getAllCategories()

    fun unlock() {
        _uiState.value = _uiState.value.copy(isUnlocked = true)
        loadDepositData()
    }

    fun lock() {
        _uiState.value = _uiState.value.copy(isUnlocked = false)
    }

    fun toggleBalanceVisibility() {
        _uiState.value = _uiState.value.copy(isBalanceHidden = !_uiState.value.isBalanceHidden)
    }

    fun setFilter(filter: DepositFilter) {
        val filtered = filterTransactions(_uiState.value.transactions, filter)
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            filteredTransactions = filtered
        )
    }

    fun loadDepositData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val summary = depositCalculator.getDepositSummary()
                val txns = depositCalculator.getDepositTransactions()
                val walletBal = walletCalculator.getCumulativeWalletBalance()
                val filtered = filterTransactions(txns, _uiState.value.selectedFilter)

                _uiState.value = _uiState.value.copy(
                    summary = summary,
                    transactions = txns,
                    filteredTransactions = filtered,
                    walletBalance = walletBal,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage
                )
            }
        }
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        filter: DepositFilter
    ): List<Transaction> {
        return when (filter) {
            DepositFilter.ALL -> transactions
            DepositFilter.PURCHASES -> transactions.filter {
                it.accountType == AccountType.DEPOSIT && it.type == TransactionType.EXPENSE
            }
            DepositFilter.INFLOWS -> transactions.filter {
                it.type == TransactionType.INCOME && it.accountType == AccountType.DEPOSIT
            }
            DepositFilter.TRANSFERS -> transactions.filter {
                it.type == TransactionType.TRANSFER || it.type == TransactionType.WALLET_MOVE
            }
        }
    }

    fun depositFunds(
        amount: Double,
        sourceAccount: AccountType,
        description: String
    ) {
        viewModelScope.launch {
            if (amount <= 0) return@launch
            val txn = if (sourceAccount == AccountType.DEPOSIT) {
                // Direct capital injection into deposit
                Transaction(
                    amount = amount,
                    description = description.ifBlank { "Deposit Inflow" },
                    date = Date(),
                    type = TransactionType.INCOME,
                    categoryId = null,
                    accountType = AccountType.DEPOSIT,
                    excludeFromMonthExpenses = true,
                    createdAt = Date()
                )
            } else {
                // Transfer from liquid account to deposit
                Transaction(
                    amount = amount,
                    description = description.ifBlank { "Transfer to Vault" },
                    date = Date(),
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountType = sourceAccount,
                    toAccountType = AccountType.DEPOSIT,
                    excludeFromMonthExpenses = true,
                    createdAt = Date()
                )
            }
            transactionRepository.insertTransaction(txn)
            loadDepositData()
            _events.emit(DepositUiEvent.TransactionSaved)
        }
    }

    fun buyFromDeposit(
        amount: Double,
        description: String,
        categoryId: Long?
    ) {
        viewModelScope.launch {
            if (amount <= 0) return@launch
            val txn = Transaction(
                amount = amount,
                description = description.ifBlank { "Capital Purchase" },
                date = Date(),
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                accountType = AccountType.DEPOSIT,
                excludeFromMonthExpenses = true,
                createdAt = Date()
            )
            transactionRepository.insertTransaction(txn)
            loadDepositData()
            _events.emit(DepositUiEvent.TransactionSaved)
        }
    }

    fun transferToWallet(amount: Double, description: String) {
        viewModelScope.launch {
            if (amount <= 0) return@launch
            val txn = Transaction(
                amount = amount,
                description = description.ifBlank { "Vault to Wallet Transfer" },
                date = Date(),
                type = TransactionType.WALLET_MOVE,
                categoryId = null,
                accountType = AccountType.DEPOSIT,
                toAccountType = AccountType.WALLET,
                excludeFromMonthExpenses = true,
                createdAt = Date()
            )
            transactionRepository.insertTransaction(txn)
            loadDepositData()
            _events.emit(DepositUiEvent.TransactionSaved)
        }
    }

    fun transferFromWallet(amount: Double, description: String) {
        viewModelScope.launch {
            if (amount <= 0) return@launch
            val txn = Transaction(
                amount = amount,
                description = description.ifBlank { "Wallet to Vault Transfer" },
                date = Date(),
                type = TransactionType.WALLET_MOVE,
                categoryId = null,
                accountType = AccountType.WALLET,
                toAccountType = AccountType.DEPOSIT,
                excludeFromMonthExpenses = true,
                createdAt = Date()
            )
            transactionRepository.insertTransaction(txn)
            loadDepositData()
            _events.emit(DepositUiEvent.TransactionSaved)
        }
    }
}
