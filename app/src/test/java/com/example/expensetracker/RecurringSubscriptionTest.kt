package com.example.expensetracker

import com.example.expensetracker.data.database.dao.RecurringTransactionDao
import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.RecurrenceFrequency
import com.example.expensetracker.data.database.entities.RecurringTransaction
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.data.repository.RecurringRepositoryImpl
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.IRecurringRepository
import com.example.expensetracker.presentation.viewModel.RecurringViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringSubscriptionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    class FakeRecurringDao : RecurringTransactionDao {
        val items = mutableListOf<RecurringTransaction>()
        var lastUpdated: RecurringTransaction? = null

        override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> =
            flowOf(items.filter { it.isActive })

        override suspend fun getDueRecurringTransactions(date: Date): List<RecurringTransaction> =
            items.filter { it.isActive && it.nextDueDate <= date }

        override suspend fun getRecurringTransactionById(id: Long): RecurringTransaction? =
            items.find { it.id == id }

        override suspend fun insertRecurringTransaction(transaction: RecurringTransaction): Long {
            val newId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
            val created = transaction.copy(id = newId)
            items.add(created)
            return newId
        }

        override suspend fun updateRecurringTransaction(transaction: RecurringTransaction) {
            val idx = items.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) {
                items[idx] = transaction
            }
            lastUpdated = transaction
        }

        override suspend fun deleteRecurringTransaction(transaction: RecurringTransaction) {
            items.removeAll { it.id == transaction.id }
        }

        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> =
            flowOf(items)

        override suspend fun deactivateRecurringTransaction(id: Long) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) {
                items[idx] = items[idx].copy(isActive = false)
            }
        }

        override suspend fun setRecurringActive(id: Long, isActive: Boolean) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) {
                items[idx] = items[idx].copy(isActive = isActive)
            }
        }

        override suspend fun deleteRecurringTransactionById(id: Long) {
            items.removeAll { it.id == id }
        }
    }

    class FakeCategoryRepository : ICategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesSortedByUsage(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getAllCategoriesSnapshot(): List<Category> = emptyList()
        override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> = flowOf(emptyList())
        override fun getCategoriesByTypeSortedByUsage(type: TransactionType): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun getCategoryByName(name: String, type: TransactionType): Category? = null
        override fun getDefaultCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun insertCategories(categories: List<Category>) {}
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(category: Category) {}
        override fun getSubCategories(categoryId: Long): Flow<List<com.example.expensetracker.data.database.entities.SubCategory>> = flowOf(emptyList())
        override suspend fun getSubCategoriesSnapshot(categoryId: Long): List<com.example.expensetracker.data.database.entities.SubCategory> = emptyList()
        override suspend fun saveSubCategory(categoryId: Long, name: String): Long = 1L
        override suspend fun deleteSubCategory(id: Long) {}
    }

    @Test
    fun advanceRecurringDueDate_advancesCorrectlyByFrequency() = runTest {
        val fakeDao = FakeRecurringDao()
        val repo = RecurringRepositoryImpl(fakeDao)

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 10, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDate = cal.time

        // 1. Daily
        val daily = RecurringTransaction(
            id = 1L,
            amount = 10.0,
            description = "Daily Pass",
            type = TransactionType.EXPENSE,
            categoryId = null,
            accountType = AccountType.CASH,
            frequency = RecurrenceFrequency.DAILY,
            nextDueDate = initialDate
        )
        fakeDao.items.add(daily)
        repo.advanceRecurringDueDate(daily)

        val expectedDailyCal = Calendar.getInstance().apply {
            time = initialDate
            add(Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals(expectedDailyCal.time, fakeDao.lastUpdated?.nextDueDate)

        // 2. Weekly
        val weekly = daily.copy(id = 2L, frequency = RecurrenceFrequency.WEEKLY)
        fakeDao.items.add(weekly)
        repo.advanceRecurringDueDate(weekly)

        val expectedWeeklyCal = Calendar.getInstance().apply {
            time = initialDate
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        assertEquals(expectedWeeklyCal.time, fakeDao.lastUpdated?.nextDueDate)

        // 3. Monthly
        val monthly = daily.copy(id = 3L, frequency = RecurrenceFrequency.MONTHLY)
        fakeDao.items.add(monthly)
        repo.advanceRecurringDueDate(monthly)

        val expectedMonthlyCal = Calendar.getInstance().apply {
            time = initialDate
            add(Calendar.MONTH, 1)
        }
        assertEquals(expectedMonthlyCal.time, fakeDao.lastUpdated?.nextDueDate)

        // 4. Yearly
        val yearly = daily.copy(id = 4L, frequency = RecurrenceFrequency.YEARLY)
        fakeDao.items.add(yearly)
        repo.advanceRecurringDueDate(yearly)

        val expectedYearlyCal = Calendar.getInstance().apply {
            time = initialDate
            add(Calendar.YEAR, 1)
        }
        assertEquals(expectedYearlyCal.time, fakeDao.lastUpdated?.nextDueDate)
    }

    @Test
    fun saveRecurringTransaction_insertsNewAndUpdatesExisting() = runTest {
        val fakeDao = FakeRecurringDao()
        val repo = RecurringRepositoryImpl(fakeDao)
        val catRepo = FakeCategoryRepository()
        val txnRepo = org.mockito.Mockito.mock(com.example.expensetracker.domain.repository.ITransactionRepository::class.java)
        val viewModel = RecurringViewModel(repo, catRepo, txnRepo)

        val targetDate = Date()

        // Insert new recurring item
        viewModel.saveRecurringTransaction(
            id = 0L,
            amount = 45.0,
            description = "Gym Membership",
            type = TransactionType.EXPENSE,
            categoryId = 5L,
            accountType = AccountType.BANK,
            frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = targetDate
        )
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeDao.items.size)
        val inserted = fakeDao.items.first()
        assertEquals("Gym Membership", inserted.description)
        assertEquals(45.0, inserted.amount, 0.001)
        assertEquals(AccountType.BANK, inserted.accountType)
        assertEquals(RecurrenceFrequency.MONTHLY, inserted.frequency)

        // Edit existing item
        viewModel.saveRecurringTransaction(
            id = inserted.id,
            amount = 50.0,
            description = "Gym Membership Premium",
            type = TransactionType.EXPENSE,
            categoryId = 5L,
            accountType = AccountType.BANK,
            frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = targetDate
        )
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeDao.items.size)
        val updated = fakeDao.items.first()
        assertEquals("Gym Membership Premium", updated.description)
        assertEquals(50.0, updated.amount, 0.001)
    }
}
