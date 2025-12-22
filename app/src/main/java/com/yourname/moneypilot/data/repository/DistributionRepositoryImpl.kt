package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.DistributionRuleDao
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DistributionRepositoryImpl @Inject constructor(
    private val distributionRuleDao: DistributionRuleDao
) : DistributionRepository {
    override fun getAllRules(): Flow<List<DistributionRuleEntity>> = distributionRuleDao.getAllRules()

    override suspend fun insertRule(rule: DistributionRuleEntity): Long = distributionRuleDao.insertRule(rule)

    override suspend fun updateRule(rule: DistributionRuleEntity) = distributionRuleDao.updateRule(rule)

    override suspend fun deleteRule(rule: DistributionRuleEntity) = distributionRuleDao.deleteRule(rule)
}
