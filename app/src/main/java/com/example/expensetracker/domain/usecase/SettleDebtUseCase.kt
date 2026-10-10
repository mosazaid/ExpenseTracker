package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Transaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.repository.ITransactionRepository
import java.util.Date
import javax.inject.Inject

/**
 * Clean Architecture Use Case: Settle Debt
 * Encapsulates the business rule for debt settlement:
 * 1. Mark original debt transaction as settled.
 * 2. Optionally create an offsetting repayment transaction.
 */
class SettleDebtUseCase @Inject constructor(
    private val transactionRepository: ITransactionRepository
) {
    suspend operator fun invoke(
        debtTransaction: Transaction,
        settleAccountType: AccountType = AccountType.CASH,
        createCounterTransaction: Boolean = true
    ): Long? {
        // 1. Mark original debt as settled
        val updatedDebt = debtTransaction.copy(
            isDebtSettled = true,
            updatedAt = Date()
        )
        transactionRepository.updateTransaction(updatedDebt)

        // 2. If creating counter transaction:
        // If it was LENT (Expense), user receives money back -> Repayment INCOME.
        // If it was BORROWED (Income), user pays money back -> Repayment EXPENSE.
        if (createCounterTransaction) {
            val isLent = debtTransaction.debtType?.equals("LENT", ignoreCase = true) == true ||
                    debtTransaction.type == TransactionType.EXPENSE

            val repaymentType = if (isLent) TransactionType.INCOME else TransactionType.EXPENSE
            val description = "Repayment: ${debtTransaction.debtorNote ?: debtTransaction.description}"

            val counterTxn = Transaction(
                amount = debtTransaction.amount,
                description = description,
                subDescription = "Settled debt #${debtTransaction.id}",
                date = Date(),
                type = repaymentType,
                categoryId = debtTransaction.categoryId,
                accountType = settleAccountType,
                debtorNote = debtTransaction.debtorNote,
                isDebtSettled = true,
                linkedExpenseId = debtTransaction.id
            )
            return transactionRepository.insertTransaction(counterTxn)
        }
        return null
    }
}
