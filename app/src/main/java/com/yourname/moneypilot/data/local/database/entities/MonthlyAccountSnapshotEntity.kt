package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Stores an immutable snapshot of an account for a given month.
 * Month format: YYYY-MM (e.g. 2026-01)
 */
@Entity(
    tableName = "monthly_account_snapshots",
    indices = [Index(value = ["account_id", "month"], unique = true)]
)
@Serializable
data class MonthlyAccountSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    // YYYY-MM
    val month: String,

    val openingBalance: Double,
    val closingBalance: Double,
    val incomeTotal: Double,
    val expenseTotal: Double,
    val netChange: Double
)
