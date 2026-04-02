package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.GoalDao
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.dao.TransactionWithDetails
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val transactionDao: TransactionDao
) : GoalRepository {
    override fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    override fun getGoalsByStatus(status: String): Flow<List<GoalEntity>> = goalDao.getGoalsByStatus(status)

    override suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    override suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insert(goal)

    override suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    override suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    // Ghost Write Loophole Fix: Removed incrementCurrentAmount and updateCurrentAmount.
    // Progress must now be driven strictly through the Transaction Ledger via GoalId linking.

    override fun getTransactionsForGoal(goalId: Long): Flow<List<TransactionWithDetails>> {
        return transactionDao.getAllTransactionsWithDetails().map { allTxs ->
            allTxs.filter { it.transaction.goalId == goalId } 
        }
    }
}
