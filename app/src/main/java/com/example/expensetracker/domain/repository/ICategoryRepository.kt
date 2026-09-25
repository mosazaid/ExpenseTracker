package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow

interface ICategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesSortedByUsage(): Flow<List<Category>>
    suspend fun getAllCategoriesSnapshot(): List<Category>
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>
    fun getCategoriesByTypeSortedByUsage(type: TransactionType): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun getCategoryByName(name: String, type: TransactionType): Category?
    fun getDefaultCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun insertCategories(categories: List<Category>)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>>
    suspend fun getSubCategoriesSnapshot(categoryId: Long): List<SubCategory>
    suspend fun saveSubCategory(categoryId: Long, name: String): Long
    suspend fun deleteSubCategory(id: Long)
}
