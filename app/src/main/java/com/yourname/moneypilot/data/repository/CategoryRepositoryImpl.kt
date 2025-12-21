package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.CategoryDao
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    override suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)

    override suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)
}
