package com.mousa.mizani.presentation.feature.settings.accounts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mousa.mizani.R
import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.BalanceCorrection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

private const val TAG = "AccountBalances"
private val CORRECTABLE_ACCOUNTS = listOf(AccountType.CASH, AccountType.BANK)

@HiltViewModel
class AccountBalancesViewModel @Inject constructor(
    private val balanceCalculator: BalanceCalculator
) : ViewModel() {

    private val _balances = MutableStateFlow<Map<AccountType, Double>>(emptyMap())
    /** Empty until loaded, and again if loading fails, so an outdated amount is never shown. */
    val balances: StateFlow<Map<AccountType, Double>> = _balances.asStateFlow()

    private val _errors = MutableSharedFlow<Int>()
    /** String resources describing a failure to show to the user. */
    val errors: SharedFlow<Int> = _errors.asSharedFlow()

    // One read or write at a time: a correction is a read-modify-write of the opening balance,
    // and an older load must not replace a newer one.
    private val mutex = Mutex()

    init {
        refresh()
    }

    fun refresh() {
        runLocked { listOfNotNull(load()) }
    }

    /** Makes the account's balance exactly [change]'s new amount, whatever it is by now. */
    fun apply(account: AccountType, change: BalanceCorrection.Change) {
        if (account !in CORRECTABLE_ACCOUNTS) return
        runLocked { listOfNotNull(save(account, change.new), load()) }
    }

    /** Runs [block] under the mutex and reports its failures after releasing it. */
    private fun runLocked(block: suspend () -> List<Int>) {
        viewModelScope.launch {
            // emit waits for slow collectors, which must not hold up the next read or write.
            mutex.withLock { block() }.forEach { _errors.emit(it) }
        }
    }

    /** Returns the error to show, or null on success. */
    private suspend fun save(account: AccountType, target: Double): Int? {
        return try {
            // A non-finite stored balance would turn the new opening balance non-finite too.
            check(target.isFinite() && balanceCalculator.getAvailableBalance(account).isFinite()) {
                "Balance of $account is not a finite number"
            }
            balanceCalculator.setCurrentBalance(account, target)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not correct the $account balance", e)
            R.string.balance_correction_failed
        }
    }

    /** Returns the error to show, or null on success. */
    private suspend fun load(): Int? {
        return try {
            _balances.value = CORRECTABLE_ACCOUNTS.associateWith {
                balanceCalculator.getAvailableBalance(it)
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not load the balances", e)
            _balances.value = emptyMap()
            R.string.balance_load_failed
        }
    }
}
