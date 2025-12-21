package com.yourname.moneypilot.domain.usecase.account

import com.yourname.moneypilot.data.repository.AccountRepository
import javax.inject.Inject

class CalculateAccountBalanceUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): Double {
        return accountRepository.getTotalBalance()
    }
}
