package com.yourname.moneypilot.domain

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class CreateCreditCardInterestUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(walletId: Long, amount: Double) {
        // Find the specific "Interest" subcategory under "Fees & Charges"
        val feesCategory = categoryRepository.getAllCategories().first().find { it.name == "Fees & Charges" }
        val interestSubcategory = feesCategory?.let { category ->
            categoryRepository.getSubcategories(category.id).first().find { it.name == "Interest" }
        }

        require(feesCategory != null && interestSubcategory != null) { 
            "Default 'Interest' category not found. Please ensure database defaults are seeded."
        }

        val interestTransaction = TransactionEntity(
            id = UUID.randomUUID().toString(),
            dateTime = LocalDateTime.now(),
            amount = amount,
            type = TransactionType.Expense,
            categoryId = feesCategory.id,
            walletFromId = walletId,
            transactionSourceType = "AUTOMATED_INTEREST",
            note = "Credit Card Interest Charge"
        )

        transactionRepository.insertTransaction(interestTransaction)
    }
}
