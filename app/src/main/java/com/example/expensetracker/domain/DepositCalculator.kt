package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.repository.ITransactionRepository
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class DepositSummary(
    val currentBalance: Double,
    val totalDeposited: Double,
    val totalCapitalPurchases: Double,
    val totalTransferredOut: Double,
    val yearCapitalPurchases: Double = 0.0
)

/**
 * Domain calculator for the Great Wallet (Deposit Vault).
 * Manages capital reserves and asset purchases held outside liquid accounts (Cash, Bank, Wallet).
 */
@Singleton
class DepositCalculator @Inject constructor(
    private val transactionRepository: ITransactionRepository
) {

    suspend fun getDepositBalance(excludeTransactionId: Long? = null): Double {
        val income = if (excludeTransactionId != null) {
            transactionRepository.getTotalIncomeForAccountExcluding(AccountType.DEPOSIT, excludeTransactionId)
        } else {
            transactionRepository.getTotalIncomeForAccount(AccountType.DEPOSIT)
        }
        val expense = if (excludeTransactionId != null) {
            transactionRepository.getTotalExpenseForAccountExcluding(AccountType.DEPOSIT, excludeTransactionId)
        } else {
            transactionRepository.getTotalExpenseForAccount(AccountType.DEPOSIT)
        }
        val transferIn = if (excludeTransactionId != null) {
            transactionRepository.getTotalTransferInExcluding(AccountType.DEPOSIT, excludeTransactionId)
        } else {
            transactionRepository.getTotalTransferIn(AccountType.DEPOSIT)
        }
        val transferOut = if (excludeTransactionId != null) {
            transactionRepository.getTotalTransferOutExcluding(AccountType.DEPOSIT, excludeTransactionId)
        } else {
            transactionRepository.getTotalTransferOut(AccountType.DEPOSIT)
        }
        val walletMoveIn = if (excludeTransactionId != null) {
            transactionRepository.getTotalWalletMoveInExcluding(AccountType.DEPOSIT, excludeTransactionId)
        } else {
            transactionRepository.getTotalWalletMoveIn(AccountType.DEPOSIT)
        }
        val walletMoveOut = if (excludeTransactionId != null) {
            transactionRepository.getTotalWalletMoveOutExcluding(AccountType.DEPOSIT, excludeTransactionId)
        } else {
            transactionRepository.getTotalWalletMoveOut(AccountType.DEPOSIT)
        }

        val totalIn = income + transferIn + walletMoveIn
        val totalOut = expense + transferOut + walletMoveOut
        return (totalIn - totalOut).coerceAtLeast(0.0)
    }

    suspend fun getDepositSummary(yearStart: Date? = null, yearEnd: Date? = null): DepositSummary {
        val balance = getDepositBalance()
        val totalDeposited = transactionRepository.getTotalIncomeForAccount(AccountType.DEPOSIT) +
            transactionRepository.getTotalTransferIn(AccountType.DEPOSIT) +
            transactionRepository.getTotalWalletMoveIn(AccountType.DEPOSIT)
        val totalPurchases = transactionRepository.getTotalExpenseForAccount(AccountType.DEPOSIT)
        val totalTransferredOut = transactionRepository.getTotalTransferOut(AccountType.DEPOSIT) +
            transactionRepository.getTotalWalletMoveOut(AccountType.DEPOSIT)

        val yearPurchases = if (yearStart != null && yearEnd != null) {
            transactionRepository.getPeriodExpenseForAccount(AccountType.DEPOSIT, yearStart, yearEnd)
        } else 0.0

        return DepositSummary(
            currentBalance = balance,
            totalDeposited = totalDeposited,
            totalCapitalPurchases = totalPurchases,
            totalTransferredOut = totalTransferredOut,
            yearCapitalPurchases = yearPurchases
        )
    }

    suspend fun getDepositTransactions(startDate: Date? = null, endDate: Date? = null): List<Transaction> {
        val all = if (startDate != null && endDate != null) {
            transactionRepository.getTransactionsBetweenDatesSnapshot(startDate, endDate)
        } else {
            transactionRepository.getAllTransactionsSnapshot()
        }
        return all.filter {
            it.accountType == AccountType.DEPOSIT || it.toAccountType == AccountType.DEPOSIT
        }
    }
}
