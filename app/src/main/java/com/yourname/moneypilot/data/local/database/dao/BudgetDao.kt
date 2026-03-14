package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import com.yourname.moneypilot.data.local.database.entities.CategoryEntity
import com.yourname.moneypilot.data.local.database.entities.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class BudgetWithDetails(
    @Embedded val budget: BudgetEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity,
    @Relation(
        parentColumn = "subcategory_id",
        entityColumn = "id"
    )
    val subcategory: SubcategoryEntity?
)

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY start_date DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId ORDER BY start_date DESC")
    fun getBudgetsByCategory(categoryId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE start_date <= :date AND end_date >= :date")
    fun getActiveBudgets(date: LocalDate): Flow<List<BudgetEntity>>

    @Transaction
    @Query("SELECT * FROM budgets WHERE start_date <= :date AND end_date >= :date")
    fun getActiveBudgetsWithDetails(date: LocalDate): Flow<List<BudgetWithDetails>>

    @Query("UPDATE budgets SET spent_amount = :spentAmount WHERE id = :budgetId")
    suspend fun updateSpentAmount(budgetId: Long, spentAmount: Double)
}
