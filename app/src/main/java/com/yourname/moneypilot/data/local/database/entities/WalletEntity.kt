package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "wallets") // Table name updated as per spec
@Serializable
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String, // CASH | BANK | CREDIT_CARD | SAVINGS | INVESTMENT_LINKED

    @ColumnInfo(name = "initial_balance")
    val initialBalance: Double,

    @ColumnInfo(name = "current_balance")
    val currentBalance: Double,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "min_balance")
    val minBalance: Double = 0.0,

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "color")
    val color: Int,

    @ColumnInfo(name = "icon")
    val icon: String,
    
    // New fields from v5 Architecture
    @ColumnInfo(name = "credit_limit")
    val creditLimit: Double? = null,

    @ColumnInfo(name = "billing_start_day")
    val billingStartDay: Int? = null,

    @ColumnInfo(name = "billing_end_day")
    val billingEndDay: Int? = null,

    @ColumnInfo(name = "due_date")
    val dueDate: Int? = null,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
