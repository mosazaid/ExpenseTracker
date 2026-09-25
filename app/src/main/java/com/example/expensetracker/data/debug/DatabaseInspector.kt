package com.example.expensetracker.data.debug

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.data.database.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

data class DbColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val primaryKey: Boolean
)

data class DbTableData(
    val tableName: String,
    val columns: List<DbColumnInfo>,
    val rows: List<Map<String, String?>>
)

@Singleton
class DatabaseInspector @Inject constructor(
    private val appDatabase: AppDatabase
) {
    private val allowedTables = setOf(
        "transactions",
        "categories",
        "sub_categories",
        "budgets",
        "recurring_transactions",
        "configured_loans",
        "monthly_loan_payments",
        "app_alerts"
    )

    fun getTableNames(): List<String> {
        val db = readableDatabase()
        val cursor = db.query(
            """
            SELECT name FROM sqlite_master
            WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'
            ORDER BY name
            """.trimIndent()
        )
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    if (name in allowedTables) add(name)
                }
            }
        }
    }

    fun inspectTable(tableName: String, columnFilters: Map<String, String>): DbTableData {
        require(tableName in allowedTables) { "Table not allowed: $tableName" }
        val db = readableDatabase()
        val columns = getColumnInfo(db, tableName)
        val activeFilters = columnFilters.filterValues { it.isNotBlank() }
        val whereClause = if (activeFilters.isEmpty()) {
            ""
        } else {
            activeFilters.entries.joinToString(" AND ") { (column, _) ->
                val safeColumn = columns.firstOrNull { it.name == column }?.name
                    ?: throw IllegalArgumentException("Unknown column: $column")
                "$safeColumn LIKE ?"
            }.let { " WHERE $it" }
        }
        val args = activeFilters.values.map { "%$it%" }.toTypedArray()
        val sql = "SELECT * FROM $tableName$whereClause ORDER BY rowid DESC LIMIT 500"
        val cursor = db.query(sql, args)
        val columnNames = columns.map { it.name }
        val rows = cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        columnNames.associateWith { col ->
                            val index = it.getColumnIndex(col)
                            if (index < 0 || it.isNull(index)) null else it.getString(index)
                        }
                    )
                }
            }
        }
        return DbTableData(tableName = tableName, columns = columns, rows = rows)
    }

    private fun getColumnInfo(db: SupportSQLiteDatabase, tableName: String): List<DbColumnInfo> {
        val cursor = db.query("PRAGMA table_info($tableName)")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        DbColumnInfo(
                            name = it.getString(it.getColumnIndex("name")),
                            type = it.getString(it.getColumnIndex("type")),
                            notNull = it.getInt(it.getColumnIndex("notnull")) == 1,
                            primaryKey = it.getInt(it.getColumnIndex("pk")) > 0
                        )
                    )
                }
            }
        }
    }

    private fun readableDatabase(): SupportSQLiteDatabase =
        appDatabase.openHelper.readableDatabase
}
