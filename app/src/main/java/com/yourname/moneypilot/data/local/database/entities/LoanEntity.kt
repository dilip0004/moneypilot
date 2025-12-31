package com.yourname.moneypilot.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lender: String,
    val totalAmount: Double,
    val interestRate: Double,
    val startDate: LocalDate,
    val durationMonths: Int,
    val currentBalance: Double,
    val monthlyPayment: Double,
    val type: String, // BORROWED, LENT
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val accountId: Long?, // Linked physical wallet
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
