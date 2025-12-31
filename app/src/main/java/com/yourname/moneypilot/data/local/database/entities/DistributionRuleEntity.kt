package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "distribution_rules")
@Serializable
data class DistributionRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceWalletId: Long,
    val targetWalletId: Long,
    val percentage: Double? = null, // e.g., 20.0 for 20%
    val fixedAmount: Double? = null,
    val priority: Int = 0
)
