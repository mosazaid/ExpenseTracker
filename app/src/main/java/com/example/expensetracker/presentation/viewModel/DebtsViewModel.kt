package com.example.expensetracker.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.repository.IAlertRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val transactionRepository: ITransactionRepository,
    private val alertRepository: IAlertRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _shouldAutoShowRolloverDialog = MutableStateFlow(false)
    val shouldAutoShowRolloverDialog: StateFlow<Boolean> = _shouldAutoShowRolloverDialog.asStateFlow()

    init {
        checkMonthChangeRolloverAlert()
    }

    private fun checkMonthChangeRolloverAlert() {
        viewModelScope.launch {
            val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val lastMonth = userPreferences.getLastDebtRolloverMonth()
            if (lastMonth != null && lastMonth != currentMonthKey) {
                val openDebts = transactionRepository.getAllOutstandingDebtsSnapshot()
                if (openDebts.isNotEmpty()) {
                    alertRepository.postDebtRolloverAlert(
                        monthKey = currentMonthKey,
                        openDebtCount = openDebts.size
                    )
                    _shouldAutoShowRolloverDialog.value = true
                }
            }
            userPreferences.setLastDebtRolloverMonth(currentMonthKey)
        }
    }

    fun dismissAutoRolloverDialog() {
        _shouldAutoShowRolloverDialog.value = false
    }

    private val _lentDebtItems = MutableStateFlow<List<DebtItemUiModel>>(emptyList())
    val lentDebtItems: StateFlow<List<DebtItemUiModel>> = _lentDebtItems.asStateFlow()

    private val _borrowedDebtItems = MutableStateFlow<List<DebtItemUiModel>>(emptyList())
    val borrowedDebtItems: StateFlow<List<DebtItemUiModel>> = _borrowedDebtItems.asStateFlow()

    val lentDebts: StateFlow<List<Transaction>> = transactionRepository.getOutstandingLentDebtsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val borrowedDebts: StateFlow<List<Transaction>> = transactionRepository.getOutstandingBorrowedDebtsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeDebts()
    }

    private fun observeDebts() {
        viewModelScope.launch {
            transactionRepository.getAllOutstandingDebtsFlow().collect {
                loadDebtItemModels()
            }
        }
    }

    fun loadDebtItemModels() {
        viewModelScope.launch {
            val rawLent = transactionRepository.getAllOutstandingDebtsSnapshot().filter { it.awaitingReimbursement && !it.isDebtSettled }
            val rawBorrowed = transactionRepository.getAllOutstandingDebtsSnapshot().filter { it.debtType == "BORROWED" && !it.isDebtSettled }

            _lentDebtItems.value = rawLent.map { txn ->
                val paid = transactionRepository.getTotalReimbursedAmountForExpense(txn.id)
                val remaining = (txn.amount - paid).coerceAtLeast(0.0)
                DebtItemUiModel(
                    transaction = txn,
                    totalAmount = txn.amount,
                    paidAmount = paid,
                    remainingAmount = remaining,
                    isSettled = txn.isDebtSettled || remaining <= 0.001,
                    isOwedToMe = true,
                    debtorName = txn.debtorNote?.ifBlank { txn.description } ?: txn.description
                )
            }.filter { !it.isSettled }

            _borrowedDebtItems.value = rawBorrowed.map { txn ->
                val paid = transactionRepository.getTotalReimbursedAmountForExpense(txn.id)
                val remaining = (txn.amount - paid).coerceAtLeast(0.0)
                DebtItemUiModel(
                    transaction = txn,
                    totalAmount = txn.amount,
                    paidAmount = paid,
                    remainingAmount = remaining,
                    isSettled = txn.isDebtSettled || remaining <= 0.001,
                    isOwedToMe = false,
                    debtorName = txn.debtorNote?.ifBlank { txn.description } ?: txn.description
                )
            }.filter { !it.isSettled }
        }
    }

    fun recordDebtPayment(
        debtId: Long,
        paymentAmount: Double,
        accountType: com.example.expensetracker.data.database.entities.AccountType,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val debt = transactionRepository.getTransactionById(debtId) ?: return@launch
            val isOwedToMe = debt.awaitingReimbursement
            val currentPaid = transactionRepository.getTotalReimbursedAmountForExpense(debtId)
            val newTotalPaid = currentPaid + paymentAmount
            val isFullyPaid = newTotalPaid >= (debt.amount - 0.001)

            // Create repayment transaction
            val repaymentType = if (isOwedToMe) {
                com.example.expensetracker.data.database.entities.TransactionType.INCOME
            } else {
                com.example.expensetracker.data.database.entities.TransactionType.EXPENSE
            }

            val desc = if (isOwedToMe) {
                "Debt Repayment: ${debt.debtorNote?.ifBlank { debt.description } ?: debt.description}"
            } else {
                "Debt Payment: ${debt.debtorNote?.ifBlank { debt.description } ?: debt.description}"
            }

            val repaymentTxn = Transaction(
                amount = paymentAmount,
                description = desc,
                subDescription = if (isFullyPaid) "Fully Paid" else "Partial: ${newTotalPaid.toInt()}/${debt.amount.toInt()}",
                date = Date(),
                type = repaymentType,
                categoryId = debt.categoryId,
                accountType = accountType,
                linkedExpenseId = debt.id,
                debtorNote = debt.debtorNote
            )
            transactionRepository.insertTransaction(repaymentTxn)

            if (isFullyPaid) {
                transactionRepository.setDebtSettled(debtId, true)
                alertRepository.dismissAlertsByTypeAndRelatedId(
                    com.example.expensetracker.data.database.entities.AppAlert.TYPE_DEBT,
                    debtId
                )
            } else {
                // Update alert with remaining balance
                val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
                val remaining = (debt.amount - newTotalPaid).coerceAtLeast(0.0)
                alertRepository.postDebtAlert(
                    transactionId = debt.id,
                    personName = debt.debtorNote?.ifBlank { debt.description } ?: debt.description,
                    amount = remaining,
                    isOwedToMe = isOwedToMe,
                    periodKey = monthKey
                )
            }

            loadDebtItemModels()
            onComplete()
        }
    }

    fun forgiveDebt(debtId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            transactionRepository.setDebtSettled(debtId, true)
            alertRepository.dismissAlertsByTypeAndRelatedId(
                com.example.expensetracker.data.database.entities.AppAlert.TYPE_DEBT,
                debtId
            )
            loadDebtItemModels()
            onComplete()
        }
    }

    fun settleDebt(transactionId: Long) {
        forgiveDebt(transactionId)
    }

    fun carryOverDebtsToNewMonth(selectedDebtIds: List<Long>) {
        viewModelScope.launch {
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val allDebts = transactionRepository.getAllOutstandingDebtsSnapshot()
            val targets = allDebts.filter { it.id in selectedDebtIds }
            targets.forEach { txn ->
                val isOwedToMe = txn.awaitingReimbursement
                val personName = txn.debtorNote?.ifBlank { "Someone" } ?: "Someone"
                alertRepository.postDebtAlert(
                    transactionId = txn.id,
                    personName = personName,
                    amount = txn.amount,
                    isOwedToMe = isOwedToMe,
                    periodKey = monthKey
                )
            }
        }
    }
}

data class DebtItemUiModel(
    val transaction: Transaction,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val isSettled: Boolean,
    val isOwedToMe: Boolean,
    val debtorName: String
)
