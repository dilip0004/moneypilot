package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.yourname.moneypilot.data.local.database.util.LocalDateTimeSerializer
import java.time.LocalDateTime

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategory_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("account_id"),
        Index("category_id"),
        Index("subcategory_id"),
        Index("goal_id"),
        Index("date"),
        Index("type")
    ]
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "subcategory_id")
    val subcategoryId: Long? = null,

    @ColumnInfo(name = "goal_id")
    val goalId: Long? = null,

    @ColumnInfo(name = "type")
    val type: String, // "INCOME", "EXPENSE", "TRANSFER", "GOAL_CONTRIBUTION"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "description")
    val description: String,

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "date")
    val date: LocalDateTime,

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurring_pattern")
    val recurringPattern: String? = null, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"

    @ColumnInfo(name = "transfer_to_account_id")
    val transferToAccountId: Long? = null,

    @ColumnInfo(name = "attachment_path")
    val attachmentPath: String? = null,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
