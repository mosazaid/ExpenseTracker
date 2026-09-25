package com.example.expensetracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.database.AppDatabase
import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseInstrumentedTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetCategory() = runBlocking {
        val category = Category(
            name = "Test Groceries",
            icon = "shopping_cart",
            color = "#FF5722",
            type = TransactionType.EXPENSE,
            isDefault = false
        )
        val id = categoryDao.insertCategory(category)
        val fetched = categoryDao.getCategoryById(id)

        assertNotNull(fetched)
        assertEquals("Test Groceries", fetched?.name)
        assertEquals(TransactionType.EXPENSE, fetched?.type)
        assertEquals("#FF5722", fetched?.color)
    }

    @Test
    fun getCategoryByNameAndType() = runBlocking {
        val category = Category(
            name = "Test Salary",
            icon = "attach_money",
            color = "#4CAF50",
            type = TransactionType.INCOME,
            isDefault = true
        )
        categoryDao.insertCategory(category)

        val fetched = categoryDao.getCategoryByName("Test Salary", TransactionType.INCOME)
        assertNotNull(fetched)
        assertEquals("Test Salary", fetched?.name)
        assertEquals(true, fetched?.isDefault)
    }
}
