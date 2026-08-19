package com.yourname.moneypilot.ui.features.planning

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import com.yourname.moneypilot.data.repository.WalletRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil

data class BigBillsState(
    val unpaidBills: List<BigBillEntity> = emptyList(),
    val paidBills: List<BigBillEntity> = emptyList(),
    val wallets: List<WalletEntity> = emptyList(),
    val totalPendingAmount: Double = 0.0
)

@HiltViewModel
class BigBillsViewModel @Inject constructor(
    private val bigBillRepository: BigBillRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) : BaseViewModel<BigBillsState>() {

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            combine(
                bigBillRepository.getAllBigBills(),
                walletRepository.getAllWallets()
            ) { allBills, wallets ->
                val unpaid = allBills.filter { !it.isPaid }
                val paid = allBills.filter { it.isPaid }
                val pendingAmount = unpaid.sumOf { it.amount }

                BigBillsState(
                    unpaidBills = unpaid,
                    paidBills = paid,
                    wallets = wallets,
                    totalPendingAmount = pendingAmount
                )
            }.collectLatest { state ->
                if (state.unpaidBills.isEmpty() && state.paidBills.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    _uiState.value = ScreenState.Success(state)
                }
            }
        }
    }

    fun markAsPaid(bill: BigBillEntity) {
        viewModelScope.launch {
            try {
                bigBillRepository.updateBigBill(bill.copy(isPaid = true, updatedAt = LocalDateTime.now()))

                bill.reserveWalletId?.let { walletId ->
                    transactionRepository.insertTransaction(
                        TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            walletFromId = walletId,
                            categoryId = bill.categoryId,
                            type = TransactionType.Expense,
                            amount = bill.amount,
                            note = "Paid Planned Expense: ${bill.name}",
                            dateTime = LocalDateTime.now(),
                            transactionSourceType = "PLANNED_EXPENSE_SETTLEMENT"
                        )
                    )
                }

                if (bill.recurrenceType != BillRecurrence.ONCE) {
                    val nextDueDate = when (bill.recurrenceType) {
                        BillRecurrence.MONTHLY -> bill.dueDate.plusMonths(1)
                        BillRecurrence.QUARTERLY -> bill.dueDate.plusMonths(3)
                        BillRecurrence.HALF_YEARLY -> bill.dueDate.plusMonths(6)
                        BillRecurrence.ANNUALLY -> bill.dueDate.plusYears(1)
                        else -> bill.dueDate
                    }
                    bigBillRepository.insertBigBill(
                        BigBillEntity(
                            name = bill.name,
                            amount = bill.amount,
                            dueDate = nextDueDate,
                            categoryId = bill.categoryId,
                            reserveWalletId = bill.reserveWalletId,
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

    fun recordReserveTransfer(bill: BigBillEntity, sourceWalletId: Long, amount: Double) {
        viewModelScope.launch {
            val targetWalletId = bill.reserveWalletId ?: return@launch
            
            val transaction = TransactionEntity(
                id = UUID.randomUUID().toString(),
                walletFromId = sourceWalletId,
                walletToId = targetWalletId,
                type = TransactionType.Transfer,
                amount = amount,
                note = "Reserve for ${bill.name}",
                dateTime = LocalDateTime.now(),
                transactionSourceType = "PLANNED_EXPENSE_RESERVE"
            )
            transactionRepository.createTransfer(transaction)
            
            // Update local reserved amount tracking
            bigBillRepository.updateBigBill(bill.copy(
                reservedAmount = bill.reservedAmount + amount,
                updatedAt = LocalDateTime.now()
            ))
        }
    }
}