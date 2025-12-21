package com.yourname.moneypilot.domain.model

import java.time.LocalDateTime

data class Account(
    val id: Long,
    val name: String,
    val type: String,
    val initialBalance: Double,
    val currentBalance: Double,
    val currency: String,
    val color: Int,
    val icon: String,
    val isArchived: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
