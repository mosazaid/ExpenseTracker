package com.mousa.mizani.domain.model

import com.mousa.mizani.data.database.entities.ConfiguredLoan
import com.mousa.mizani.data.database.entities.MonthlyLoanPayment

data class MonthlyLoanItem(
    val payment: MonthlyLoanPayment,
    val loanConfig: ConfiguredLoan
)
