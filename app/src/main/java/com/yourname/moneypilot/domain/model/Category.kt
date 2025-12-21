package com.yourname.moneypilot.domain.model

import java.time.LocalDateTime

data class Category(
    val id: Long,
    val name: String,
    val type: String,
    val parentId: Long?,
    val color: Int,
    val icon: String,
    val budgetLimit: Double?,
    val isSystem: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
