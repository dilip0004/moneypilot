package com.yourname.moneypilot.ui.features.distribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.local.database.entities.DistributionRuleEntity
import com.yourname.moneypilot.data.local.database.entities.WalletEntity
import com.yourname.moneypilot.data.repository.DistributionRuleRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DistributionViewModel @Inject constructor(
    private val distributionRuleRepository: DistributionRuleRepository,
    private val walletRepository: WalletRepository
) : ViewModel() {

    val rules: StateFlow<List<DistributionRuleEntity>> = distributionRuleRepository.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletRepository.getAllWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(rule: DistributionRuleEntity) {
        viewModelScope.launch {
            distributionRuleRepository.insertRule(rule)
        }
    }

    fun deleteRule(rule: DistributionRuleEntity) {
        viewModelScope.launch {
            distributionRuleRepository.deleteRule(rule)
        }
    }
}
