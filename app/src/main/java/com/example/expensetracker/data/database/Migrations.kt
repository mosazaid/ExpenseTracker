package com.example.expensetracker.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = Migration(1, 2) { db ->
    db.execSQL("ALTER TABLE transactions ADD COLUMN toAccountType TEXT")
    db.execSQL(
        "ALTER TABLE transactions ADD COLUMN startsNewPeriod INTEGER NOT NULL DEFAULT 0"
    )
    db.execSQL(
        "ALTER TABLE transactions ADD COLUMN allowNegativeBalance INTEGER NOT NULL DEFAULT 0"
    )

    db.execSQL(
        "UPDATE categories SET name = 'Restaurants' WHERE name = 'Food & Dining' AND isDefault = 1"
    )

    val now = System.currentTimeMillis()
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Cafés', '☕', '#8D6E63', 'EXPENSE', 1, $now
        WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Cafés' AND type = 'EXPENSE')
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Charity', '🤝', '#E91E63', 'EXPENSE', 1, $now
        WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Charity' AND type = 'EXPENSE')
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Family', '👨‍👩‍👧', '#FF5722', 'EXPENSE', 1, $now
        WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Family' AND type = 'EXPENSE')
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Dept', '🔄', '#607D8B', 'INCOME', 1, $now
        WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Dept' AND type = 'INCOME')
        """.trimIndent()
    )
}

val MIGRATION_2_3 = Migration(2, 3) { db ->
    db.execSQL(
        """
        UPDATE transactions
        SET startsNewPeriod = 1
        WHERE type = 'INCOME'
          AND startsNewPeriod = 0
          AND categoryId = (
              SELECT id FROM categories
              WHERE name = 'Salary' AND type = 'INCOME'
              LIMIT 1
          )
        """.trimIndent()
    )
}

val MIGRATION_3_4 = Migration(3, 4) { db ->
    // Undo over-marking from v2→v3: only the latest salary should anchor the open period.
    db.execSQL(
        """
        UPDATE transactions
        SET startsNewPeriod = 0
        WHERE type = 'INCOME'
          AND categoryId = (
              SELECT id FROM categories
              WHERE name = 'Salary' AND type = 'INCOME'
              LIMIT 1
          )
        """.trimIndent()
    )
    db.execSQL(
        """
        UPDATE transactions
        SET startsNewPeriod = 1
        WHERE id = (
            SELECT id FROM transactions
            WHERE type = 'INCOME'
              AND categoryId = (
                  SELECT id FROM categories
                  WHERE name = 'Salary' AND type = 'INCOME'
                  LIMIT 1
              )
            ORDER BY date DESC
            LIMIT 1
        )
        """.trimIndent()
    )
}

val MIGRATION_4_5 = Migration(4, 5) { db ->
    db.execSQL("ALTER TABLE transactions ADD COLUMN linkedExpenseId INTEGER")
    db.execSQL("ALTER TABLE transactions ADD COLUMN debtorNote TEXT")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_linkedExpenseId ON transactions(linkedExpenseId)")

    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS budgets_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            categoryId INTEGER NOT NULL,
            amount REAL NOT NULL,
            periodStart INTEGER NOT NULL,
            periodEnd INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO budgets_new (id, categoryId, amount, periodStart, periodEnd, createdAt)
        SELECT
            id,
            categoryId,
            amount,
            CAST(strftime('%s', printf('%04d-%02d-01 00:00:00', year, month + 1)) AS INTEGER) * 1000,
            CAST(
                strftime(
                    '%s',
                    datetime(date(printf('%04d-%02d-01', year, month + 1), '+1 month', '-1 day'), '23:59:59')
                ) AS INTEGER
            ) * 1000,
            createdAt
        FROM budgets
        """.trimIndent()
    )
    db.execSQL("DROP TABLE budgets")
    db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
    db.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId_periodStart_periodEnd " +
            "ON budgets(categoryId, periodStart, periodEnd)"
    )
}

val MIGRATION_5_6 = Migration(5, 6) { db ->
    db.execSQL(
        "ALTER TABLE transactions ADD COLUMN awaitingReimbursement INTEGER NOT NULL DEFAULT 0"
    )
}

// Retroactively reassign all existing transactions (income, expense, transfer) to BANK,
// because the user's real-world setup uses a bank account as the primary account.
// Users can edit individual transactions to Cash via the edit screen going forward.
val MIGRATION_6_7 = Migration(6, 7) { db ->
    db.execSQL("UPDATE transactions SET accountType = 'BANK'")
    // For transfers the destination account (toAccountType) stays as-is so the user
    // can correct transfer direction if needed by editing the transfer.
}

val MIGRATION_7_8 = Migration(7, 8) { db ->
    val now = System.currentTimeMillis()
    val newCategories = listOf(
        Triple("Groceries", "🛒", "#43A047"),
        Triple("Cleaning supplies", "🧹", "#5C6BC0"),
        Triple("Personal care", "🧴", "#AB47BC"),
        Triple("Home maintenance", "🔧", "#6D4C41"),
        Triple("Subscriptions", "📱", "#26A69A"),
        Triple("Fuel", "⛽", "#FFA726")
    )
    newCategories.forEach { (name, icon, color) ->
        db.execSQL(
            """
            INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
            SELECT '$name', '$icon', '$color', 'EXPENSE', 1, $now
            WHERE NOT EXISTS (
                SELECT 1 FROM categories WHERE name = '$name' AND type = 'EXPENSE'
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_8_9 = Migration(8, 9) { db ->
    db.execSQL("ALTER TABLE transactions ADD COLUMN carriedForwardBalance REAL")
}

val MIGRATION_9_10 = Migration(9, 10) { db ->
    db.execSQL("ALTER TABLE transactions ADD COLUMN subDescription TEXT")
}

val MIGRATION_10_11 = Migration(10, 11) { db ->
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS sub_categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            categoryId INTEGER NOT NULL,
            name TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL(
        "CREATE INDEX IF NOT EXISTS index_sub_categories_categoryId ON sub_categories(categoryId)"
    )

    val now = System.currentTimeMillis()
    val updatedCategories = listOf(
        Triple("Car & Transportation", "🚗", "#FFA726"),
        Triple("Groceries & Food", "🛒", "#43A047"),
        Triple("Restaurants & Cafés", "🍽️", "#FF6B6B"),
        Triple("Subscriptions & AI / Work", "🤖", "#26A69A"),
        Triple("Travel & Entertainment", "🎭", "#96CEB4"),
        Triple("Personal Care & Home", "🧴", "#AB47BC"),
        Triple("Education & Learning", "📚", "#98D8C8")
    )
    updatedCategories.forEach { (name, icon, color) ->
        db.execSQL(
            """
            INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
            SELECT '$name', '$icon', '$color', 'EXPENSE', 1, $now
            WHERE NOT EXISTS (
                SELECT 1 FROM categories WHERE name = '$name' AND type = 'EXPENSE'
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_11_12 = Migration(11, 12) { db ->
    val now = System.currentTimeMillis()

    // 1. Rename "Groceries & Food" to "Groceries & Shopping"
    db.execSQL("UPDATE categories SET name = 'Groceries & Shopping' WHERE name = 'Groceries & Food' AND type = 'EXPENSE'")

    // Ensure all combined categories exist
    val targetCategories = listOf(
        Triple("Groceries & Shopping", "🛒", "#43A047"),
        Triple("Car & Transportation", "🚗", "#FFA726"),
        Triple("Restaurants & Cafés", "🍽️", "#FF6B6B"),
        Triple("Subscriptions & AI / Work", "🤖", "#26A69A"),
        Triple("Travel & Entertainment", "🎭", "#96CEB4"),
        Triple("Personal Care & Home", "🧴", "#AB47BC"),
        Triple("Education & Learning", "📚", "#98D8C8"),
        Triple("Family & Kids", "👨‍👩‍👧", "#FF5722"),
        Triple("Charity & Giving", "🤝", "#E91E63")
    )
    targetCategories.forEach { (name, icon, color) ->
        db.execSQL(
            """
            INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
            SELECT '$name', '$icon', '$color', 'EXPENSE', 1, $now
            WHERE NOT EXISTS (
                SELECT 1 FROM categories WHERE name = '$name' AND type = 'EXPENSE'
            )
            """.trimIndent()
        )
    }
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Freelance & Work', '💻', '#E67E22', 'INCOME', 1, $now
        WHERE NOT EXISTS (
            SELECT 1 FROM categories WHERE name = 'Freelance & Work' AND type = 'INCOME'
        )
        """.trimIndent()
    )

    // 2. Mappings of obsolete single categories -> combined category
    // (oldName, oldType, targetName, targetType, subCategoryName)
    val mappings = listOf(
        listOf("Groceries", "EXPENSE", "Groceries & Shopping", "EXPENSE", "Groceries"),
        listOf("Shopping", "EXPENSE", "Groceries & Shopping", "EXPENSE", "Shopping"),
        listOf("Food", "EXPENSE", "Groceries & Shopping", "EXPENSE", "Food"),
        listOf("Education", "EXPENSE", "Education & Learning", "EXPENSE", "Education"),
        listOf("Learning", "EXPENSE", "Education & Learning", "EXPENSE", "Learning"),
        listOf("Transportation", "EXPENSE", "Car & Transportation", "EXPENSE", "Transportation"),
        listOf("Fuel", "EXPENSE", "Car & Transportation", "EXPENSE", "Fuel"),
        listOf("Car", "EXPENSE", "Car & Transportation", "EXPENSE", "Car"),
        listOf("Restaurants", "EXPENSE", "Restaurants & Cafés", "EXPENSE", "Restaurants"),
        listOf("Cafés", "EXPENSE", "Restaurants & Cafés", "EXPENSE", "Cafés"),
        listOf("Food & Dining", "EXPENSE", "Restaurants & Cafés", "EXPENSE", "Dining"),
        listOf("Subscriptions", "EXPENSE", "Subscriptions & AI / Work", "EXPENSE", "Subscriptions"),
        listOf("Travel", "EXPENSE", "Travel & Entertainment", "EXPENSE", "Travel"),
        listOf("Entertainment", "EXPENSE", "Travel & Entertainment", "EXPENSE", "Entertainment"),
        listOf("Personal care", "EXPENSE", "Personal Care & Home", "EXPENSE", "Personal care"),
        listOf("Personal Care", "EXPENSE", "Personal Care & Home", "EXPENSE", "Personal care"),
        listOf("Cleaning supplies", "EXPENSE", "Personal Care & Home", "EXPENSE", "Cleaning supplies"),
        listOf("Home maintenance", "EXPENSE", "Personal Care & Home", "EXPENSE", "Home maintenance"),
        listOf("Family", "EXPENSE", "Family & Kids", "EXPENSE", "Family"),
        listOf("Charity", "EXPENSE", "Charity & Giving", "EXPENSE", "Charity"),
        listOf("Freelance", "INCOME", "Freelance & Work", "INCOME", "Freelance")
    )

    mappings.forEach { mapping ->
        val oldName = mapping[0]
        val oldType = mapping[1]
        val targetName = mapping[2]
        val targetType = mapping[3]
        val subCatName = mapping[4]

        val oldCursor = db.query("SELECT id FROM categories WHERE name = ? AND type = ?", arrayOf(oldName, oldType))
        if (oldCursor.moveToFirst()) {
            val oldId = oldCursor.getLong(0)
            oldCursor.close()

            val targetCursor = db.query("SELECT id FROM categories WHERE name = ? AND type = ?", arrayOf(targetName, targetType))
            if (targetCursor.moveToFirst()) {
                val targetId = targetCursor.getLong(0)
                targetCursor.close()

                if (oldId != targetId) {
                    // Update transactions
                    db.execSQL(
                        "UPDATE transactions SET categoryId = ?, subDescription = CASE WHEN (subDescription IS NULL OR TRIM(subDescription) = '') THEN ? ELSE subDescription END WHERE categoryId = ?",
                        arrayOf(targetId, subCatName, oldId)
                    )

                    // Ensure subcategory exists
                    db.execSQL(
                        "INSERT INTO sub_categories (categoryId, name, createdAt) SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM sub_categories WHERE categoryId = ? AND LOWER(name) = LOWER(?))",
                        arrayOf(targetId, subCatName, now, targetId, subCatName)
                    )

                    // Reassign recurring transactions
                    db.execSQL(
                        "UPDATE recurring_transactions SET categoryId = ? WHERE categoryId = ?",
                        arrayOf(targetId, oldId)
                    )

                    // Reassign budgets safely avoiding duplicate period collisions
                    db.execSQL(
                        """
                        DELETE FROM budgets WHERE categoryId = ? AND EXISTS (
                            SELECT 1 FROM budgets b2 WHERE b2.categoryId = ? 
                            AND b2.periodStart = budgets.periodStart 
                            AND b2.periodEnd = budgets.periodEnd
                        )
                        """.trimIndent(),
                        arrayOf(oldId, targetId)
                    )
                    db.execSQL(
                        "UPDATE budgets SET categoryId = ? WHERE categoryId = ?",
                        arrayOf(targetId, oldId)
                    )

                    // Delete old single category
                    db.execSQL("DELETE FROM categories WHERE id = ?", arrayOf(oldId))
                }
            } else {
                targetCursor.close()
            }
        } else {
            oldCursor.close()
        }
    }

    // 3. Seed standard subcategories for the main combined categories
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
                    "INSERT INTO sub_categories (categoryId, name, createdAt) SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM sub_categories WHERE categoryId = ? AND LOWER(name) = LOWER(?))",
                    arrayOf(catId, subName, now, catId, subName)
                )
            }
        } else {
            cursor.close()
        }
    }
}

val MIGRATION_12_13 = Migration(12, 13) { db ->
    // 1. Add debtType and isDebtSettled to transactions
    db.execSQL("ALTER TABLE transactions ADD COLUMN debtType TEXT")
    db.execSQL("ALTER TABLE transactions ADD COLUMN isDebtSettled INTEGER NOT NULL DEFAULT 0")

    // 2. Create configured_loans table
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS configured_loans (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            defaultAmount REAL NOT NULL,
            accountType TEXT NOT NULL,
            isActive INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
        )
        """.trimIndent()
    )

    // 3. Create monthly_loan_payments table
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS monthly_loan_payments (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            loanConfigId INTEGER NOT NULL,
            monthKey TEXT NOT NULL,
            amount REAL NOT NULL,
            accountType TEXT NOT NULL,
            isPaid INTEGER NOT NULL,
            paidDate INTEGER,
            transactionId INTEGER,
            isDismissed INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            FOREIGN KEY(loanConfigId) REFERENCES configured_loans(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent()
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_monthly_loan_payments_loanConfigId ON monthly_loan_payments(loanConfigId)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_loan_payments_loanConfigId_monthKey ON monthly_loan_payments(loanConfigId, monthKey)")

    // 4. Create app_alerts table
    db.execSQL(
        """
        CREATE TABLE IF NOT EXISTS app_alerts (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            type TEXT NOT NULL,
            title TEXT NOT NULL,
            message TEXT NOT NULL,
            relatedId INTEGER,
            periodKey TEXT NOT NULL,
            isDismissed INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
        )
        """.trimIndent()
    )

    // 5. Seed default Loan and Dept expense categories if not present
    val now = System.currentTimeMillis()
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Loan', '🏦', '#3F51B5', 'EXPENSE', 1, $now
        WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Loan' AND type = 'EXPENSE')
        """.trimIndent()
    )
    db.execSQL(
        """
        INSERT INTO categories (name, icon, color, type, isDefault, createdAt)
        SELECT 'Dept', '🔄', '#607D8B', 'EXPENSE', 1, $now
        WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Dept' AND type = 'EXPENSE')
        """.trimIndent()
    )
}

val MIGRATION_13_14 = Migration(13, 14) { db ->
    db.execSQL(
        "ALTER TABLE configured_loans ADD COLUMN deductFromIncome INTEGER NOT NULL DEFAULT 1"
    )
}

val MIGRATION_14_15 = Migration(14, 15) { db ->
    db.execSQL("ALTER TABLE transactions ADD COLUMN recurringId INTEGER")
    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_recurringId ON transactions(recurringId)")
}

