package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.DistributionRuleDao
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DistributionRepositoryImpl @Inject constructor(
    private val distributionRuleDao: DistributionRuleDao
) : DistributionRepository {

    override fun getAllRules(): Flow<List<DistributionRuleEntity>> = distributionRuleDao.getAllRulesAsFlow()

    override suspend fun insertRule(rule: DistributionRuleEntity): Long {
        return distributionRuleDao.insert(rule)
    }

    override suspend fun updateRule(rule: DistributionRuleEntity) {
        distributionRuleDao.insert(rule) // Uses OnConflictStrategy.REPLACE
    }

    override suspend fun deleteRule(rule: DistributionRuleEntity) {
        distributionRuleDao.delete(rule)
    }
}
