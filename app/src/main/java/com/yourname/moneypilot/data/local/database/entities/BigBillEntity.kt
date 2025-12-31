package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

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
    val isPaid: Boolean = false,
    val notes: String = "",
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
