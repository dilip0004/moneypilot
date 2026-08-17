package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.BudgetDao
import com.yourname.moneypilot.data.local.database.dao.BudgetWithDetails
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.BudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    override suspend fun getAllBudgetsList(): List<BudgetEntity> = budgetDao.getAllBudgetsList()

    override fun getActiveBudgets(date: LocalDate): Flow<List<BudgetEntity>> = budgetDao.getActiveBudgets(date)

    override fun getActiveBudgetsWithDetails(date: LocalDate): Flow<List<BudgetWithDetails>> {
        val monthStart = date.withDayOfMonth(1)
        
        return budgetDao.getActiveBudgetsWithDetails(date).map { list ->
            // Resolution Logic: override -> recurring -> one-time
            val grouped = list.groupBy { it.budget.categoryId to it.budget.subcategoryId }
            
            grouped.map { (_, budgets) ->
                val override = budgets.find { it.budget.parentBudgetId != null && it.budget.startDate == monthStart }
                val recurring = budgets.find { it.budget.isRecurring }
                val oneTime = budgets.find { !it.budget.isRecurring && it.budget.parentBudgetId == null }
                
                override ?: recurring ?: oneTime!!
            }.sortedByDescending { it.budget.amount }
        }
    }

    override suspend fun getBudgetById(id: Long): BudgetEntity? = budgetDao.getBudgetById(id)

    override suspend fun insertBudget(budget: BudgetEntity): Long = budgetDao.insert(budget)

    override suspend fun updateBudget(budget: BudgetEntity) = budgetDao.update(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.delete(budget)

    override suspend fun updateSpentAmount(budgetId: Long, amount: Double) = budgetDao.updateSpentAmount(budgetId, amount)

    override suspend fun rolloverBudgets(from: LocalDate, to: LocalDate) {
        // ... implementation from original file if needed, but the user wants to avoid duplicate records
    }

    override suspend fun refreshActiveBudgets(date: LocalDate) {
        // Handled dynamically now
    }
}
