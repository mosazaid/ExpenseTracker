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
    version = 10,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            insertDefaultCategories(db)
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun insertDefaultCategories(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            val expenseCategories = listOf(
                "('Restaurants', '🍽️', '#FF6B6B', 'EXPENSE', 1, $now)",
                "('Cafés', '☕', '#8D6E63', 'EXPENSE', 1, $now)",
                "('Groceries', '🛒', '#43A047', 'EXPENSE', 1, $now)",
                "('Cleaning supplies', '🧹', '#5C6BC0', 'EXPENSE', 1, $now)",
                "('Personal care', '🧴', '#AB47BC', 'EXPENSE', 1, $now)",
                "('Home maintenance', '🔧', '#6D4C41', 'EXPENSE', 1, $now)",
                "('Subscriptions', '📱', '#26A69A', 'EXPENSE', 1, $now)",
                "('Fuel', '⛽', '#FFA726', 'EXPENSE', 1, $now)",
                "('Transportation', '\uD83D\uDE97', '#4ECDC4', 'EXPENSE', 1, $now)",
                "('Shopping', '🛒', '#45B7D1', 'EXPENSE', 1, $now)",
                "('Entertainment', '\uD83C\uDFAC', '#96CEB4', 'EXPENSE', 1, $now)",
                "('Bills & Utilities', '💡', '#FFEAA7', 'EXPENSE', 1, $now)",
                "('Healthcare', '🏥', '#DDA0DD', 'EXPENSE', 1, $now)",
                "('Education', '📚', '#98D8C8', 'EXPENSE', 1, $now)",
                "('Travel', '✈️', '#F7DC6F', 'EXPENSE', 1, $now)",
                "('Charity', '🤝', '#E91E63', 'EXPENSE', 1, $now)",
                "('Family', '👨‍👩‍👧', '#FF5722', 'EXPENSE', 1, $now)",
                "('Other', '📋', '#BDC3C7', 'EXPENSE', 1, $now)",
            )

            val incomeCategories = listOf(
                "('Salary', '💰', '#2ECC71', 'INCOME', 1, $now)",
                "('Dept', '🔄', '#607D8B', 'INCOME', 1, $now)",
                "('Business', '🏢', '#3498DB', 'INCOME', 1, $now)",
                "('Investment', '📈', '#9B59B6', 'INCOME', 1, $now)",
                "('Freelance', '💻', '#E67E22', 'INCOME', 1, $now)",
                "('Gift', '🎁', '#E74C3C', 'INCOME', 1, $now)",
                "('Other', '📋', '#95A5A6', 'INCOME', 1, $now)",
            )

            expenseCategories.forEach { category ->
                db.execSQL(
                    "INSERT INTO categories (name, icon, color, type, isDefault, createdAt) VALUES $category"
                )
            }

            incomeCategories.forEach { category ->
                db.execSQL(
                    "INSERT INTO categories (name, icon, color, type, isDefault, createdAt) VALUES $category"
                )
            }
        }
    }
}
