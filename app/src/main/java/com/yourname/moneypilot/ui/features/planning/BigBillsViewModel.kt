package com.yourname.moneypilot.ui.features.planning

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate  // ADDED
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil

data class BigBillsState(
    val unpaidBills: List<BigBillEntity> = emptyList(),
    val paidBills: List<BigBillEntity> = emptyList(),
    val totalPendingAmount: Double = 0.0
)

@HiltViewModel
class BigBillsViewModel @Inject constructor(
    private val bigBillRepository: BigBillRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel<BigBillsState>() {

    init {
        loadBigBills()
    }

    private fun loadBigBills() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            bigBillRepository.getAllBigBills().collectLatest { allBills ->
                val unpaid = allBills.filter { !it.isPaid }
                val paid = allBills.filter { it.isPaid }
                val pendingAmount = unpaid.sumOf { it.amount }

                _uiState.value = ScreenState.Success(
                    BigBillsState(
                        unpaidBills = unpaid,
                        paidBills = paid,
                        totalPendingAmount = pendingAmount
                    )
                )
            }
        }
    }

    fun markAsPaid(bill: BigBillEntity) {
        viewModelScope.launch {
            try {
                bigBillRepository.updateBigBill(bill.copy(isPaid = true, updatedAt = LocalDateTime.now()))

                bill.linkedWalletId?.let { walletId ->
                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            walletFromId = walletId,
                            categoryId = bill.categoryId,
                            type = TransactionType.Expense,
                            amount = bill.amount,
                            note = "Settled Big Bill: ${bill.name}",
                            dateTime = LocalDateTime.now(),
                            transactionSourceType = "BIG_BILL_SETTLEMENT"
                        )
                    )
                }

                if (bill.recurrenceType != BillRecurrence.ONCE) {
                    val nextDueDate = when (bill.recurrenceType) {
                        BillRecurrence.MONTHLY -> bill.dueDate.plusMonths(1)
                        BillRecurrence.QUARTERLY -> bill.dueDate.plusMonths(3)
                        BillRecurrence.ANNUALLY -> bill.dueDate.plusYears(1)
                        else -> bill.dueDate
                    }
                    bigBillRepository.insertBigBill(
                        BigBillEntity(
                            name = bill.name,
                            amount = bill.amount,
                            dueDate = nextDueDate,
                            categoryId = bill.categoryId,
                            linkedWalletId = bill.linkedWalletId,
                            recurrenceType = bill.recurrenceType,
                            isPaid = false,
                            notes = bill.notes
                        )
                    )
                }
            } catch (e: Exception) { }
        }
    }

    fun deleteBill(bill: BigBillEntity) {
        viewModelScope.launch {
            bigBillRepository.deleteBigBill(bill)
        }
    }

    fun createMonthlyTransferForBill(bill: BigBillEntity) {
        viewModelScope.launch {
            if (bill.linkedWalletId == null) return@launch
            val monthsRemaining = ChronoUnit.MONTHS.between(LocalDate.now(), bill.dueDate).coerceAtLeast(1)
            val monthlyAmount = ceil(bill.amount / monthsRemaining)

            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = bill.linkedWalletId,
                categoryId = bill.categoryId,
                type = TransactionType.Expense,
                amount = monthlyAmount,
                note = "Auto-reserve for ${bill.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "AUTO_RESERVE"
            )
            transactionRepository.insertTransaction(transaction)
        }
    }
}