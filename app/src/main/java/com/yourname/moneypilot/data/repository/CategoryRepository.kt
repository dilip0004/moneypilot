package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
    suspend fun getCategoryById(id: Long): CategoryEntity?
    suspend fun insertCategory(category: CategoryEntity): Long
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    
    // Subcategory support
    fun getSubcategories(parentId: Long): Flow<List<SubcategoryEntity>>
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long
    suspend fun updateSubcategory(subcategory: SubcategoryEntity)
    suspend fun deleteSubcategory(subcategory: SubcategoryEntity)
    
    // Seeding
    suspend fun seedDefaults()
}
