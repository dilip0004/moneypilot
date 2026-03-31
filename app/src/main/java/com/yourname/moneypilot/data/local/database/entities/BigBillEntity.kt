package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

enum class BillRecurrence {
    ONCE, MONTHLY, QUARTERLY, ANNUALLY
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
    val linkedWalletId: Long? = null,
    val recurrenceType: BillRecurrence = BillRecurrence.ONCE,
    val autoReserveFlag: Boolean = false, // (TASK-47)
    val reminderDaysBefore: Int = 3, // (TASK-48)
    val isPaid: Boolean = false,
    val notes: String = "",
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
