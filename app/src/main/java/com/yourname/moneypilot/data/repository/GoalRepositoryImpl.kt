package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.GoalDao
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    override fun getGoalsByStatus(status: String): Flow<List<GoalEntity>> = goalDao.getGoalsByStatus(status)

    override suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    override suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insert(goal)

    override suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    override suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    override suspend fun updateCurrentAmount(goalId: Long, amount: Double) = goalDao.updateCurrentAmount(goalId, amount)
}
