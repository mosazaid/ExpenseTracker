package com.example.expensetracker.data.di

import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTransactionRepository(transactionDao: TransactionDao): TransactionRepository {
        return TransactionRepository(transactionDao)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository {
        return CategoryRepository(categoryDao)
    }
}