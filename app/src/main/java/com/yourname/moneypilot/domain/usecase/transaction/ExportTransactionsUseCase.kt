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
            // #73: Added human-readable headers
            out.write("ID,DateTime,Type,Wallet (From),Wallet (To),Category,Subcategory,Amount,Note\n")
            
            transactionsWithDetails.forEach { detail ->
                val tx = detail.transaction
                val row = listOf(
                    tx.id,
                    tx.dateTime.toString(),
                    tx.type.name,
                    detail.walletFrom?.name ?: "N/A",
                    detail.walletTo?.name ?: "N/A",
                    detail.category?.name ?: "Uncategorized",
                    detail.subcategory?.name ?: "",
                    tx.amount.toString(),
                    "\"${(tx.note ?: "").replace("\"", "'")}\"" // Quote note to handle commas
                ).joinToString(",")
                
                out.write("$row\n")
            }
        }
        return file
    }
}
