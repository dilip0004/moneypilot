package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistributionRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: DistributionRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<DistributionRuleEntity>)

    @Delete
    suspend fun delete(rule: DistributionRuleEntity)

    @Query("SELECT * FROM distribution_rules ORDER BY priority ASC")
    fun getAllRulesAsFlow(): Flow<List<DistributionRuleEntity>>

    @Query("SELECT * FROM distribution_rules ORDER BY priority ASC")
    suspend fun getAllRules(): List<DistributionRuleEntity>

    @Query("DELETE FROM distribution_rules")
    suspend fun deleteAll()
}
