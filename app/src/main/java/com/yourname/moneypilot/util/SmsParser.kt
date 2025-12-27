package com.yourname.moneypilot.util

import java.time.LocalDateTime
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double?,
    val type: String,
    val merchant: String?,
    val accountSuffix: String?,
    val date: LocalDateTime = LocalDateTime.now()
)

object SmsParser {
    // Common patterns for Indian Banking SMS
    private val amountPattern = Pattern.compile("(?i)(?:rs|inr|amt)\\.?\\s*([\\d,]+\\.?\\d*)")
    private val accountPattern = Pattern.compile("(?i)(?:a/c|acc|account|card)\\s*(?:no\\.)?\\s*[x*]*(\\d{4})")
    private val merchantPattern = Pattern.compile("(?i)(?:at|to|towards|vpa)\\s+([^.,\\s]+)")
    
    private val expenseKeywords = listOf("debited", "spent", "paid", "transaction", "vpa")
    private val incomeKeywords = listOf("credited", "received", "deposited")

    fun parse(text: String): ParsedTransaction {
        val amountMatch = amountPattern.matcher(text)
        val amount = if (amountMatch.find()) {
            amountMatch.group(1)?.replace(",", "")?.toDoubleOrNull()
        } else null

        val accountMatch = accountPattern.matcher(text)
        val accountSuffix = if (accountMatch.find()) accountMatch.group(1) else null

        val merchantMatch = merchantPattern.matcher(text)
        val merchant = if (merchantMatch.find()) merchantMatch.group(1) else null

        val lowerText = text.lowercase()
        val type = when {
            incomeKeywords.any { lowerText.contains(it) } -> "INCOME"
            else -> "EXPENSE" // Default to expense
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            accountSuffix = accountSuffix
        )
    }
}
