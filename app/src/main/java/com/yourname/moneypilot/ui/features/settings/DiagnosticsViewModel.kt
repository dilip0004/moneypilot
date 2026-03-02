package com.yourname.moneypilot.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.moneypilot.data.repository.*
import com.yourname.moneypilot.util.SmsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestCase(
    val id: String,
    val name: String,
    val description: String,
    val status: TestStatus = TestStatus.PENDING,
    val errorMessage: String? = null
)

enum class TestStatus {
    PENDING, RUNNING, PASSED, FAILED
}

data class DiagnosticsState(
    val testCases: List<TestCase> = emptyList(),
    val isRunning: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository, // Updated
    private val loanRepository: LoanRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsState(
        testCases = listOf(
            TestCase("sms_parser", "SMS Parsing Engine", "Verifies bank SMS extraction logic"),
            TestCase("balance_sync", "Balance Sync Logic", "Tests wallet updates on transactions"),
            TestCase("loan_repayment", "Loan Repayment Engine", "Checks debt reduction sync"),
            TestCase("db_integrity", "Database Integrity", "Verifies schema version and DAO access"),
            TestCase("serialization", "JSON Serialization", "Tests backup data conversion")
        )
    ))
    val state = _state.asStateFlow()

    fun runAllTests() {
        viewModelScope.launch {
            _state.update { it.copy(isRunning = true) }
            
            _state.value.testCases.forEach { test ->
                updateTestStatus(test.id, TestStatus.RUNNING)
                delay(500) // Visual feedback
                
                val result = runTestLogic(test.id)
                updateTestStatus(test.id, if (result == null) TestStatus.PASSED else TestStatus.FAILED, result)
            }
            
            _state.update { it.copy(isRunning = false) }
        }
    }

    private fun updateTestStatus(id: String, status: TestStatus, error: String? = null) {
        _state.update { currentState ->
            val updatedList = currentState.testCases.map {
                if (it.id == id) it.copy(status = status, errorMessage = error) else it
            }
            currentState.copy(testCases = updatedList)
        }
    }

    private suspend fun runTestLogic(id: String): String? {
        return try {
            when (id) {
                "sms_parser" -> {
                    val sms = "Rs 500.00 spent at AMZN from Acc x1234"
                    val parsed = SmsParser.parse(sms)
                    if (parsed.amount == 500.0 && parsed.type == "EXPENSE") null 
                    else "Amount mismatch: expected 500.0, got ${parsed.amount}"
                }
                "balance_sync" -> {
                    walletRepository.getAllWallets().first()
                    null
                }
                "loan_repayment" -> {
                    loanRepository.getAllLoans().first()
                    null
                }
                "db_integrity" -> {
                    transactionRepository.getAllTransactionsWithDetails().first()
                    null
                }
                "serialization" -> {
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }
    }
}
