package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import kotlinx.coroutines.flow.Flow

interface DistributionRuleRepository {
    fun getAllRules(): Flow<List<DistributionRuleEntity>>
    suspend fun insertRule(rule: DistributionRuleEntity)
    suspend fun deleteRule(rule: DistributionRuleEntity)
}
