package com.yourname.moneypilot.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Budget(
    val id: Long,
    val categoryId: Long,
    val amount: Double,
    val period: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val rolloverEnabled: Boolean,
    val alertThreshold: Int,
    val spentAmount: Double,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
