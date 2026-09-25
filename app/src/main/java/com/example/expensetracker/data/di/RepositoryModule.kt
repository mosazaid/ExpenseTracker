package com.example.expensetracker.data.di

import com.example.expensetracker.data.repository.AlertRepositoryImpl
import com.example.expensetracker.data.repository.BudgetRepositoryImpl
import com.example.expensetracker.data.repository.CategoryRepositoryImpl
import com.example.expensetracker.data.repository.LoanRepositoryImpl
import com.example.expensetracker.data.repository.RecurringRepositoryImpl
import com.example.expensetracker.data.repository.TransactionRepositoryImpl
import com.example.expensetracker.domain.repository.IAlertRepository
import com.example.expensetracker.domain.repository.IBudgetRepository
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ILoanRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): ITransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): ICategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): IBudgetRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): IRecurringRepository

    @Binds
    @Singleton
    abstract fun bindLoanRepository(impl: LoanRepositoryImpl): ILoanRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): IAlertRepository
}
