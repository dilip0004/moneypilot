package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "categories")
@Serializable
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
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
