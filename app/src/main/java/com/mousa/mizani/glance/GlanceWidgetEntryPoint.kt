package com.mousa.mizani.glance

import com.mousa.mizani.data.database.dao.MonthlyLoanPaymentDao
import com.mousa.mizani.data.preferences.UserPreferences
import com.mousa.mizani.domain.calculator.BalanceCalculator
import com.mousa.mizani.domain.calculator.PeriodCalculator
import com.mousa.mizani.domain.calculator.WalletCalculator
import com.mousa.mizani.domain.repository.ITransactionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlanceWidgetEntryPoint {
    fun balanceCalculator(): BalanceCalculator
    fun periodCalculator(): PeriodCalculator
    fun walletCalculator(): WalletCalculator
    fun userPreferences(): UserPreferences
    fun transactionRepository(): ITransactionRepository
    fun monthlyLoanPaymentDao(): MonthlyLoanPaymentDao?
}

