package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.yourname.moneypilot.data.local.database.util.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

enum class TransactionType {
    Income,
    Expense,
    Transfer
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_from_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_to_id"],
            onDelete = ForeignKey.SET_NULL
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
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loan_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = InvestmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["investment_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("wallet_from_id"),
        Index("wallet_to_id"),
        Index("category_id"),
        Index("subcategory_id"),
        Index("loan_id"),
        Index("goal_id"),
        Index("investment_id"),
        Index("dateTime"),
        Index("type")
    ]
)
@Serializable
data class TransactionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "dateTime")
    val dateTime: LocalDateTime,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "type")
    val type: TransactionType,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "subcategory_id")
    val subcategoryId: Long? = null,

    @ColumnInfo(name = "loan_id")
    val loanId: Long? = null,

    @ColumnInfo(name = "goal_id")
    val goalId: Long? = null,

    @ColumnInfo(name = "investment_id")
    val investmentId: Long? = null,

    @ColumnInfo(name = "wallet_from_id")
    val walletFromId: Long? = null,

    @ColumnInfo(name = "wallet_to_id")
    val walletToId: Long? = null,

    @ColumnInfo(name = "transaction_source_type")
    val transactionSourceType: String,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "is_refund", defaultValue = "0")
    val isRefund: Boolean = false,

    @ColumnInfo(name = "soft_deleted", defaultValue = "0")
    val softDeleted: Boolean = false,

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
