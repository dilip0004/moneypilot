package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistributionRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: DistributionRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<DistributionRuleEntity>)

    @Update
    suspend fun updateRule(rule: DistributionRuleEntity)

    @Delete
    suspend fun deleteRule(rule: DistributionRuleEntity)

    @Query("SELECT * FROM distribution_rules ORDER BY priority ASC")
    fun getAllRules(): Flow<List<DistributionRuleEntity>>

    @Query("SELECT * FROM distribution_rules")
    suspend fun getAllRulesList(): List<DistributionRuleEntity>
}
