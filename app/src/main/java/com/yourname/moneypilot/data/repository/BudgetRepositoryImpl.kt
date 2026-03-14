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
    private val transactionDao: TransactionDao // Injected for calculating spent amounts
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

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

            // Create a new budget for the next period with the adjusted amount
            val newBudget = budget.copy(
                id = 0, // Auto-generate new primary key
                startDate = to,
                endDate = to.plusMonths(1).minusDays(1),
                amount = budget.amount + leftover, // Carry over the leftover amount
                spentAmount = 0.0 // Reset spent amount for the new period
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
