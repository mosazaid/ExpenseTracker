package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.dao.CategoryDao
import com.example.expensetracker.data.database.dao.SubCategoryDao
import com.example.expensetracker.data.database.entities.Category
import com.example.expensetracker.data.database.entities.SubCategory
import com.example.expensetracker.data.database.entities.TransactionType
import com.example.expensetracker.domain.repository.ICategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao
) : ICategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    override fun getCategoriesSortedByUsage(): Flow<List<Category>> {
        return categoryDao.getCategoriesSortedByUsage()
    }

    override suspend fun getAllCategoriesSnapshot(): List<Category> {
        return getAllCategories().first()
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }

    override fun getCategoriesByTypeSortedByUsage(type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByTypeSortedByUsage(type)
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)
    }

    override suspend fun getCategoryByName(name: String, type: TransactionType): Category? {
        return categoryDao.getCategoryByName(name, type)
    }

    override fun getDefaultCategories(): Flow<List<Category>> {
        return categoryDao.getDefaultCategories()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    override suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories)
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategoryById(category.id)
    }

    override fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        return subCategoryDao.getSubCategoriesByCategoryId(categoryId)
    }

    override suspend fun getSubCategoriesSnapshot(categoryId: Long): List<SubCategory> {
        return subCategoryDao.getSubCategoriesByCategoryIdSnapshot(categoryId)
    }

    override suspend fun saveSubCategory(categoryId: Long, name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return -1L
        val existing = subCategoryDao.getSubCategoryByName(categoryId, trimmed)
        if (existing != null) return existing.id
        return subCategoryDao.insertSubCategory(
            SubCategory(categoryId = categoryId, name = trimmed)
        )
    }

    override suspend fun deleteSubCategory(id: Long) {
        subCategoryDao.deleteSubCategoryById(id)
    }
}

typealias CategoryRepository = CategoryRepositoryImpl
