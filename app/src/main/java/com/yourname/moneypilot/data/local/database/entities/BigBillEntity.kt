package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

enum class BillRecurrence {
    ONCE, MONTHLY, QUARTERLY, HALF_YEARLY, ANNUALLY
}

@Entity(tableName = "big_bills")
@Serializable
data class BigBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    @Contextual
    val dueDate: LocalDate,
    val categoryId: Long?,
    val subcategoryId: Long? = null,
    val reserveWalletId: Long? = null, // Where money is physically kept
    val fundingWalletId: Long? = null, // Preferred source for payment
    val recurrenceType: BillRecurrence = BillRecurrence.ONCE,
    val reminderDaysBefore: Int = 3,
    val isPaid: Boolean = false,
    val notes: String = "",
    val reservedAmount: Double = 0.0,
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
