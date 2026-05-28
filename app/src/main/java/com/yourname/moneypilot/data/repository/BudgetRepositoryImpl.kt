package com.yourname.moneypilot.data.repository

import androidx.room.Transaction
import com.yourname.moneypilot.data.local.database.dao.BudgetDao
import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    override suspend fun getAllBudgetsList(): List<BudgetEntity> = budgetDao.getAllBudgetsList()

    override fun getActiveBudgets(date: LocalDate): Flow<List<BudgetEntity>> = budgetDao.getActiveBudgets(date)

    override fun getActiveBudgetsWithDetails(date: LocalDate): Flow<List<BudgetWithDetails>> = 
        budgetDao.getActiveBudgetsWithDetails(date)

    override suspend fun getBudgetById(id: Long): BudgetEntity? = budgetDao.getBudgetById(id)

    override suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insert(budget)

    override suspend fun updateBudget(budget: BudgetEntity) = budgetDao.update(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.delete(budget)

    override suspend fun updateSpentAmount(budgetId: Long, amount: Double) = budgetDao.updateSpentAmount(budgetId, amount)

    @Transaction
    override suspend fun rolloverBudgets(from: LocalDate, to: LocalDate) {
        val activeBudgets = budgetDao.getActiveBudgets(from).first()
        val rolloverBudgets = activeBudgets.filter { it.rolloverEnabled }

        for (budget in rolloverBudgets) {
            val spent = transactionDao.getCategoryExpenseSum(budget.categoryId, budget.startDate.atStartOfDay(), budget.endDate.atTime(LocalTime.MAX)) ?: 0.0
            val leftover = budget.amount - spent

            val newBudget = budget.copy(
                id = 0,
                startDate = to,
                endDate = to.plusMonths(1).minusDays(1),
                amount = budget.amount + leftover,
                spentAmount = 0.0
            )
            budgetDao.insert(newBudget)
        }
    }

    @Transaction
    override suspend fun refreshActiveBudgets(date: LocalDate) {
        val activeBudgets = budgetDao.getActiveBudgets(date).first()
        for (budget in activeBudgets) {
            val start = budget.startDate.atStartOfDay()
            val end = budget.endDate.atTime(LocalTime.MAX)
            
            val actualSpent = if (budget.subcategoryId != null) {
                transactionDao.getSubcategoryExpenseSum(budget.subcategoryId, start, end) ?: 0.0
            } else {
                transactionDao.getCategoryExpenseSum(budget.categoryId, start, end) ?: 0.0
            }
            
            if (budget.spentAmount != actualSpent) {
                budgetDao.updateSpentAmount(budget.id, actualSpent)
            }
        }
    }
}
