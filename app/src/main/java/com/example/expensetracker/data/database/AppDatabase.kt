package com.example.expensetracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expensetracker.data.database.dao.AppAlertDao
import com.example.expensetracker.data.database.dao.BudgetDao
import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.ConfiguredLoanDao
import com.example.expensetracker.data.database.dao.MonthlyLoanPaymentDao
import com.example.expensetracker.data.database.dao.RecurringTransactionDao
import com.example.expensetracker.data.database.dao.TransactionDao
import com.example.expensetracker.data.database.dao.SubCategoryDao
import com.example.expensetracker.data.database.entities.AppAlert
import com.example.expensetracker.data.database.entities.Budget
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.ConfiguredLoan
import com.example.expensetracker.data.database.entities.MonthlyLoanPayment
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.data.database.entities.Transaction

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        RecurringTransaction::class,
        SubCategory::class,
        ConfiguredLoan::class,
        MonthlyLoanPayment::class,
        AppAlert::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun configuredLoanDao(): ConfiguredLoanDao
    abstract fun monthlyLoanPaymentDao(): MonthlyLoanPaymentDao
    abstract fun appAlertDao(): AppAlertDao

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
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15
                    )
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
                "('Car & Transportation', '🚗', '#FFA726', 'EXPENSE', 1, $now)",
                "('Groceries & Shopping', '🛒', '#43A047', 'EXPENSE', 1, $now)",
                "('Restaurants & Cafés', '🍽️', '#FF6B6B', 'EXPENSE', 1, $now)",
                "('Subscriptions & AI / Work', '🤖', '#26A69A', 'EXPENSE', 1, $now)",
                "('Travel & Entertainment', '🎭', '#96CEB4', 'EXPENSE', 1, $now)",
                "('Personal Care & Home', '🧴', '#AB47BC', 'EXPENSE', 1, $now)",
                "('Bills & Utilities', '💡', '#FFEAA7', 'EXPENSE', 1, $now)",
                "('Healthcare', '🏥', '#DDA0DD', 'EXPENSE', 1, $now)",
                "('Education & Learning', '📚', '#98D8C8', 'EXPENSE', 1, $now)",
                "('Family & Kids', '👨‍👩‍👧', '#FF5722', 'EXPENSE', 1, $now)",
                "('Charity & Giving', '🤝', '#E91E63', 'EXPENSE', 1, $now)",
                "('Loan', '🏦', '#3F51B5', 'EXPENSE', 1, $now)",
                "('Dept', '🔄', '#607D8B', 'EXPENSE', 1, $now)",
                "('Other', '📋', '#BDC3C7', 'EXPENSE', 1, $now)",
            )

            val incomeCategories = listOf(
                "('Salary', '💰', '#2ECC71', 'INCOME', 1, $now)",
                "('Freelance & Work', '💻', '#E67E22', 'INCOME', 1, $now)",
                "('Business', '🏢', '#3498DB', 'INCOME', 1, $now)",
                "('Investment', '📈', '#9B59B6', 'INCOME', 1, $now)",
                "('Dept', '🔄', '#607D8B', 'INCOME', 1, $now)",
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

            // Seed default subcategories
            val defaultSubCats = listOf(
                Pair("Groceries & Shopping", listOf("Groceries", "Shopping")),
                Pair("Education & Learning", listOf("Education", "Learning", "Courses", "Books")),
                Pair("Car & Transportation", listOf("Transportation", "Fuel", "Car", "Maintenance")),
                Pair("Restaurants & Cafés", listOf("Restaurants", "Cafés", "Fast Food")),
                Pair("Subscriptions & AI / Work", listOf("Subscriptions", "AI", "Work")),
                Pair("Travel & Entertainment", listOf("Travel", "Entertainment", "Movies")),
                Pair("Personal Care & Home", listOf("Personal care", "Cleaning supplies", "Home maintenance")),
                Pair("Family & Kids", listOf("Family", "Kids")),
                Pair("Charity & Giving", listOf("Charity", "Giving")),
                Pair("Freelance & Work", listOf("Freelance", "Work", "Consulting"))
            )

            defaultSubCats.forEach { (catName, subCats) ->
                val cursor = db.query("SELECT id FROM categories WHERE name = ?", arrayOf(catName))
                if (cursor.moveToFirst()) {
                    val catId = cursor.getLong(0)
                    cursor.close()
                    subCats.forEach { subName ->
                        db.execSQL(
                            "INSERT INTO sub_categories (categoryId, name, createdAt) VALUES ($catId, '$subName', $now)"
                        )
                    }
                } else {
                    cursor.close()
                }
            }
        }
    }
}
