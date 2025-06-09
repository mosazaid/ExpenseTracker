package com.example.expensetracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensetracker.data.database.dao.BudgetDao
import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.RecurringTransactionDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.entities.Budget
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.Transaction

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        RecurringTransaction::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate default categories
                            insertDefaultCategories(db)
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun insertDefaultCategories(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // Insert default expense categories
            val expenseCategories = listOf(
                "('Food & Dining', '🍽️', '#FF6B6B', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Transportation', '\uD83D\uDE97', '#4ECDC4', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Shopping', '🛒', '#45B7D1', 'EXPENSE', 1, ${System.currentTimeMillis()})",
                "('Entertainment', '\uD83C\uDFAC', '#96CEB4', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Bills & Utilities', '💡', '#FFEAA7', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Healthcare', '🏥', '#DDA0DD', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Education', '📚', '#98D8C8', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Travel', '✈️', '#F7DC6F', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
                "('Other', '📋', '#BDC3C7', 'EXPENSE', 1,  ${System.currentTimeMillis()})",
            )

            // Insert default income categories
            val incomeCategories = listOf(
                "('Salary', '💰', '#2ECC71', 'INCOME', 1,  ${System.currentTimeMillis()})",
                "('Business', '🏢', '#3498DB', 'INCOME', 1,  ${System.currentTimeMillis()})",
                "('Investment', '📈', '#9B59B6', 'INCOME', 1,  ${System.currentTimeMillis()})",
                "('Freelance', '💻', '#E67E22', 'INCOME', 1,  ${System.currentTimeMillis()})",
                "('Gift', '🎁', '#E74C3C', 'INCOME', 1,  ${System.currentTimeMillis()})",
                "('Other', '📋', '#95A5A6', 'INCOME', 1,  ${System.currentTimeMillis()})",
            )

            expenseCategories.forEach { category ->
                db.execSQL("INSERT INTO categories (name, icon, color, type, isDefault, createdAt) VALUES $category")
            }

            incomeCategories.forEach { category ->
                db.execSQL("INSERT INTO categories (name, icon, color, type, isDefault, createdAt) VALUES $category")
            }
        }
    }
}