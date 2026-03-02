package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "loans")
@Serializable
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lender: String,
    val totalAmount: Double,
    val interestRate: Double,
    @Contextual
    val startDate: LocalDate,
    val durationMonths: Int,
    val currentBalance: Double,
    val monthlyPayment: Double,
    val type: String, // BORROWED, LENT
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val linkedWalletId: Long?, // Linked physical wallet
    @Contextual
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Contextual
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
