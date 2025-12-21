package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String, // "INCOME", "EXPENSE"

    @ColumnInfo(name = "parent_id")
    val parentId: Long?,

    @ColumnInfo(name = "color")
    val color: Int,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "budget_limit")
    val budgetLimit: Double?,

    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
