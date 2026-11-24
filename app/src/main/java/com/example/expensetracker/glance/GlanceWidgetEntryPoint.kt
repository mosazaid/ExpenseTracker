package com.example.expensetracker.glance

import com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao
import com.example.expensetracker.data.preferences.UserPreferences
import com.example.expensetracker.domain.calculator.BalanceCalculator
import com.example.expensetracker.domain.calculator.PeriodCalculator
import com.example.expensetracker.domain.calculator.WalletCalculator
import com.example.expensetracker.domain.repository.ITransactionRepository
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

