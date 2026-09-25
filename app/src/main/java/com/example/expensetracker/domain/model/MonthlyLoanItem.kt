package com.example.expensetracker.domain.model

import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.database.entities.MonthlyLoanPayment

data class MonthlyLoanItem(
    val payment: MonthlyLoanPayment,
    val loanConfig: ConfiguredLoan
)
