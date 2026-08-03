package com.yourname.moneypilot.data.repository

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

class BankImportRepositoryImpl @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionDao: TransactionDao
) : BankImportRepository {

    override suspend fun parseCsv(inputStream: InputStream, mapping: CsvColumnMapping): List<ImportedTransaction> = withContext(Dispatchers.IO) {
        try {
            val lines = CsvParser.parse(inputStream)
            convertToTransactions(lines, mapping)
        } catch (e: Throwable) {
            emptyList()
        }
    }

    override suspend fun convertToTransactions(lines: List<List<String>>, mapping: CsvColumnMapping): List<ImportedTransaction> = withContext(Dispatchers.Default) {
        val transactions = mutableListOf<ImportedTransaction>()
        
        val startIndex = if (mapping.hasHeader) 1 else 0
        
        // Use a limited history or optimized suggestion to avoid OOM
        val existingTxs = try {
            transactionDao.getRecentTransactions(1000) 
        } catch (e: Exception) {
            emptyList()
        }
        
        // Pre-process history to make category suggestion faster
        val historySnippets = existingTxs.filter { it.categoryId != null && !it.note.isNullOrBlank() }
            .map { it.categoryId!! to (it.note?.trim()?.take(8)?.lowercase() ?: "") }
            .filter { it.second.length >= 3 } // Only use snippets of at least 3 chars

        for (i in startIndex until lines.size) {
            val line = lines[i]
            if (line.size <= mapping.dateIndex || line.size <= mapping.descriptionIndex || line.size <= mapping.amountIndex) continue

            val rawDate = line[mapping.dateIndex]
            val rawDesc = line[mapping.descriptionIndex]
            val rawAmount = line[mapping.amountIndex]

            val parsedDate = tryParseDate(rawDate)
            val parsedAmt = rawAmount.replace(",", "").replace("₹", "").trim().toDoubleOrNull()
            
            val suggestedCategoryId = if (parsedAmt != null) {
                findSuggestedCategoryOptimized(rawDesc, historySnippets)
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

    private fun findSuggestedCategoryOptimized(description: String, historySnippets: List<Pair<Long, String>>): Long? {
        if (description.isBlank()) return null
        val descLower = description.lowercase().trim()
        if (descLower.length < 3) return null
        
        val matchingCategories = historySnippets.filter { (_, snippet) ->
            descLower.contains(snippet) || (descLower.length >= 5 && snippet.contains(descLower.take(5)))
        }.map { it.first }

        return matchingCategories
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key
    }

    override suspend fun detectDuplicates(transactions: List<ImportedTransaction>): List<ImportedTransaction> = withContext(Dispatchers.IO) {
        try {
            // Only load recent transactions for duplicate detection to save memory
            val existing = transactionDao.getRecentTransactions(2000)
            val groupedExisting = existing.groupBy { it.dateTime.toLocalDate() }
            
            transactions.map { imported ->
                val importedDate = imported.parsedDateTime?.toLocalDate()
                val potentialDupes = if (importedDate != null) groupedExisting[importedDate] ?: emptyList() else emptyList()
                
                val isDuplicate = potentialDupes.any { exist ->
                    val amtDiff = Math.abs(exist.amount - Math.abs(imported.parsedAmount ?: 0.0))
                    val descMatch = if (imported.rawDescription.length >= 5) {
                        exist.note?.contains(imported.rawDescription.take(5), ignoreCase = true) == true
                    } else {
                        exist.note?.equals(imported.rawDescription, ignoreCase = true) == true
                    }
                    amtDiff < 0.01 && descMatch
                }
                imported.copy(isDuplicate = isDuplicate, isSelected = !isDuplicate)
            }
        } catch (e: Throwable) {
            transactions
        }
    }

    override suspend fun commitImports(transactions: List<ImportedTransaction>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val toSave = transactions.filter { it.isSelected && it.parsedDateTime != null && it.parsedAmount != null && it.walletId != null }
            
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
                transactionRepository.insertTransaction(entity)
            }
            Result.success(toSave.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryParseDate(raw: String): LocalDateTime? {
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "MM/dd/yyyy",
            "dd/MM/yy", "dd-MM-yy", "yyyy/MM/dd", "dd MMM yyyy", "MMM dd, yyyy",
            "dd/MM/yyyy HH:mm", "dd-MM-yyyy HH:mm", "yyyy-MM-dd HH:mm"
        )
        for (fmt in formats) {
            try {
                if (raw.contains(":")) {
                    val pattern = if (raw.count { it == ':' } == 2) "$fmt HH:mm:ss" else "$fmt HH:mm"
                    return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern(pattern))
                }
                return java.time.LocalDate.parse(raw, DateTimeFormatter.ofPattern(fmt)).atStartOfDay()
            } catch (e: Exception) {
                // continue to next format
            }
        }
        return null
    }
}
