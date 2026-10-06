package com.example.expensetracker.core.export

import android.content.Context
import androidx.room.withTransaction
import com.example.expensetracker.data.database.AppDatabase
import com.example.expensetracker.data.database.entities.*
import com.example.expensetracker.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class FullBackupRestoreResult(
    val isSuccess: Boolean,
    val transactionCount: Int = 0,
    val splitCount: Int = 0,
    val categoryCount: Int = 0,
    val subCategoryCount: Int = 0,
    val budgetCount: Int = 0,
    val loanCount: Int = 0,
    val loanPaymentCount: Int = 0,
    val recurringCount: Int = 0,
    val alertCount: Int = 0,
    val preferencesRestored: Boolean = false,
    val errorMessage: String? = null
)

@Singleton
class FullBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val userPreferences: UserPreferences
) {

    suspend fun exportFullBackup(): File {
        val root = JSONObject()
        root.put("version", 1)
        root.put("databaseVersion", 17)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        // 1. SharedPreferences / DataStore
        val prefsMap = userPreferences.exportAllPreferencesMap()
        val prefsJson = JSONObject()
        for ((key, value) in prefsMap) {
            prefsJson.put(key, value)
        }
        root.put("preferences", prefsJson)

        // 2. Categories
        val categories = appDatabase.categoryDao().getAllCategoriesSnapshot()
        val catArray = JSONArray()
        categories.forEach { cat ->
            catArray.put(JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("icon", cat.icon)
                put("color", cat.color)
                put("type", cat.type.name)
                put("isDefault", cat.isDefault)
                put("createdAt", cat.createdAt.time)
            })
        }
        root.put("categories", catArray)

        // 3. SubCategories
        val subCategories = appDatabase.subCategoryDao().getAllSubCategoriesSnapshot()
        val subCatArray = JSONArray()
        subCategories.forEach { sub ->
            subCatArray.put(JSONObject().apply {
                put("id", sub.id)
                put("categoryId", sub.categoryId)
                put("name", sub.name)
                put("createdAt", sub.createdAt.time)
            })
        }
        root.put("subCategories", subCatArray)

        // 4. Budgets
        val budgets = appDatabase.budgetDao().getAllBudgetsSnapshot()
        val budgetArray = JSONArray()
        budgets.forEach { b ->
            budgetArray.put(JSONObject().apply {
                put("id", b.id)
                put("categoryId", b.categoryId)
                put("amount", b.amount)
                put("periodStart", b.periodStart.time)
                put("periodEnd", b.periodEnd.time)
                put("createdAt", b.createdAt.time)
            })
        }
        root.put("budgets", budgetArray)

        // 5. Configured Loans
        val loans = appDatabase.configuredLoanDao().getAllLoansSnapshot()
        val loanArray = JSONArray()
        loans.forEach { loan ->
            loanArray.put(JSONObject().apply {
                put("id", loan.id)
                put("name", loan.name)
                put("defaultAmount", loan.defaultAmount)
                put("accountType", loan.accountType.name)
                put("deductFromIncome", loan.deductFromIncome)
                put("isActive", loan.isActive)
                put("createdAt", loan.createdAt.time)
            })
        }
        root.put("configuredLoans", loanArray)

        // 6. Monthly Loan Payments
        val payments = appDatabase.monthlyLoanPaymentDao().getAllPaymentsSnapshot()
        val paymentArray = JSONArray()
        payments.forEach { p ->
            paymentArray.put(JSONObject().apply {
                put("id", p.id)
                put("loanConfigId", p.loanConfigId)
                put("monthKey", p.monthKey)
                put("amount", p.amount)
                put("accountType", p.accountType.name)
                put("isPaid", p.isPaid)
                put("paidDate", p.paidDate?.time ?: JSONObject.NULL)
                put("transactionId", p.transactionId ?: JSONObject.NULL)
                put("isDismissed", p.isDismissed)
                put("createdAt", p.createdAt.time)
            })
        }
        root.put("monthlyLoanPayments", paymentArray)

        // 7. Recurring Transactions
        val recurring = appDatabase.recurringTransactionDao().getAllRecurringTransactionsSnapshot()
        val recurringArray = JSONArray()
        recurring.forEach { r ->
            recurringArray.put(JSONObject().apply {
                put("id", r.id)
                put("amount", r.amount)
                put("description", r.description)
                put("type", r.type.name)
                put("categoryId", r.categoryId ?: JSONObject.NULL)
                put("accountType", r.accountType.name)
                put("frequency", r.frequency.name)
                put("nextDueDate", r.nextDueDate.time)
                put("isActive", r.isActive)
                put("createdAt", r.createdAt.time)
            })
        }
        root.put("recurringTransactions", recurringArray)

        // 8. Transactions
        val transactions = appDatabase.transactionDao().getAllTransactionsSnapshot()
        val txnArray = JSONArray()
        transactions.forEach { t ->
            txnArray.put(JSONObject().apply {
                put("id", t.id)
                put("amount", t.amount)
                put("description", t.description)
                put("subDescription", t.subDescription ?: JSONObject.NULL)
                put("date", t.date.time)
                put("type", t.type.name)
                put("categoryId", t.categoryId ?: JSONObject.NULL)
                put("accountType", t.accountType.name)
                put("toAccountType", t.toAccountType?.name ?: JSONObject.NULL)
                put("startsNewPeriod", t.startsNewPeriod)
                put("carriedForwardBalance", t.carriedForwardBalance ?: JSONObject.NULL)
                put("allowNegativeBalance", t.allowNegativeBalance)
                put("linkedExpenseId", t.linkedExpenseId ?: JSONObject.NULL)
                put("debtorNote", t.debtorNote ?: JSONObject.NULL)
                put("awaitingReimbursement", t.awaitingReimbursement)
                put("debtType", t.debtType ?: JSONObject.NULL)
                put("isDebtSettled", t.isDebtSettled)
                put("recurringId", t.recurringId ?: JSONObject.NULL)
                put("receiptImagePath", t.receiptImagePath ?: JSONObject.NULL)
                put("createdAt", t.createdAt.time)
                put("updatedAt", t.updatedAt.time)
            })
        }
        root.put("transactions", txnArray)

        // 9. Transaction Splits
        val splits = appDatabase.transactionSplitDao().getAllSplitsSnapshot()
        val splitArray = JSONArray()
        splits.forEach { s ->
            splitArray.put(JSONObject().apply {
                put("id", s.id)
                put("transactionId", s.transactionId)
                put("subCategoryId", s.subCategoryId ?: JSONObject.NULL)
                put("subCategoryName", s.subCategoryName ?: JSONObject.NULL)
                put("amount", s.amount)
                put("note", s.note ?: JSONObject.NULL)
                put("isDebt", s.isDebt)
                put("debtPersonName", s.debtPersonName ?: JSONObject.NULL)
                put("isDebtSettled", s.isDebtSettled)
            })
        }
        root.put("transactionSplits", splitArray)

        // 10. App Alerts
        val alerts = appDatabase.appAlertDao().getAllAlertsSnapshot()
        val alertArray = JSONArray()
        alerts.forEach { a ->
            alertArray.put(JSONObject().apply {
                put("id", a.id)
                put("type", a.type)
                put("title", a.title)
                put("message", a.message)
                put("relatedId", a.relatedId ?: JSONObject.NULL)
                put("periodKey", a.periodKey)
                put("isDismissed", a.isDismissed)
                put("createdAt", a.createdAt.time)
            })
        }
        root.put("appAlerts", alertArray)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "expense_tracker_full_backup_$timestamp.json"
        val file = File(context.cacheDir, fileName)
        file.writeText(root.toString(2), Charsets.UTF_8)
        return file
    }

    suspend fun importFullBackup(jsonString: String): FullBackupRestoreResult {
        return try {
            val root = JSONObject(jsonString)

            // Extract collections before transaction to fail early if format is corrupted
            val catArray = root.optJSONArray("categories") ?: JSONArray()
            val subCatArray = root.optJSONArray("subCategories") ?: JSONArray()
            val budgetArray = root.optJSONArray("budgets") ?: JSONArray()
            val loanArray = root.optJSONArray("configuredLoans") ?: JSONArray()
            val paymentArray = root.optJSONArray("monthlyLoanPayments") ?: JSONArray()
            val recurringArray = root.optJSONArray("recurringTransactions") ?: JSONArray()
            val txnArray = root.optJSONArray("transactions") ?: JSONArray()
            val splitArray = root.optJSONArray("transactionSplits") ?: JSONArray()
            val alertArray = root.optJSONArray("appAlerts") ?: JSONArray()

            val categoriesList = mutableListOf<Category>()
            for (i in 0 until catArray.length()) {
                val o = catArray.getJSONObject(i)
                categoriesList.add(
                    Category(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        icon = o.getString("icon"),
                        color = o.getString("color"),
                        type = TransactionType.valueOf(o.getString("type")),
                        isDefault = o.optBoolean("isDefault", false),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            val subCategoriesList = mutableListOf<SubCategory>()
            for (i in 0 until subCatArray.length()) {
                val o = subCatArray.getJSONObject(i)
                subCategoriesList.add(
                    SubCategory(
                        id = o.getLong("id"),
                        categoryId = o.getLong("categoryId"),
                        name = o.getString("name"),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            val budgetsList = mutableListOf<Budget>()
            for (i in 0 until budgetArray.length()) {
                val o = budgetArray.getJSONObject(i)
                budgetsList.add(
                    Budget(
                        id = o.getLong("id"),
                        categoryId = o.getLong("categoryId"),
                        amount = o.getDouble("amount"),
                        periodStart = Date(o.getLong("periodStart")),
                        periodEnd = Date(o.getLong("periodEnd")),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            val loansList = mutableListOf<ConfiguredLoan>()
            for (i in 0 until loanArray.length()) {
                val o = loanArray.getJSONObject(i)
                loansList.add(
                    ConfiguredLoan(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        defaultAmount = o.getDouble("defaultAmount"),
                        accountType = AccountType.valueOf(o.optString("accountType", "BANK")),
                        deductFromIncome = o.optBoolean("deductFromIncome", true),
                        isActive = o.optBoolean("isActive", true),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            val recurringList = mutableListOf<RecurringTransaction>()
            for (i in 0 until recurringArray.length()) {
                val o = recurringArray.getJSONObject(i)
                recurringList.add(
                    RecurringTransaction(
                        id = o.getLong("id"),
                        amount = o.getDouble("amount"),
                        description = o.getString("description"),
                        type = TransactionType.valueOf(o.getString("type")),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        accountType = AccountType.valueOf(o.optString("accountType", "BANK")),
                        frequency = RecurrenceFrequency.valueOf(o.optString("frequency", "MONTHLY")),
                        nextDueDate = Date(o.getLong("nextDueDate")),
                        isActive = o.optBoolean("isActive", true),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            val transactionsList = mutableListOf<Transaction>()
            for (i in 0 until txnArray.length()) {
                val o = txnArray.getJSONObject(i)
                transactionsList.add(
                    Transaction(
                        id = o.getLong("id"),
                        amount = o.getDouble("amount"),
                        description = o.getString("description"),
                        subDescription = if (o.isNull("subDescription")) null else o.getString("subDescription"),
                        date = Date(o.getLong("date")),
                        type = TransactionType.valueOf(o.getString("type")),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        accountType = AccountType.valueOf(o.getString("accountType")),
                        toAccountType = if (o.isNull("toAccountType")) null else AccountType.valueOf(o.getString("toAccountType")),
                        startsNewPeriod = o.optBoolean("startsNewPeriod", false),
                        carriedForwardBalance = if (o.isNull("carriedForwardBalance")) null else o.getDouble("carriedForwardBalance"),
                        allowNegativeBalance = o.optBoolean("allowNegativeBalance", false),
                        linkedExpenseId = if (o.isNull("linkedExpenseId")) null else o.getLong("linkedExpenseId"),
                        debtorNote = if (o.isNull("debtorNote")) null else o.getString("debtorNote"),
                        awaitingReimbursement = o.optBoolean("awaitingReimbursement", false),
                        debtType = if (o.isNull("debtType")) null else o.getString("debtType"),
                        isDebtSettled = o.optBoolean("isDebtSettled", false),
                        recurringId = if (o.isNull("recurringId")) null else o.getLong("recurringId"),
                        receiptImagePath = if (o.isNull("receiptImagePath")) null else o.getString("receiptImagePath"),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis())),
                        updatedAt = Date(o.optLong("updatedAt", System.currentTimeMillis()))
                    )
                )
            }

            val splitsList = mutableListOf<TransactionSplit>()
            for (i in 0 until splitArray.length()) {
                val o = splitArray.getJSONObject(i)
                splitsList.add(
                    TransactionSplit(
                        id = o.getLong("id"),
                        transactionId = o.getLong("transactionId"),
                        subCategoryId = if (o.isNull("subCategoryId")) null else o.getLong("subCategoryId"),
                        subCategoryName = if (o.isNull("subCategoryName")) null else o.getString("subCategoryName"),
                        amount = o.getDouble("amount"),
                        note = if (o.isNull("note")) null else o.getString("note"),
                        isDebt = o.optBoolean("isDebt", false),
                        debtPersonName = if (o.isNull("debtPersonName")) null else o.getString("debtPersonName"),
                        isDebtSettled = o.optBoolean("isDebtSettled", false)
                    )
                )
            }

            val paymentsList = mutableListOf<MonthlyLoanPayment>()
            for (i in 0 until paymentArray.length()) {
                val o = paymentArray.getJSONObject(i)
                paymentsList.add(
                    MonthlyLoanPayment(
                        id = o.getLong("id"),
                        loanConfigId = o.getLong("loanConfigId"),
                        monthKey = o.getString("monthKey"),
                        amount = o.getDouble("amount"),
                        accountType = AccountType.valueOf(o.optString("accountType", "BANK")),
                        isPaid = o.optBoolean("isPaid", false),
                        paidDate = if (o.isNull("paidDate")) null else Date(o.getLong("paidDate")),
                        transactionId = if (o.isNull("transactionId")) null else o.getLong("transactionId"),
                        isDismissed = o.optBoolean("isDismissed", false),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            val alertsList = mutableListOf<AppAlert>()
            for (i in 0 until alertArray.length()) {
                val o = alertArray.getJSONObject(i)
                alertsList.add(
                    AppAlert(
                        id = o.getLong("id"),
                        type = o.getString("type"),
                        title = o.getString("title"),
                        message = o.getString("message"),
                        relatedId = if (o.isNull("relatedId")) null else o.getLong("relatedId"),
                        periodKey = o.optString("periodKey", ""),
                        isDismissed = o.optBoolean("isDismissed", false),
                        createdAt = Date(o.optLong("createdAt", System.currentTimeMillis()))
                    )
                )
            }

            // Execute full replacement inside atomic Room transaction
            appDatabase.withTransaction {
                // Delete in reverse FK order
                appDatabase.transactionSplitDao().deleteAllSplits()
                appDatabase.transactionDao().deleteAllTransactions()
                appDatabase.monthlyLoanPaymentDao().deleteAllPayments()
                appDatabase.budgetDao().deleteAllBudgets()
                appDatabase.subCategoryDao().deleteAllSubCategories()
                appDatabase.recurringTransactionDao().deleteAllRecurringTransactions()
                appDatabase.configuredLoanDao().deleteAllLoans()
                appDatabase.categoryDao().deleteAllCategories()
                appDatabase.appAlertDao().deleteAllAlerts()

                // Insert in safe FK dependency order
                if (categoriesList.isNotEmpty()) {
                    appDatabase.categoryDao().insertCategories(categoriesList)
                }
                if (subCategoriesList.isNotEmpty()) {
                    appDatabase.subCategoryDao().insertSubCategories(subCategoriesList)
                }
                if (budgetsList.isNotEmpty()) {
                    appDatabase.budgetDao().insertBudgets(budgetsList)
                }
                if (loansList.isNotEmpty()) {
                    appDatabase.configuredLoanDao().insertLoans(loansList)
                }
                if (recurringList.isNotEmpty()) {
                    appDatabase.recurringTransactionDao().insertRecurringTransactions(recurringList)
                }
                if (transactionsList.isNotEmpty()) {
                    appDatabase.transactionDao().insertTransactions(transactionsList)
                }
                if (splitsList.isNotEmpty()) {
                    appDatabase.transactionSplitDao().insertSplits(splitsList)
                }
                if (paymentsList.isNotEmpty()) {
                    appDatabase.monthlyLoanPaymentDao().insertPayments(paymentsList)
                }
                if (alertsList.isNotEmpty()) {
                    appDatabase.appAlertDao().insertAlerts(alertsList)
                }
            }

            // Restore SharedPreferences / DataStore
            val prefsJson = root.optJSONObject("preferences")
            var prefsRestored = false
            if (prefsJson != null) {
                val map = mutableMapOf<String, String>()
                val keys = prefsJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = prefsJson.optString(k, "")
                }
                userPreferences.importAllPreferencesMap(map)
                prefsRestored = true
            }

            FullBackupRestoreResult(
                isSuccess = true,
                transactionCount = transactionsList.size,
                splitCount = splitsList.size,
                categoryCount = categoriesList.size,
                subCategoryCount = subCategoriesList.size,
                budgetCount = budgetsList.size,
                loanCount = loansList.size,
                loanPaymentCount = paymentsList.size,
                recurringCount = recurringList.size,
                alertCount = alertsList.size,
                preferencesRestored = prefsRestored
            )
        } catch (e: Exception) {
            FullBackupRestoreResult(
                isSuccess = false,
                errorMessage = e.message ?: "Failed to parse or restore backup"
            )
        }
    }
}
