package com.yourname.moneypilot.domain.model

import java.time.LocalDateTime

data class Transaction(
    val id: Long,
    val accountId: Long,
    val categoryId: Long?,
    val type: String,
    val amount: Double,
    val description: String,
    val date: LocalDateTime,
    val isRecurring: Boolean,
    val recurringPattern: String?,
    val transferToAccountId: Long?,
    val attachmentPath: String?,
    val note: String?
)
