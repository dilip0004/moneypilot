package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // STOCKS, CRYPTO, MUTUAL_FUNDS
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val currency: String = "USD",
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
