package com.yourname.moneypilot.data.repository

import com.yourname.moneypilot.domain.model.CsvColumnMapping
import com.yourname.moneypilot.domain.model.ImportedTransaction
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface BankImportRepository {
    suspend fun parseCsv(inputStream: InputStream, mapping: CsvColumnMapping): List<ImportedTransaction>
    suspend fun convertToTransactions(lines: List<List<String>>, mapping: CsvColumnMapping): List<ImportedTransaction>
    suspend fun detectDuplicates(transactions: List<ImportedTransaction>): List<ImportedTransaction>
    suspend fun commitImports(transactions: List<ImportedTransaction>): Result<Int>
}
