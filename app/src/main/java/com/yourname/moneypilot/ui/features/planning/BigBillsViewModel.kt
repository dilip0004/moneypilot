package com.yourname.moneypilot.ui.features.planning

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.BigBillEntity
import com.yourname.moneypilot.data.repository.BigBillRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BigBillsState(
    val unpaidBills: List<BigBillEntity> = emptyList(),
    val paidBills: List<BigBillEntity> = emptyList(),
    val totalPendingAmount: Double = 0.0
)

@HiltViewModel
class BigBillsViewModel @Inject constructor(
    private val bigBillRepository: BigBillRepository
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
            bigBillRepository.updateBigBill(bill.copy(isPaid = true))
        }
    }

    fun deleteBill(bill: BigBillEntity) {
        viewModelScope.launch {
            bigBillRepository.deleteBigBill(bill)
        }
    }
}
