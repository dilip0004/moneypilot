package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.dao.TransactionDao
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.domain.model.CsvColumnMapping
import com.yourname.moneypilot.domain.model.ImportedTransaction
import com.yourname.moneypilot.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import androidx.room.withTransaction

class BankImportRepositoryImpl @Inject constructor(
    private val database: MoneyPilotDatabase,
    private val transactionRepository: TransactionRepository,
    private val transactionDao: TransactionDao
) : BankImportRepository {

    override suspend fun parseCsv(inputStream: InputStream, mapping: CsvColumnMapping): List<ImportedTransaction> = withContext(Dispatchers.IO) {
        val lines = CsvParser.parse(inputStream)
        val transactions = mutableListOf<ImportedTransaction>()
        
        val startIndex = if (mapping.hasHeader) 1 else 0
        val existingTxs = transactionDao.getAllTransactionsForBackup() 
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.size <= mapping.dateIndex || line.size <= mapping.descriptionIndex || line.size <= mapping.amountIndex) continue

            val rawDate = line[mapping.dateIndex]
            val rawDesc = line[mapping.descriptionIndex]
            val rawAmount = line[mapping.amountIndex]

            val parsedDate = tryParseDate(rawDate)
            val parsedAmt = rawAmount.replace(",", "").toDoubleOrNull()
            
            val suggestedCategoryId = if (parsedAmt != null) {
                findSuggestedCategory(rawDesc, existingTxs)
            } else null

            transactions.add(
                ImportedTransaction(
                    rawDate = rawDate,
                    rawDescription = rawDesc,
                    rawAmount = rawAmount,
                    parsedDateTime = parsedDate,
                    parsedAmount = parsedAmt,
                    categoryId = suggestedCategoryId
                )
            )
        }
        transactions
    }

    private fun findSuggestedCategory(description: String, history: List<TransactionEntity>): Long? {
        if (description.isBlank()) return null
        val relevantTxs = history.filter { 
            it.note?.contains(description.take(8), ignoreCase = true) == true || 
            description.contains(it.note?.take(8) ?: "____", ignoreCase = true)
        }
        
        return relevantTxs
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .maxByOrNull { it.value.size }
            ?.key
    }

    override suspend fun detectDuplicates(transactions: List<ImportedTransaction>): List<ImportedTransaction> = withContext(Dispatchers.IO) {
        val existing = transactionDao.getAllTransactionsForBackup()
        
        transactions.map { imported ->
            val isDuplicate = existing.any { exist ->
                Math.abs(exist.amount - Math.abs(imported.parsedAmount ?: 0.0)) < 0.01 && 
                exist.dateTime.toLocalDate() == imported.parsedDateTime?.toLocalDate() &&
                exist.note?.contains(imported.rawDescription.take(10), ignoreCase = true) == true
            }
            imported.copy(isDuplicate = isDuplicate, isSelected = !isDuplicate)
        }
    }

    /**
     * Section 10.0 Compliance: Concurrency & Atomicity.
     * Section 1.1 Compliance: Source of Truth.
     * 
     * Uses the TransactionRepository to ensure wallet balances are updated 
     * alongside the ledger entries.
     */
    override suspend fun commitImports(transactions: List<ImportedTransaction>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val toSave = transactions.filter { it.isSelected && it.parsedDateTime != null && it.parsedAmount != null && it.walletId != null }
            
            database.withTransaction {
                toSave.forEach { 
                    val isIncome = it.parsedAmount!! >= 0
                    val entity = TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        dateTime = it.parsedDateTime!!,
                        amount = Math.abs(it.parsedAmount!!),
                        type = if (isIncome) TransactionType.Income else TransactionType.Expense,
                        walletFromId = if (!isIncome) it.walletId else null,
                        walletToId = if (isIncome) it.walletId else null,
                        categoryId = it.categoryId,
                        note = it.rawDescription,
                        transactionSourceType = "BANK_IMPORT"
                    )
                    // Use repository to trigger financial impact and budget sync
                    transactionRepository.insertTransaction(entity)
                }
            }
            Result.success(toSave.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryParseDate(raw: String): LocalDateTime? {
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "MM/dd/yyyy",
            "dd/MM/yy", "dd-MM-yy", "yyyy/MM/dd", "dd MMM yyyy", "MMM dd, yyyy"
        )
        for (fmt in formats) {
            try {
                return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("$fmt HH:mm:ss"))
            } catch (e: Exception) {
                try {
                    return java.time.LocalDate.parse(raw, DateTimeFormatter.ofPattern(fmt)).atStartOfDay()
                } catch (e2: Exception) { /* ignore */ }
            }
        }
        return null
    }
}
