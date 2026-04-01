package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.dao.InvestmentDao
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao
) : InvestmentRepository {
    override fun getAllInvestments(): Flow<List<InvestmentEntity>> = investmentDao.getAllInvestments()

    override suspend fun insertInvestment(investment: InvestmentEntity): Long = investmentDao.insertInvestment(investment)

    override suspend fun updateInvestment(investment: InvestmentEntity) = investmentDao.updateInvestment(investment)

    override suspend fun deleteInvestment(investment: InvestmentEntity) = investmentDao.deleteInvestment(investment)

    override suspend fun getInvestmentById(id: Long): InvestmentEntity? = investmentDao.getInvestmentById(id)

    override suspend fun getTotalPortfolioValue(): Double = investmentDao.getTotalPortfolioValue() ?: 0.0
}
