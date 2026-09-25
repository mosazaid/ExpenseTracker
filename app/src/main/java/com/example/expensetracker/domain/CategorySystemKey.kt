package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType

enum class CategorySystemKey(val displayName: String, val type: TransactionType) {
    SALARY("Salary", TransactionType.INCOME),
    DEPT("Dept", TransactionType.EXPENSE),
    LOAN("Loan", TransactionType.EXPENSE)
}

fun Category?.matchesSystemKey(key: CategorySystemKey): Boolean {
    if (this == null) return false
    return type == key.type && name.equals(key.displayName, ignoreCase = true)
}

fun Category?.isDept(): Boolean {
    if (this == null) return false
    return name.equals("Dept", ignoreCase = true) || name.equals("Debt", ignoreCase = true)
}

fun Category?.isLoan(): Boolean {
    if (this == null) return false
    return name.equals("Loan", ignoreCase = true) || name.equals("Loans", ignoreCase = true)
}


