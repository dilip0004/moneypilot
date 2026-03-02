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
        val transactionsWithDetails = transactionRepository.getAllTransactionsWithDetails().first()
        if (transactionsWithDetails.isEmpty()) return null
        
        val fileName = "MoneyPilot_Export_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        
        file.bufferedWriter().use { out ->
            out.write("ID,DateTime,Note,Amount,Type,WalletFromID,CategoryID\n")
            transactionsWithDetails.forEach { 
                val tx = it.transaction
                out.write("${tx.id},${tx.dateTime},${tx.note ?: ""},${tx.amount},${tx.type},${tx.walletFromId},${tx.categoryId}\n")
            }
        }
        return file
    }
}
