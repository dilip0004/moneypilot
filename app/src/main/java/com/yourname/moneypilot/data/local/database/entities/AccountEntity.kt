package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String, // "CASH", "BANK", "CREDIT_CARD", "INVESTMENT"

    @ColumnInfo(name = "initial_balance")
    val initialBalance: Double,

    @ColumnInfo(name = "current_balance")
    val currentBalance: Double,

    @ColumnInfo(name = "currency")
    val currency: String = "INR",

    @ColumnInfo(name = "color")
    val color: Int,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
