package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.SubCategoryDao
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun getAllCategoriesSnapshot(): List<Category> {
        return getAllCategories().first()
    }

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun getCategoryByName(name: String, type: TransactionType): Category? {
        return categoryDao.getCategoryByName(name, type)
    }

    fun getDefaultCategories(): Flow<List<Category>> {
        return categoryDao.getDefaultCategories()
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategoryById(category.id)
    }

    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        return subCategoryDao.getSubCategoriesByCategoryId(categoryId)
    }

    suspend fun getSubCategoriesSnapshot(categoryId: Long): List<SubCategory> {
        return subCategoryDao.getSubCategoriesByCategoryIdSnapshot(categoryId)
    }

    suspend fun saveSubCategory(categoryId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return -1L
        val existing = subCategoryDao.getSubCategoryByName(categoryId, trimmed)
        if (existing != null) return existing.id
        return subCategoryDao.insertSubCategory(
            SubCategory(categoryId = categoryId, name = trimmed)
        )
    }

    suspend fun deleteSubCategory(id: Long) {
        subCategoryDao.deleteSubCategoryById(id)
    }
}
