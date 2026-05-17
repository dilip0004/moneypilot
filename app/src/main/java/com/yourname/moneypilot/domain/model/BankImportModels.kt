package com.yourname.moneypilot.domain.model

import java.time.LocalDateTime

data class CsvColumnMapping(
    val dateIndex: Int = -1,
    val descriptionIndex: Int = -1,
    val amountIndex: Int = -1,
    val typeIndex: Int = -1, // Optional: if the CSV has an 'In/Out' or 'DR/CR' column
    val hasHeader: Boolean = true
)

data class ImportedTransaction(
    val rawDate: String,
    val rawDescription: String,
    val rawAmount: String,
    val parsedDateTime: LocalDateTime? = null,
    val parsedAmount: Double? = null,
    val isDuplicate: Boolean = false,
    val categoryId: Long? = null,
    val walletId: Long? = null,
    val isSelected: Boolean = true
)
