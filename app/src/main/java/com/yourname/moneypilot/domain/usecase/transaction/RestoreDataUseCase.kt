package com.yourname.moneypilot.domain.usecase.transaction

import android.content.Context
import android.net.Uri
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class RestoreDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MoneyPilotDatabase
) {
    suspend operator fun invoke(fileUri: Uri): Result<Unit> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(fileUri) ?: return Result.failure(Exception("Could not open file"))
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            
            // For V1, assuming we export/import a JSON list of transactions
            // In a full implementation, we'd restore accounts, categories, and tags too.
            val transactions = Json.decodeFromString<List<TransactionEntity>>(jsonString)
            
            database.transactionDao().insertAll(transactions)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
