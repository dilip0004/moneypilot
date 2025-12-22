package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import kotlinx.coroutines.flow.Flow

interface InvestmentRepository {
    fun getAllInvestments(): Flow<List<InvestmentEntity>>
    suspend fun insertInvestment(investment: InvestmentEntity): Long
    suspend fun updateInvestment(investment: InvestmentEntity)
    suspend fun deleteInvestment(investment: InvestmentEntity)
    suspend fun getTotalPortfolioValue(): Double
}
