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
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
    val subcategories: List<SubcategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val loans: List<LoanEntity>,
    val loanEvents: List<LoanEventEntity> = emptyList(), // #69
    val investments: List<InvestmentEntity>,
    val distributionRules: List<DistributionRuleEntity>,
    val bigBills: List<BigBillEntity>,
    val tags: List<TagEntity> = emptyList(), // #70
    val tagCrossRefs: List<TransactionTagCrossRef> = emptyList() // #70
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
            version = 16, // #71: Match current MoneyPilotDatabase version
            wallets = database.walletDao().getAllWalletsList(),
            categories = database.categoryDao().getAllCategoriesList(),
            subcategories = database.categoryDao().getAllSubcategoriesList(),
            transactions = database.transactionDao().getAllTransactionsForBackup(),
            budgets = database.budgetDao().getAllBudgetsList(),
            goals = database.goalDao().getAllGoalsList(),
            loans = database.loanDao().getAllLoansList(),
            loanEvents = database.loanEventDao().getAllLoanEvents(),
            investments = database.investmentDao().getAllInvestmentsList(),
            distributionRules = database.distributionRuleDao().getAllRules(),
            bigBills = database.bigBillDao().getAllBigBillsList(),
            tags = database.tagDao().getAllTagsList(),
            tagCrossRefs = database.tagDao().getAllCrossRefs()
        )
        return json.encodeToString(backup)
    }

    suspend fun restoreFromJson(uri: Uri): Result<Unit> {
        return try {
            val content = readUriContent(uri)
            val backup = json.decodeFromString<MoneyPilotBackup>(content)

            database.withTransaction {
                clearAllData()

                database.walletDao().insertAll(backup.wallets)
                database.categoryDao().insertAllCategories(backup.categories)
                database.categoryDao().insertAllSubcategories(backup.subcategories)
                database.transactionDao().insertAll(backup.transactions)
                database.budgetDao().insertAll(backup.budgets)
                database.goalDao().insertAll(backup.goals)
                database.loanDao().insertAll(backup.loans)
                database.loanEventDao().insertAll(backup.loanEvents)
                database.investmentDao().insertAll(backup.investments)
                database.distributionRuleDao().insertAll(backup.distributionRules)
                database.bigBillDao().insertAll(backup.bigBills)
                database.tagDao().insertAllTags(backup.tags)
                database.tagDao().insertAllCrossRefs(backup.tagCrossRefs)
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

    private suspend fun clearAllData() {
        database.walletDao().deleteAll()
        database.categoryDao().deleteAll()
        database.transactionDao().deleteAll()
        database.budgetDao().deleteAll()
        database.goalDao().deleteAll()
        database.loanDao().deleteAll()
        database.loanEventDao().deleteAll()
        database.investmentDao().deleteAll()
        database.distributionRuleDao().deleteAll()
        database.bigBillDao().deleteAll()
        database.tagDao().deleteAllTags()
        database.tagDao().deleteAllCrossRefs()
    }
}
