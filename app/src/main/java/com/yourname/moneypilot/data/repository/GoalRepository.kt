package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import com.yourname.moneypilot.data.local.database.dao.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<GoalEntity>>
    fun getGoalsByStatus(status: String): Flow<List<GoalEntity>>
    suspend fun getGoalById(id: Long): GoalEntity?
    suspend fun insertGoal(goal: GoalEntity): Long
    suspend fun updateGoal(goal: GoalEntity)
    suspend fun deleteGoal(goal: GoalEntity)
    suspend fun incrementCurrentAmount(goalId: Long, amount: Double)
    suspend fun updateCurrentAmount(goalId: Long, amount: Double)
    fun getTransactionsForGoal(goalId: Long): kotlinx.coroutines.flow.Flow<List<TransactionWithCategory>>
}
