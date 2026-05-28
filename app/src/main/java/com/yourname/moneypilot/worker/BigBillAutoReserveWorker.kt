package com.yourname.moneypilot.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.local.preferences.UserPreferencesRepository
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil

@HiltWorker
class BigBillAutoReserveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val bigBillRepository: BigBillRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("BigBillAutoReserveWorker: Starting execution audit")
        return try {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            val today = LocalDate.now()
            val currentMonthStr = YearMonth.from(today).toString()

            if (prefs.lastAutoReserveMonth != currentMonthStr) {
                processAutoReserves(today, currentMonthStr)
                preferencesRepository.updateLastAutoReserveMonth(currentMonthStr)
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BigBillAutoReserveWorker: Execution failed")
            Result.retry()
        }
    }

    private suspend fun processAutoReserves(today: LocalDate, monthStr: String) {
        val unpaidBills = bigBillRepository.getUnpaidBigBills().first()
            .filter { it.autoReserveFlag && it.linkedWalletId != null }

        Timber.i("BigBillAutoReserveWorker: Processing ${unpaidBills.size} flagged bills")

        for (bill in unpaidBills) {
            // Section 16.0: Hardened Idempotency (TASK-40)
            // Deterministic UUID based on Bill ID and Month
            val deterministicId = UUID.nameUUIDFromBytes("RESERVE_${bill.id}_$monthStr".toByteArray()).toString()
            
            // Check if already exists in ledger
            if (transactionRepository.getTransactionById(deterministicId) != null) continue

            // Calculate monthly portion
            val monthsRemaining = ChronoUnit.MONTHS.between(today, bill.dueDate).coerceAtLeast(1)
            val monthlyAmount = ceil(bill.amount / monthsRemaining)

            if (monthlyAmount > 0) {
                val transaction = TransactionEntity(
                    id = deterministicId,
                    walletFromId = bill.linkedWalletId,
                    categoryId = bill.categoryId,
                    type = TransactionType.Expense,
                    amount = monthlyAmount,
                    note = "Auto-reserve: ${bill.name} ($monthStr)",
                    dateTime = LocalDateTime.now(),
                    transactionSourceType = "AUTO_RESERVE"
                )
                
                transactionRepository.insertTransaction(transaction)
                Timber.d("BigBillAutoReserveWorker: Reserved ₹$monthlyAmount for ${bill.name} (ID: $deterministicId)")
            }
        }
    }
}
