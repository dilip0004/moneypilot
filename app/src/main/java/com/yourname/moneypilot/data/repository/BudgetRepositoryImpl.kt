package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.BudgetDao
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    override fun getActiveBudgets(date: LocalDate): Flow<List<BudgetEntity>> = budgetDao.getActiveBudgets(date)

    override suspend fun getBudgetById(id: Long): BudgetEntity? = budgetDao.getBudgetById(id)

    override suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insert(budget)

    override suspend fun updateBudget(budget: BudgetEntity) = budgetDao.update(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.delete(budget)

    override suspend fun updateSpentAmount(budgetId: Long, amount: Double) = budgetDao.updateSpentAmount(budgetId, amount)
}
