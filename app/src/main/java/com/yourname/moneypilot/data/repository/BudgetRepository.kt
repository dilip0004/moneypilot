package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    fun getActiveBudgets(date: LocalDate): Flow<List<BudgetEntity>>
    suspend fun getBudgetById(id: Long): BudgetEntity?
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun updateSpentAmount(budgetId: Long, amount: Double)
}
