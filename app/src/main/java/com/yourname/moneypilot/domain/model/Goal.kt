package com.yourname.moneypilot.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Goal(
    val id: Long,
    val name: String,
    val description: String?,
    val type: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: LocalDate,
    val priority: Int,
    val color: Int,
    val icon: String,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
