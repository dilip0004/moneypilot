package com.yourname.moneypilot.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(
    tableName = "loan_events",
    foreignKeys = [
        ForeignKey(
            entity = LoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loan_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loan_id"]), Index(value = ["event_date"])]
)
@Serializable
data class LoanEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "loan_id")
    val loanId: Long,

    @ColumnInfo(name = "event_type")
    val eventType: String, // RATE_CHANGE, PREPAYMENT, EMI_CHANGE, TENURE_CHANGE, REPAYMENT_POSTED

    @Contextual
    @ColumnInfo(name = "event_date")
    val eventDate: LocalDate,

    // Generic numeric payloads
    val amount: Double? = null, // prepayment amount or payment amount

    // Rate change payload
    val newInterestRate: Double? = null,

    // EMI change payload
    val newMonthlyPayment: Double? = null,

    // Tenure change payload
    val newDurationMonths: Int? = null,

    // Optional note
    val note: String? = null
)
