package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.CategoryDao
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : CategoryRepository {
    override fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByType(type)

    override suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)

    override suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    override suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)

    override fun getSubcategories(parentId: Long): Flow<List<SubcategoryEntity>> = categoryDao.getSubcategories(parentId)

    override suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long = categoryDao.insertSubcategory(subcategory)

    override suspend fun updateSubcategory(subcategory: SubcategoryEntity) = categoryDao.updateSubcategory(subcategory)

    override suspend fun deleteSubcategory(subcategory: SubcategoryEntity) = categoryDao.deleteSubcategory(subcategory)

    override suspend fun getTransactionCountForCategory(categoryId: Long): Int = 
        transactionDao.getTransactionCountForCategory(categoryId)

    override suspend fun getTransactionCountForSubcategory(subcategoryId: Long): Int = 
        transactionDao.getTransactionCountForSubcategory(subcategoryId)

    override suspend fun seedDefaults() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            val defaults = listOf(
                Pair(CategoryEntity(name = "Food & Dining", type = "EXPENSE", color = 0, icon = "🍔", budgetLimit = null, parentId = null), listOf("Groceries", "Restaurants", "Cafe/Drinks", "Fast Food")),
                Pair(CategoryEntity(name = "Transportation", type = "EXPENSE", color = 0, icon = "🚗", budgetLimit = null, parentId = null), listOf("Fuel/Petrol", "Public Transport", "Taxi/Uber", "Maintenance")),
                Pair(CategoryEntity(name = "Shopping", type = "EXPENSE", color = 0, icon = "🛍️", budgetLimit = null, parentId = null), listOf("Clothing", "Electronics", "Gifts", "Online Shopping")),
                Pair(CategoryEntity(name = "Housing", type = "EXPENSE", color = 0, icon = "🏠", budgetLimit = null, parentId = null), listOf("Rent", "EMI", "Home Improvement", "Insurance")),
                Pair(CategoryEntity(name = "Bills & Utilities", type = "EXPENSE", color = 0, icon = "💡", budgetLimit = null, parentId = null), listOf("Electricity", "Water", "Internet", "Mobile Recharge")),
                Pair(CategoryEntity(name = "Health & Wellness", type = "EXPENSE", color = 0, icon = "💊", budgetLimit = null, parentId = null), listOf("Doctor", "Pharmacy", "Gym", "Personal Care")),
                Pair(CategoryEntity(name = "Entertainment", type = "EXPENSE", color = 0, icon = "🎬", budgetLimit = null, parentId = null), listOf("Movies", "Subscriptions", "Hobbies", "Gaming")),
                Pair(CategoryEntity(name = "Fees & Charges", type = "EXPENSE", color = 0, icon = "💳", budgetLimit = null, parentId = null), listOf("Interest", "Bank Fees", "Late Fees")),
                Pair(CategoryEntity(name = "Income", type = "INCOME", color = 0, icon = "💰", budgetLimit = null, parentId = null), listOf("Salary", "Freelance", "Gifts", "Investment Profit"))
            )

            defaults.forEach { (category, subs) ->
                val categoryId = categoryDao.insert(category)
                subs.forEach { subName ->
                    categoryDao.insertSubcategory(SubcategoryEntity(categoryId = categoryId, name = subName))
                }
            }
        }
    }
}
