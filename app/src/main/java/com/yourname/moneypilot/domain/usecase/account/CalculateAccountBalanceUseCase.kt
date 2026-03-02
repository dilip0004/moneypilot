package com.yourname.moneypilot.domain.usecase.account

import com.yourname.moneypilot.data.repository.WalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalculateAccountBalanceUseCase @Inject constructor(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Double {
        return walletRepository.getAllWallets().first().sumOf { it.currentBalance }
    }
}
