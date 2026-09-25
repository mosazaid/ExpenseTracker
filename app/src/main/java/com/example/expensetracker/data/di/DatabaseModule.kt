package com.example.expensetracker.data.di

import android.content.Context
import com.example.expensetracker.data.database.AppDatabase
import com.example.expensetracker.data.database.dao.AppAlertDao
import com.example.expensetracker.data.database.dao.BudgetDao
import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.ConfiguredLoanDao
import com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao
import com.example.expensetracker.data.database.dao.RecurringTransactionDao
import com.example.expensetracker.data.database.dao.SubCategoryDao
import com.example.expensetracker.data.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideRecurringTransactionDao(database: AppDatabase): RecurringTransactionDao {
        return database.recurringTransactionDao()
    }

    @Provides
    fun provideSubCategoryDao(database: AppDatabase): SubCategoryDao {
        return database.subCategoryDao()
    }

    @Provides
    fun provideConfiguredLoanDao(database: AppDatabase): ConfiguredLoanDao {
        return database.configuredLoanDao()
    }

    @Provides
    fun provideMonthlyLoanPaymentDao(database: AppDatabase): MonthlyLoanPaymentDao {
        return database.monthlyLoanPaymentDao()
    }

    @Provides
    fun provideAppAlertDao(database: AppDatabase): AppAlertDao {
        return database.appAlertDao()
    }
}