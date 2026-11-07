package com.example.expensetracker.data.di

import com.example.expensetracker.data.repository.AlertRepositoryImpl
import com.example.expensetracker.data.repository.BudgetRepositoryImpl
import com.example.expensetracker.data.repository.CategoryRepositoryImpl
import com.example.expensetracker.data.repository.CurrencyExchangeRepositoryImpl
import com.example.expensetracker.data.repository.ItemPriceRepositoryImpl
import com.example.expensetracker.data.repository.LoanRepositoryImpl
import com.example.expensetracker.data.repository.RecurringRepositoryImpl
import com.example.expensetracker.data.repository.TransactionRepositoryImpl
import com.example.expensetracker.data.repository.TripRepositoryImpl
import com.example.expensetracker.domain.repository.IAlertRepository
import com.example.expensetracker.domain.repository.IBudgetRepository
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ICurrencyExchangeRepository
import com.example.expensetracker.domain.repository.IItemPriceRepository
import com.example.expensetracker.domain.repository.ILoanRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import com.example.expensetracker.domain.repository.ITripRepository
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

    @Binds
    @Singleton
    abstract fun bindCurrencyExchangeRepository(impl: CurrencyExchangeRepositoryImpl): ICurrencyExchangeRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): ITripRepository

    @Binds
    @Singleton
    abstract fun bindItemPriceRepository(impl: ItemPriceRepositoryImpl): IItemPriceRepository
}
