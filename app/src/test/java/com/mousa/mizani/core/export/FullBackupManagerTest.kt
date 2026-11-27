package com.mousa.mizani.core.export

import android.content.Context
import android.util.Log
import com.mousa.mizani.core.security.AppLockAvailability
import com.mousa.mizani.data.database.AppDatabase
import com.mousa.mizani.data.database.dao.AppAlertDao
import com.mousa.mizani.data.database.dao.BudgetDao
import com.mousa.mizani.data.database.dao.CategoryDao
import com.mousa.mizani.data.database.dao.ConfiguredLoanDao
import com.mousa.mizani.data.database.dao.MonthlyLoanPaymentDao
import com.mousa.mizani.data.database.dao.RecurringTransactionDao
import com.mousa.mizani.data.database.dao.SubCategoryDao
import com.mousa.mizani.data.database.dao.TransactionDao
import com.mousa.mizani.data.database.dao.TransactionSplitDao
import com.mousa.mizani.data.preferences.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import kotlin.coroutines.Continuation

class FullBackupManagerTest {

    private val transactionDao = mock(TransactionDao::class.java)
    private val splitDao = mock(TransactionSplitDao::class.java)
    private val categoryDao = mock(CategoryDao::class.java)
    private val daos = listOf<Any>(transactionDao, splitDao, categoryDao)
    private val database = mock(AppDatabase::class.java)

    /** What the settings import does; replaced per test. */
    private var settingsImport: () -> Unit = {}
    private var settingsImportCalls = 0
    private var importedSettings: Map<*, *>? = null
    private var isLockAvailable = true
    private var isLockOnBeforeRestore = false

    private val userPreferences = mock(UserPreferences::class.java, Answer<Any?> { invocation ->
        when (invocation.method.name) {
            "importAllPreferencesMap" -> {
                settingsImportCalls++
                importedSettings = invocation.arguments[0] as Map<*, *>
                settingsImport()
                Unit
            }
            "getBiometricLockEnabled" -> flowOf(isLockOnBeforeRestore)
            else -> Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    })

    private val manager = FullBackupManager(
        mock(Context::class.java), database, userPreferences, AppLockAvailability { isLockAvailable }
    )

    // android.util.Log has no JVM implementation.
    private lateinit var log: MockedStatic<Log>

    // Room's withTransaction needs a real database, so here it just runs its block.
    private lateinit var roomTransaction: MockedStatic<*>

    @Before
    fun setUp() {
        log = mockStatic(Log::class.java)
        roomTransaction = mockStatic(Class.forName("androidx.room.RoomDatabaseKt"), Answer(::runBlockArgument))
        `when`(database.transactionDao()).thenReturn(transactionDao)
        `when`(database.transactionSplitDao()).thenReturn(splitDao)
        `when`(database.categoryDao()).thenReturn(categoryDao)
        `when`(database.subCategoryDao()).thenReturn(mock(SubCategoryDao::class.java))
        `when`(database.budgetDao()).thenReturn(mock(BudgetDao::class.java))
        `when`(database.configuredLoanDao()).thenReturn(mock(ConfiguredLoanDao::class.java))
        `when`(database.monthlyLoanPaymentDao()).thenReturn(mock(MonthlyLoanPaymentDao::class.java))
        `when`(database.recurringTransactionDao()).thenReturn(mock(RecurringTransactionDao::class.java))
        `when`(database.appAlertDao()).thenReturn(mock(AppAlertDao::class.java))
    }

    @After
    fun tearDown() {
        roomTransaction.close()
        log.close()
    }

    @Suppress("UNCHECKED_CAST")
    private fun runBlockArgument(invocation: InvocationOnMock): Any? {
        check(invocation.method.name == "withTransaction") { "Unexpected call: ${invocation.method}" }
        // For suspend functions, only the raw arguments still carry the continuation.
        val (_, block, continuation) = invocation.rawArguments
        return (block as Function1<Continuation<Any?>, Any?>).invoke(continuation as Continuation<Any?>)
    }

    private fun deleteCalls(): List<String> = daos.flatMap { dao ->
        mockingDetails(dao).invocations.map { it.method.name }.filter { it.startsWith("delete") }
    }

    private fun backup(preferences: String?): String {
        val settings = preferences?.let { """"preferences": $it,""" }.orEmpty()
        return """
            {"version": 1, $settings
             "categories": [], "subCategories": [], "budgets": [], "configuredLoans": [],
             "monthlyLoanPayments": [], "recurringTransactions": [], "transactions": [],
             "transactionSplits": [], "appAlerts": []}
        """.trimIndent()
    }

    @Test
    fun `restore with settings reports both as restored`() = runTest {
        val result = manager.importFullBackup(backup("""{"language":"ar"}"""))

        assertTrue(result.errorMessage, result.isSuccess)
        assertTrue(result.preferencesRestored)
        assertFalse(result.settingsFailed)
        assertEquals(1, settingsImportCalls)
        assertTrue(deleteCalls().isNotEmpty())
    }

    @Test
    fun `file without settings is not reported as a settings failure`() = runTest {
        val result = manager.importFullBackup(backup(null))

        assertTrue(result.errorMessage, result.isSuccess)
        assertFalse(result.preferencesRestored)
        assertFalse(result.settingsFailed)
        assertEquals(0, settingsImportCalls)
    }

    @Test
    fun `settings failure after the records were replaced is reported as partial success`() = runTest {
        settingsImport = { throw IllegalStateException("disk full") }

        val result = manager.importFullBackup(backup("""{"language":"ar"}"""))

        assertTrue(result.isSuccess)
        assertTrue(result.settingsFailed)
        assertFalse(result.preferencesRestored)
        assertNull(result.errorMessage)
        assertTrue(deleteCalls().isNotEmpty())
    }

    @Test
    fun `restore does not turn the app lock on when this phone cannot unlock it`() = runTest {
        isLockAvailable = false

        val result = manager.importFullBackup(
            backup("""{"biometric_lock_enabled":"true","language":"ar","theme_mode":"DARK"}""")
        )

        assertTrue(result.preferencesRestored)
        assertTrue(result.appLockSkipped)
        assertFalse(result.appLockTurnedOff)
        assertEquals(mapOf("language" to "ar", "theme_mode" to "DARK"), importedSettings)
    }

    @Test
    fun `restore turns the app lock on when this phone can unlock it`() = runTest {
        isLockAvailable = true

        val result = manager.importFullBackup(backup("""{"biometric_lock_enabled":"true","language":"ar"}"""))

        assertFalse(result.appLockSkipped)
        assertEquals(mapOf("biometric_lock_enabled" to "true", "language" to "ar"), importedSettings)
    }

    @Test
    fun `restore can always turn the app lock off`() = runTest {
        isLockAvailable = false

        manager.importFullBackup(backup("""{"biometric_lock_enabled":"false"}"""))

        assertEquals(mapOf("biometric_lock_enabled" to "false"), importedSettings)
    }

    @Test
    fun `restore that turns off a lock that was on says so`() = runTest {
        isLockOnBeforeRestore = true

        val result = manager.importFullBackup(backup("""{"biometric_lock_enabled":"false"}"""))

        assertTrue(result.appLockTurnedOff)
        assertFalse(result.appLockSkipped)
    }

    @Test
    fun `restore that keeps the lock off reports no lock change`() = runTest {
        isLockOnBeforeRestore = false

        val result = manager.importFullBackup(backup("""{"biometric_lock_enabled":"false"}"""))

        assertFalse(result.appLockTurnedOff)
    }

    @Test
    fun `failed settings import reports no lock change`() = runTest {
        isLockAvailable = false
        isLockOnBeforeRestore = true
        settingsImport = { throw IllegalStateException("disk full") }

        val skipped = manager.importFullBackup(backup("""{"biometric_lock_enabled":"true"}"""))
        val turnedOff = manager.importFullBackup(backup("""{"biometric_lock_enabled":"false"}"""))

        assertTrue(skipped.settingsFailed)
        assertFalse(skipped.appLockSkipped)
        assertFalse(turnedOff.appLockTurnedOff)
    }

    @Test
    fun `cancellation during the settings import is rethrown`() = runTest {
        val cancellation = CancellationException("left the screen")
        settingsImport = { throw cancellation }

        try {
            manager.importFullBackup(backup("""{"language":"ar"}"""))
            fail("Expected cancellation")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
    }

    @Test
    fun `rejected file never touches the database or settings`() = runTest {
        listOf("{}", """{"name":"something else"}""", """{"version":1,"categories":[]}""", "not json")
            .forEach { file ->
                val result = manager.importFullBackup(file)

                assertFalse(file, result.isSuccess)
                assertFalse(file, result.settingsFailed)
                assertNotNull(file, result.errorMessage)
            }

        assertEquals(emptyList<String>(), deleteCalls())
        verifyNoInteractions(database, transactionDao, splitDao, categoryDao)
        assertEquals(0, settingsImportCalls)
    }
}
