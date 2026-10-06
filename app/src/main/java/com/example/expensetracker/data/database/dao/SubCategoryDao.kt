package com.example.expensetracker.data.database.dao

import androidx.room.*
import com.example.expensetracker.data.database.entities.SubCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface SubCategoryDao {

    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getSubCategoriesByCategoryId(categoryId: Long): Flow<List<SubCategory>>

    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId ORDER BY name ASC")
    suspend fun getSubCategoriesByCategoryIdSnapshot(categoryId: Long): List<SubCategory>

    @Query("SELECT * FROM sub_categories ORDER BY name ASC")
    fun getAllSubCategories(): Flow<List<SubCategory>>

    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSubCategoryByName(categoryId: Long, name: String): SubCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategories(subCategories: List<SubCategory>): List<Long>

    @Query("DELETE FROM sub_categories WHERE id = :id")
    suspend fun deleteSubCategoryById(id: Long)

    @Query("SELECT * FROM sub_categories ORDER BY id ASC")
    suspend fun getAllSubCategoriesSnapshot(): List<SubCategory>

    @Query("DELETE FROM sub_categories")
    suspend fun deleteAllSubCategories()
}
