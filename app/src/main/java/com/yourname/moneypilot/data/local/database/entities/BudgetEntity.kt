package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategory_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("category_id"),
        Index("subcategory_id"),
        Index("start_date"),
        Index("end_date")
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "subcategory_id")
    val subcategoryId: Long? = null,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "period")
    val period: String, // "MONTHLY", "YEARLY", "CUSTOM"

    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,

    @ColumnInfo(name = "end_date")
    val endDate: LocalDate,

    @ColumnInfo(name = "rollover_enabled")
    val rolloverEnabled: Boolean = false,

    @ColumnInfo(name = "alert_threshold")
    val alertThreshold: Int = 90, // Percentage

    @ColumnInfo(name = "spent_amount")
    val spentAmount: Double = 0.0,

    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
