package com.mousa.mizani.domain.usecase

import com.mousa.mizani.data.database.entities.AccountType
import com.mousa.mizani.data.database.entities.RecurringTransaction
import com.mousa.mizani.data.database.entities.TransactionType
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.DepositCalculator
import com.mousa.mizani.domain.calculator.WalletCalculator
import javax.inject.Inject

/** The money available to a recurring expense in its account; 0 for anything but an expense. */
class RecurringAccountBalance @Inject constructor(
    private val balanceCalculator: BalanceCalculator,
    private val walletCalculator: WalletCalculator,
    private val depositCalculator: DepositCalculator
) {
    suspend operator fun invoke(item: RecurringTransaction): Double {
        if (item.type != TransactionType.EXPENSE) return 0.0
        return when (item.accountType) {
            AccountType.CASH, AccountType.BANK -> balanceCalculator.getAvailableBalance(item.accountType)
            AccountType.WALLET -> walletCalculator.getCumulativeWalletBalance(null)
            AccountType.DEPOSIT -> depositCalculator.getDepositBalance(null)
        }
    }
}
