package com.yourname.moneypilot.ui.features.accounts

import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.AccountEntity
import com.yourname.moneypilot.data.local.database.entities.MonthlyAccountSnapshotEntity
import com.yourname.moneypilot.data.repository.AccountRepository
import com.yourname.moneypilot.data.repository.MonthlySnapshotRepository
import com.yourname.moneypilot.ui.common.BaseViewModel
import com.yourname.moneypilot.ui.common.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

enum class AccountsPeriod { THIS_MONTH, LAST_MONTH }

data class AccountsState(
    val accounts: List<AccountEntity> = emptyList(),
    val period: AccountsPeriod = AccountsPeriod.THIS_MONTH,
    val lastMonthSnapshots: Map<Long, MonthlyAccountSnapshotEntity> = emptyMap()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val snapshotRepository: MonthlySnapshotRepository
) : BaseViewModel<AccountsState>() {

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collectLatest { list ->
                if (list.isEmpty()) {
                    _uiState.value = ScreenState.Empty
                } else {
                    // Try loading last month snapshots (if any)
                    val prev = YearMonth.now().minusMonths(1)
                    val key = "%04d-%02d".format(prev.year, prev.monthValue)
                    val snapshotMap = mutableMapOf<Long, MonthlyAccountSnapshotEntity>()
                    // We'll pull snapshots per account lazily via flows (first value).
                    for (acc in list) {
                        val snaps = snapshotRepository.getSnapshotsForAccount(acc.id).first()
                        val match = snaps.firstOrNull { it.month == key }
                        if (match != null) snapshotMap[acc.id] = match
                    }
                    _uiState.value = ScreenState.Success(AccountsState(list, AccountsPeriod.THIS_MONTH, snapshotMap))
                }
            }
        }
    }

    fun setPeriod(period: AccountsPeriod) {
        val current = (_uiState.value as? ScreenState.Success)?.data ?: return
        _uiState.value = ScreenState.Success(current.copy(period = period))
    }

    fun archiveAccount(account: AccountEntity) {
        viewModelScope.launch {
            accountRepository.updateAccount(account.copy(isArchived = true))
        }
    }
}
