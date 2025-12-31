package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "goals",
    indices = [
        Index("target_date"),
        Index("status")
    ]
)
@Serializable
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "type")
    val type: String, // "WISH_LIST", "BILL", "SAVINGS"

    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,

    @ColumnInfo(name = "current_amount")
    val currentAmount: Double = 0.0,

    @ColumnInfo(name = "target_date")
    @Contextual
    val targetDate: LocalDate,

    @ColumnInfo(name = "priority")
    val priority: Int, // 1-5

    @ColumnInfo(name = "color")
    val color: Int,

    @ColumnInfo(name = "icon")
    val icon: String,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"

    @ColumnInfo(name = "created_at")
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
