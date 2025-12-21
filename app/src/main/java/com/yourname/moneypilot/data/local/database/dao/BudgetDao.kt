package com.yourname.moneypilot.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY start_date DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId ORDER BY start_date DESC")
    fun getBudgetsByCategory(categoryId: Long): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE start_date <= :date AND end_date >= :date")
    fun getActiveBudgets(date: java.time.LocalDate): Flow<List<BudgetEntity>>

    @Query("UPDATE budgets SET spent_amount = :spentAmount WHERE id = :budgetId")
    suspend fun updateSpentAmount(budgetId: Long, spentAmount: Double)
}
