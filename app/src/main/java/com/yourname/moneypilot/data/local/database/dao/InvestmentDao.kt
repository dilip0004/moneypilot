package com.yourname.moneypilot.data.local.database.dao

import androidx.room.*
import com.yourname.moneypilot.data.local.database.entities.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity): Long

    @Update
    suspend fun updateInvestment(investment: InvestmentEntity)

    @Delete
    suspend fun deleteInvestment(investment: InvestmentEntity)

    @Query("SELECT * FROM investments")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT SUM(quantity * currentPrice) FROM investments")
    suspend fun getTotalPortfolioValue(): Double?
}
