package com.yourname.moneypilot.domain.usecase.transaction

import android.content.Context
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

class ExportTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): File? {
        val transactions = transactionRepository.getAllTransactions().first()
        if (transactions.isEmpty()) return null
        
        val fileName = "MoneyPilot_Export_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        file.bufferedWriter().use { out ->
            out.write("ID,Date,Description,Amount,Type,AccountID,CategoryID\n")
            transactions.forEach {
                out.write("${it.id},${it.date},${it.description},${it.amount},${it.type},${it.accountId},${it.categoryId}\n")
            }
        }
        return file
    }
}
