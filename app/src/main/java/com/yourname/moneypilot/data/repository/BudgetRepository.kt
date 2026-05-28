package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    suspend fun getAllBudgetsList(): List<BudgetEntity>
    fun getActiveBudgets(date: LocalDate): Flow<List<BudgetEntity>>
    fun getActiveBudgetsWithDetails(date: LocalDate): Flow<List<BudgetWithDetails>>
    suspend fun getBudgetById(id: Long): BudgetEntity?
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun updateSpentAmount(budgetId: Long, spentAmount: Double)
    suspend fun rolloverBudgets(from: LocalDate, to: LocalDate)
    suspend fun refreshActiveBudgets(date: LocalDate)
}
