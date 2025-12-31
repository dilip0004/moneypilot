package com.yourname.moneypilot.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.yourname.moneypilot.data.local.database.MoneyPilotDatabase
import com.yourname.moneypilot.data.local.database.converters.LocalDateSerializer
import com.yourname.moneypilot.data.local.database.converters.LocalDateTimeSerializer
import com.yourname.moneypilot.data.local.database.entities.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@Serializable
data class MoneyPilotBackup(
    val version: Int,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val subcategories: List<SubcategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val loans: List<LoanEntity>,
    val investments: List<InvestmentEntity>,
    val distributionRules: List<DistributionRuleEntity>,
    val bigBills: List<BigBillEntity>
)

@OptIn(ExperimentalSerializationApi::class)
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MoneyPilotDatabase
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(LocalDateSerializer)
            contextual(LocalDateTimeSerializer)
        }
    }

    suspend fun createJsonBackup(): String {
        val backup = MoneyPilotBackup(
            version = 7,
            accounts = database.accountDao().getAllAccountsList(),
            categories = database.categoryDao().getAllCategoriesList(),
            subcategories = database.categoryDao().getAllSubcategoriesList(),
            transactions = database.transactionDao().getAllTransactionsList(),
            budgets = database.budgetDao().getAllBudgetsList(),
            goals = database.goalDao().getAllGoalsList(),
            loans = database.loanDao().getAllLoansList(),
            investments = database.investmentDao().getAllInvestmentsList(),
            distributionRules = database.distributionRuleDao().getAllRulesList(),
            bigBills = database.bigBillDao().getAllBigBillsList()
        )
        return json.encodeToString(backup)
    }

    suspend fun restoreFromJson(uri: Uri): Result<Unit> {
        return try {
            val content = readUriContent(uri)
            val backup = json.decodeFromString<MoneyPilotBackup>(content)
            
            database.withTransaction {
                // Wipe existing data safely
                clearAllData()
                
                // Restore in correct order to respect dependencies
                database.accountDao().insertAll(backup.accounts)
                database.categoryDao().insertAllCategories(backup.categories)
                database.categoryDao().insertAllSubcategories(backup.subcategories)
                database.transactionDao().insertAll(backup.transactions)
                database.budgetDao().insertAll(backup.budgets)
                database.goalDao().insertAll(backup.goals)
                database.loanDao().insertAll(backup.loans)
                database.investmentDao().insertAll(backup.investments)
                database.distributionRuleDao().insertAll(backup.distributionRules)
                database.bigBillDao().insertAll(backup.bigBills)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readUriContent(uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun clearAllData() {
        database.query("DELETE FROM accounts", null).close()
        database.query("DELETE FROM categories", null).close()
        database.query("DELETE FROM subcategories", null).close()
        database.query("DELETE FROM transactions", null).close()
        database.query("DELETE FROM budgets", null).close()
        database.query("DELETE FROM goals", null).close()
        database.query("DELETE FROM loans", null).close()
        database.query("DELETE FROM investments", null).close()
        database.query("DELETE FROM distribution_rules", null).close()
        database.query("DELETE FROM big_bills", null).close()
    }
}
