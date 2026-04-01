package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "investments")
@Serializable
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // STOCKS, MUTUAL_FUNDS, CRYPTO, GOLD, REAL_ESTATE, FD
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val linkedWalletId: Long? = null, // (TASK-INVESTMENT-LINK)
    val currency: String = "INR",
    @Contextual
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
