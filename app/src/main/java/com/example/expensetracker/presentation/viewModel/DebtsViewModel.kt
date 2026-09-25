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

    val lentDebts: StateFlow<List<Transaction>> = transactionRepository.getOutstandingLentDebtsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val borrowedDebts: StateFlow<List<Transaction>> = transactionRepository.getOutstandingBorrowedDebtsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun settleDebt(transactionId: Long) {
        viewModelScope.launch {
            transactionRepository.setDebtSettled(transactionId, true)
            alertRepository.dismissAlertsByTypeAndRelatedId(
                com.example.expensetracker.data.database.entities.AppAlert.TYPE_DEBT,
                transactionId
            )
        }
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
