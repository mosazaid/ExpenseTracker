package com.mousa.mizani.core.export

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.mousa.mizani.core.export.backup.BackupJsonCodec
import com.mousa.mizani.core.export.backup.BackupSanitizer
import com.mousa.mizani.core.export.backup.BackupSnapshot
import com.mousa.mizani.data.database.AppDatabase
import com.mousa.mizani.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
    /** Records that were skipped or repaired because their parent no longer existed. */
    val fixedRecordsCount: Int = 0,
    val errorMessage: String? = null
)

/**
 * Exports every table plus preferences to one JSON file, and restores such a file
 * (including format-1 files written before the Mizani rename).
 *
 * Every failure and every repaired record is logged under the [TAG] tag in Logcat.
 */
@Singleton
class FullBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val userPreferences: UserPreferences
) {

    suspend fun exportFullBackup(): File {
        val snapshot = readSnapshot()
        val json = BackupJsonCodec.encode(
            snapshot = snapshot,
            preferences = userPreferences.exportAllPreferencesMap(),
            databaseVersion = appDatabase.openHelper.readableDatabase.version,
            exportedAt = SimpleDateFormat(ISO_FORMAT, Locale.US).format(Date())
        )
        val timestamp = SimpleDateFormat(FILE_TIMESTAMP_FORMAT, Locale.US).format(Date())
        val file = File(context.cacheDir, "mizani_full_backup_$timestamp.json")
        file.writeText(json.toString(2), Charsets.UTF_8)
        Log.i(TAG, "Exported backup ${file.name}: ${describe(snapshot)}")
        return file
    }

    suspend fun importFullBackup(jsonString: String): FullBackupRestoreResult {
        return try {
            val decoded = BackupJsonCodec.decode(jsonString)
            Log.i(TAG, "Restoring backup format ${decoded.formatVersion}: ${describe(decoded.snapshot)}")

            val sanitized = BackupSanitizer.sanitize(decoded.snapshot)
            sanitized.fixes.forEach { Log.w(TAG, "Repaired during restore: $it") }

            replaceDatabase(sanitized.snapshot)

            val preferences = decoded.preferences
            if (preferences != null) {
                userPreferences.importAllPreferencesMap(preferences)
            }

            val restored = sanitized.snapshot
            Log.i(TAG, "Restore finished (${sanitized.fixes.size} records repaired): ${describe(restored)}")
            FullBackupRestoreResult(
                isSuccess = true,
                transactionCount = restored.transactions.size,
                splitCount = restored.transactionSplits.size,
                categoryCount = restored.categories.size,
                subCategoryCount = restored.subCategories.size,
                budgetCount = restored.budgets.size,
                loanCount = restored.configuredLoans.size,
                loanPaymentCount = restored.monthlyLoanPayments.size,
                recurringCount = restored.recurringTransactions.size,
                alertCount = restored.appAlerts.size,
                preferencesRestored = preferences != null,
                fixedRecordsCount = sanitized.fixes.size
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Backup restoration failed", e)
            FullBackupRestoreResult(
                isSuccess = false,
                errorMessage = e.message ?: e.javaClass.simpleName
            )
        }
    }

    private suspend fun readSnapshot(): BackupSnapshot = BackupSnapshot(
        categories = appDatabase.categoryDao().getAllCategoriesSnapshot(),
        subCategories = appDatabase.subCategoryDao().getAllSubCategoriesSnapshot(),
        budgets = appDatabase.budgetDao().getAllBudgetsSnapshot(),
        configuredLoans = appDatabase.configuredLoanDao().getAllLoansSnapshot(),
        monthlyLoanPayments = appDatabase.monthlyLoanPaymentDao().getAllPaymentsSnapshot(),
        recurringTransactions = appDatabase.recurringTransactionDao().getAllRecurringTransactionsSnapshot(),
        transactions = appDatabase.transactionDao().getAllTransactionsSnapshot(),
        transactionSplits = appDatabase.transactionSplitDao().getAllSplitsSnapshot(),
        appAlerts = appDatabase.appAlertDao().getAllAlertsSnapshot(),
        trips = appDatabase.tripDao().getAllTripsSnapshot(),
        currencyExchanges = appDatabase.currencyExchangeDao().getAllExchangesSnapshot(),
        itemPriceHistory = appDatabase.itemPriceHistoryDao().getAllPriceRecordsSnapshot(),
        pendingSmsTransactions = appDatabase.pendingSmsTransactionDao().getAllPendingSmsSnapshot()
    )

    /** Replaces the tables atomically: if any insert fails, the device keeps its current data. */
    private suspend fun replaceDatabase(s: BackupSnapshot) = appDatabase.withTransaction {
        // Children before parents.
        appDatabase.transactionSplitDao().deleteAllSplits()
        appDatabase.transactionDao().deleteAllTransactions()
        appDatabase.monthlyLoanPaymentDao().deleteAllPayments()
        appDatabase.budgetDao().deleteAllBudgets()
        appDatabase.subCategoryDao().deleteAllSubCategories()
        appDatabase.recurringTransactionDao().deleteAllRecurringTransactions()
        appDatabase.configuredLoanDao().deleteAllLoans()
        appDatabase.categoryDao().deleteAllCategories()
        appDatabase.appAlertDao().deleteAllAlerts()

        // Parents before children.
        appDatabase.categoryDao().insertCategories(s.categories)
        appDatabase.subCategoryDao().insertSubCategories(s.subCategories)
        appDatabase.budgetDao().insertBudgets(s.budgets)
        appDatabase.configuredLoanDao().insertLoans(s.configuredLoans)
        appDatabase.recurringTransactionDao().insertRecurringTransactions(s.recurringTransactions)
        appDatabase.transactionDao().insertTransactions(s.transactions)
        appDatabase.transactionSplitDao().insertSplits(s.transactionSplits)
        appDatabase.monthlyLoanPaymentDao().insertPayments(s.monthlyLoanPayments)
        appDatabase.appAlertDao().insertAlerts(s.appAlerts)

        // Tables added in backup format 2: only replaced when the file contains them.
        s.trips?.let {
            appDatabase.tripDao().deleteAllTrips()
            appDatabase.tripDao().insertTrips(it)
        }
        s.currencyExchanges?.let {
            appDatabase.currencyExchangeDao().deleteAllExchanges()
            appDatabase.currencyExchangeDao().insertExchanges(it)
        }
        s.itemPriceHistory?.let {
            appDatabase.itemPriceHistoryDao().deleteAllRecords()
            appDatabase.itemPriceHistoryDao().insertRecords(it)
        }
        s.pendingSmsTransactions?.let {
            appDatabase.pendingSmsTransactionDao().deleteAllPendingSms()
            appDatabase.pendingSmsTransactionDao().insertAllPendingSms(it)
        }
    }

    private fun describe(s: BackupSnapshot): String =
        "categories=${s.categories.size}, subCategories=${s.subCategories.size}, " +
            "budgets=${s.budgets.size}, loans=${s.configuredLoans.size}, " +
            "loanPayments=${s.monthlyLoanPayments.size}, recurring=${s.recurringTransactions.size}, " +
            "transactions=${s.transactions.size}, splits=${s.transactionSplits.size}, " +
            "alerts=${s.appAlerts.size}, trips=${s.trips?.size ?: "-"}, " +
            "exchanges=${s.currencyExchanges?.size ?: "-"}, prices=${s.itemPriceHistory?.size ?: "-"}, " +
            "pendingSms=${s.pendingSmsTransactions?.size ?: "-"}"

    companion object {
        const val TAG = "FullBackup"
        private const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        private const val FILE_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"
    }
}
