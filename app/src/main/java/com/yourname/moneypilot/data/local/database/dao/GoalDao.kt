package com.yourname.moneypilot.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourname.moneypilot.data.local.database.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): GoalEntity?

    @Query("SELECT * FROM goals ORDER BY target_date ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = :status ORDER BY target_date ASC")
    fun getGoalsByStatus(status: String): Flow<List<GoalEntity>>

    @Query("UPDATE goals SET current_amount = :amount WHERE id = :goalId")
    suspend fun updateCurrentAmount(goalId: Long, amount: Double)
}
