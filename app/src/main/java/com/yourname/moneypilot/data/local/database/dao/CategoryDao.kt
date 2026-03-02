package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubcategories(subcategories: List<SubcategoryEntity>)

    @Update
    suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    @Delete
    suspend fun deleteSubcategory(subcategory: SubcategoryEntity)

    @Query("SELECT * FROM subcategories WHERE categoryId = :parentId ORDER BY name ASC")
    fun getSubcategories(parentId: Long): Flow<List<SubcategoryEntity>>

    @Query("SELECT * FROM subcategories")
    suspend fun getAllSubcategoriesList(): List<SubcategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
