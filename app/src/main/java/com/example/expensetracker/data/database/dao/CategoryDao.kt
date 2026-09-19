package com.example.expensetracker.data.database.dao

import androidx.room.*
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("""
        SELECT c.* FROM categories c
        LEFT JOIN (
            SELECT categoryId, COUNT(*) as txn_count FROM transactions GROUP BY categoryId
        ) t ON c.id = t.categoryId
        ORDER BY COALESCE(t.txn_count, 0) DESC, c.name ASC
    """)
    fun getCategoriesSortedByUsage(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    @Query("""
        SELECT c.* FROM categories c
        LEFT JOIN (
            SELECT categoryId, COUNT(*) as txn_count FROM transactions GROUP BY categoryId
        ) t ON c.id = t.categoryId
        WHERE c.type = :type
        ORDER BY COALESCE(t.txn_count, 0) DESC, c.name ASC
    """)
    fun getCategoriesByTypeSortedByUsage(type: TransactionType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByName(name: String, type: TransactionType): Category?

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    fun getDefaultCategories(): Flow<List<Category>>

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Insert
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCategoryById(id: Long)
}
