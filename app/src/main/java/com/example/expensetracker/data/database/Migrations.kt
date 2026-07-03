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
