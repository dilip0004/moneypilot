package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.yourname.moneypilot.data.local.database.util.LocalDateTimeSerializer
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "investments")
@Serializable
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // STOCKS, MUTUAL_FUNDS, CRYPTO, GOLD, REAL_ESTATE, FD, PPF, SIP, RD
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val linkedWalletId: Long? = null,
    val currency: String = "INR",
    @Contextual
    val startDate: LocalDate? = null,
    @ColumnInfo(name = "extra_data")
    val extraData: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
