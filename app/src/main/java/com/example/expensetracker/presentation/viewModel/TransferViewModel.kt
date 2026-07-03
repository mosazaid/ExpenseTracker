package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.BalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val balanceCalculator: BalanceCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<TransferUiEvent>()
    val events = _events.asSharedFlow()

    suspend fun loadTransferForEdit(id: Long): Boolean {
        val transaction = transactionRepository.getTransactionById(id) ?: return false
        if (transaction.type != TransactionType.TRANSFER) return false
        _uiState.value = TransferUiState(
            editingTransactionId = transaction.id,
            amount = transaction.amount.toString(),
            description = transaction.description,
            selectedDate = transaction.date,
            transferFromAccount = transaction.accountType,
            transferToAccount = transaction.toAccountType ?: AccountType.BANK,
            createdAt = transaction.createdAt
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

    suspend fun saveTransfer(allowNegative: Boolean): Long {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull() ?: return -1L
        val transaction = Transaction(
            id = state.editingTransactionId ?: 0L,
            amount = amount,
            description = state.description.ifBlank { "Transfer" },
            date = state.selectedDate,
            type = TransactionType.TRANSFER,
            categoryId = null,
            accountType = state.transferFromAccount,
            toAccountType = state.transferToAccount,
            allowNegativeBalance = allowNegative,
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

    fun updateTransferFromAccount(account: AccountType) {
        _uiState.value = _uiState.value.copy(
            transferFromAccount = account,
            transferToAccount = if (account == AccountType.CASH) AccountType.BANK else AccountType.CASH
        )
    }

    fun updateTransferToAccount(account: AccountType) {
        _uiState.value = _uiState.value.copy(transferToAccount = account)
    }

    fun resetForm() {
        _uiState.value = TransferUiState()
    }

    suspend fun emitNavigateBack() {
        _events.emit(TransferUiEvent.NavigateBack)
    }
}

data class TransferUiState(
    val editingTransactionId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val selectedDate: Date = Date(),
    val transferFromAccount: AccountType = AccountType.CASH,
    val transferToAccount: AccountType = AccountType.BANK,
    val createdAt: Date? = null
)

sealed interface TransferUiEvent {
    data object NavigateBack : TransferUiEvent
}
