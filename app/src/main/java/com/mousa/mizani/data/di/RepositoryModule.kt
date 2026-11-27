package com.mousa.mizani.data.di

import com.mousa.mizani.data.database.RoomTransactionRunner
import com.mousa.mizani.domain.repository.TransactionRunner
import com.mousa.mizani.data.repository.AlertRepositoryImpl
import com.mousa.mizani.data.repository.BudgetRepositoryImpl
import com.mousa.mizani.data.repository.CategoryRepositoryImpl
import com.mousa.mizani.data.repository.CurrencyExchangeRepositoryImpl
import com.mousa.mizani.data.repository.ItemPriceRepositoryImpl
import com.mousa.mizani.data.repository.LoanRepositoryImpl
import com.mousa.mizani.data.repository.RecurringRepositoryImpl
import com.mousa.mizani.data.repository.TransactionRepositoryImpl
import com.mousa.mizani.data.repository.TripRepositoryImpl
import com.mousa.mizani.domain.repository.IAlertRepository
import com.mousa.mizani.domain.repository.IBudgetRepository
import com.mousa.mizani.domain.repository.ICategoryRepository
import com.mousa.mizani.domain.repository.ICurrencyExchangeRepository
import com.mousa.mizani.domain.repository.IItemPriceRepository
import com.mousa.mizani.domain.repository.ILoanRepository
import com.mousa.mizani.domain.repository.IRecurringRepository
import com.mousa.mizani.domain.repository.ITransactionRepository
import com.mousa.mizani.domain.repository.ITripRepository
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
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner

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
