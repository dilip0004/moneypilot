package com.yourname.moneypilot.api

import com.yourname.moneypilot.data.local.database.entities.*
import com.yourname.moneypilot.data.repository.*
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import java.io.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Singleton
class MoneyPilotApi @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val investmentRepository: InvestmentRepository,
    private val loanRepository: LoanRepository,
    private val bigBillRepository: BigBillRepository,
    private val backupRepository: BackupRepository
) {
    private val apiScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sessions = Collections.newSetFromMap(ConcurrentHashMap<DefaultWebSocketServerSession, Boolean>())

    init {
        observeChanges()
    }

    private fun observeChanges() {
        // Broadcast updates whenever transactions or wallets change
        apiScope.launch {
            merge(
                transactionRepository.getAllTransactionsWithDetails(),
                walletRepository.getAllWallets(),
                budgetRepository.getAllBudgets(),
                goalRepository.getAllGoals(),
                investmentRepository.getAllInvestments(),
                loanRepository.getAllLoans()
            ).collect {
                broadcast("DATA_UPDATED")
            }
        }
    }

    private suspend fun broadcast(message: String) {
        sessions.forEach { session ->
            try {
                if (session.isActive) {
                    session.send(Frame.Text(message))
                }
            } catch (e: Exception) {
                sessions.remove(session)
            }
        }
    }

    fun Application.module() {
        install(ContentNegotiation) {
            json()
        }
        install(WebSockets) {
            pingPeriod = java.time.Duration.ofSeconds(15)
            timeout = java.time.Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Delete)
        }
        install(CallLogging)
        
        routing {
            get("/") {
                try {
                    val html = this@MoneyPilotApi.context.assets.open("webapp/index.html").bufferedReader().use { it.readText() }
                    call.respondText(html, ContentType.Text.Html)
                } catch (e: Exception) {
                    call.respondText("Error loading index.html: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }

            webSocket("/ws") {
                sessions.add(this)
                try {
                    for (frame in incoming) {
                        // Keep alive or handle client messages
                    }
                } finally {
                    sessions.remove(this)
                }
            }
            
            route("/api") {
                get("/status") {
                    call.respond(mapOf("status" to "online", "version" to "1.1"))
                }
                
                route("/wallets") {
                    get {
                        call.respond(walletRepository.getAllWallets().first())
                    }
                    post {
                        val req = call.receive<Map<String, String>>()
                        val name = req["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val type = req["type"] ?: "CASH"
                        val balance = req["balance"]?.toDoubleOrNull() ?: 0.0
                        val icon = req["icon"] ?: "💰"
                        
                        val wallet = WalletEntity(
                            name = name,
                            type = type,
                            initialBalance = balance,
                            currentBalance = balance,
                            icon = icon,
                            color = 0
                        )
                        walletRepository.insertWallet(wallet)
                        call.respond(HttpStatusCode.Created)
                    }
                    delete("/{id}") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val wallet = walletRepository.getWalletById(id)
                        if (wallet != null) {
                            walletRepository.deleteWallet(wallet)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
                
                route("/categories") {
                    get {
                        call.respond(categoryRepository.getAllCategories().first())
                    }
                    get("/{id}/subcategories") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                        call.respond(categoryRepository.getSubcategories(id).first())
                    }
                    post {
                        val req = call.receive<Map<String, String>>()
                        val name = req["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val type = req["type"] ?: "EXPENSE"
                        val icon = req["icon"] ?: "📁"
                        
                        val category = CategoryEntity(
                            name = name,
                            type = type,
                            icon = icon,
                            color = 0,
                            parentId = null,
                            budgetLimit = null
                        )
                        categoryRepository.insertCategory(category)
                        call.respond(HttpStatusCode.Created)
                    }
                    delete("/{id}") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val category = categoryRepository.getCategoryById(id)
                        if (category != null) {
                            categoryRepository.deleteCategory(category)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                route("/budgets") {
                    get {
                        val budgets = budgetRepository.getActiveBudgetsWithDetails(LocalDate.now()).first()
                        call.respond(budgets.map { 
                            mapOf(
                                "id" to it.budget.id,
                                "categoryName" to it.category.name,
                                "categoryIcon" to it.category.icon,
                                "amount" to it.budget.amount,
                                "spentAmount" to it.budget.spentAmount,
                                "remaining" to (it.budget.amount - it.budget.spentAmount),
                                "percent" to (if (it.budget.amount > 0) (it.budget.spentAmount / it.budget.amount) * 100 else 0.0)
                            )
                        })
                    }
                    post {
                        val req = call.receive<Map<String, String>>()
                        val categoryId = req["categoryId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val amount = req["amount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        
                        val budget = BudgetEntity(
                            categoryId = categoryId,
                            amount = amount,
                            period = "MONTHLY",
                            startDate = LocalDate.now().withDayOfMonth(1),
                            endDate = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1)
                        )
                        budgetRepository.insertBudget(budget)
                        call.respond(HttpStatusCode.Created)
                    }
                    delete("/{id}") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val budget = budgetRepository.getBudgetById(id)
                        if (budget != null) {
                            budgetRepository.deleteBudget(budget)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                route("/goals") {
                    get {
                        call.respond(goalRepository.getAllGoals().first())
                    }
                    post {
                        val req = call.receive<Map<String, String>>()
                        val name = req["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val target = req["targetAmount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        
                        val goal = GoalEntity(
                            name = name,
                            targetAmount = target,
                            currentAmount = 0.0,
                            targetDate = LocalDate.now().plusYears(1),
                            type = "SAVINGS",
                            priority = 3,
                            color = 0,
                            icon = "🎯",
                            description = ""
                        )
                        goalRepository.insertGoal(goal)
                        call.respond(HttpStatusCode.Created)
                    }
                    delete("/{id}") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val goal = goalRepository.getGoalById(id)
                        if (goal != null) {
                            goalRepository.deleteGoal(goal)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                route("/investments") {
                    get {
                        call.respond(investmentRepository.getAllInvestments().first())
                    }
                    post {
                        val req = call.receive<Map<String, String>>()
                        val name = req["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val price = req["price"]?.toDoubleOrNull() ?: 0.0
                        
                        val investment = InvestmentEntity(
                            name = name,
                            type = req["type"] ?: "STOCKS",
                            symbol = req["symbol"] ?: name.take(4).uppercase(),
                            quantity = 0.0,
                            averagePrice = price,
                            currentPrice = price
                        )
                        investmentRepository.insertInvestment(investment)
                        call.respond(HttpStatusCode.Created)
                    }
                    post("/{id}/buy") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val req = call.receive<Map<String, String>>()
                        val amount = req["amount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val walletId = req["walletId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        
                        // We need a helper or use existing logic in repository
                        // For simplicity, we'll create a transaction of type Expense with transactionSourceType = "INVESTMENT_BUY"
                        val transaction = TransactionEntity(
                            amount = amount,
                            type = TransactionType.Expense,
                            walletFromId = walletId,
                            investmentId = id,
                            transactionSourceType = "INVESTMENT_BUY",
                            dateTime = LocalDateTime.now(),
                            note = "Investment Purchase"
                        )
                        transactionRepository.insertTransaction(transaction)
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/{id}/sell") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val req = call.receive<Map<String, String>>()
                        val amount = req["amount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val walletId = req["walletId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        
                        val transaction = TransactionEntity(
                            amount = amount,
                            type = TransactionType.Income,
                            walletToId = walletId,
                            investmentId = id,
                            transactionSourceType = "INVESTMENT_SELL",
                            dateTime = LocalDateTime.now(),
                            note = "Investment Sale"
                        )
                        transactionRepository.insertTransaction(transaction)
                        call.respond(HttpStatusCode.OK)
                    }
                }

                route("/loans") {
                    get {
                        call.respond(loanRepository.getAllLoans().first())
                    }
                    get("/{id}/statement") {
                        val id = call.parameters["id"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
                        call.respond(loanRepository.getEventsForLoan(id).first())
                    }
                }

                route("/backup") {
                    get("/export") {
                        val backup = backupRepository.createJsonBackup()
                        call.respondText(backup, ContentType.Application.Json)
                    }
                    post("/import") {
                        val jsonContent = call.receiveText()
                        // Use a temporary file to leverage existing restoreFromJson(Uri)
                        val tempFile = File(this@MoneyPilotApi.context.cacheDir, "web_restore.json")
                        tempFile.writeText(jsonContent)
                        val result = backupRepository.restoreFromJson(android.net.Uri.fromFile(tempFile))
                        if (result.isSuccess) {
                            call.respond(HttpStatusCode.OK, mapOf("message" to "Restore successful"))
                            broadcast("DATA_UPDATED")
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to result.exceptionOrNull()?.message))
                        }
                    }
                }

                get("/stats/categories") {
                    val now = LocalDateTime.now()
                    val start = now.withDayOfMonth(1).withHour(0).withMinute(0)
                    val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                    
                    val categories = categoryRepository.getAllCategories().first()
                    val data = categories.map { category ->
                        val sum = transactionRepository.getCategoryExpenseSum(category.id, start, end)
                        mapOf(
                            "name" to category.name,
                            "sum" to sum,
                            "color" to category.color,
                            "icon" to category.icon
                        )
                    }.filter { (it["sum"] as Double) > 0 }
                    
                    call.respond(data)
                }

                get("/big-bills") {
                    call.respond(bigBillRepository.getAllBigBills().first())
                }
                
                route("/transactions") {
                    get {
                        val transactions = transactionRepository.getAllTransactionsWithDetails().first()
                        call.respond(transactions.map { 
                            mapOf(
                                "id" to it.transaction.id,
                                "amount" to it.transaction.amount,
                                "type" to it.transaction.type.name,
                                "note" to it.transaction.note,
                                "dateTime" to it.transaction.dateTime.toString(),
                                "categoryName" to it.category?.name,
                                "walletName" to (it.walletFrom?.name ?: it.walletTo?.name),
                                "source" to it.transaction.transactionSourceType
                            )
                        })
                    }
                    
                    post {
                        val req = call.receive<Map<String, String>>()
                        val amount = req["amount"]?.toDoubleOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val typeStr = req["type"] ?: "Expense"
                        val type = TransactionType.valueOf(typeStr)
                        val note = req["note"] ?: ""
                        val categoryId = req["categoryId"]?.toLongOrNull()
                        val subcategoryId = req["subcategoryId"]?.toLongOrNull()
                        val goalId = req["goalId"]?.toLongOrNull()
                        val investmentId = req["investmentId"]?.toLongOrNull()
                        val dateTime = try { LocalDateTime.parse(req["dateTime"]) } catch (e: Exception) { LocalDateTime.now() }

                        val txId = UUID.randomUUID().toString()
                        
                        if (type == TransactionType.Transfer) {
                            val fromId = req["walletFromId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val toId = req["walletToId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                            
                            val transfer = TransactionEntity(
                                id = txId,
                                amount = amount,
                                type = type,
                                walletFromId = fromId,
                                walletToId = toId,
                                note = note,
                                dateTime = dateTime,
                                transactionSourceType = req["source"] ?: "WEB_DESKTOP"
                            )
                            transactionRepository.createTransfer(transfer)
                        } else {
                            val walletId = req["walletId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val transaction = TransactionEntity(
                                id = txId,
                                amount = amount,
                                type = type,
                                walletFromId = if (type == TransactionType.Expense) walletId else null,
                                walletToId = if (type == TransactionType.Income) walletId else null,
                                categoryId = categoryId,
                                subcategoryId = subcategoryId,
                                goalId = goalId,
                                investmentId = investmentId,
                                note = note,
                                dateTime = dateTime,
                                transactionSourceType = req["source"] ?: "WEB_DESKTOP"
                            )
                            transactionRepository.insertTransaction(transaction)
                        }
                        call.respond(HttpStatusCode.Created, mapOf("id" to txId))
                    }

                    delete("/{id}") {
                        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val transaction = transactionRepository.getTransactionById(id)
                        if (transaction != null) {
                            transactionRepository.deleteTransaction(transaction)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                get("/stats/summary") {
                    val now = LocalDateTime.now()
                    val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0)
                    val endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59)
                    
                    val income = transactionRepository.getTotalSumByType(TransactionType.Income, startOfMonth, endOfMonth) ?: 0.0
                    val expense = transactionRepository.getTotalSumByType(TransactionType.Expense, startOfMonth, endOfMonth) ?: 0.0
                    
                    call.respond(mapOf(
                        "income" to income,
                        "expense" to expense,
                        "savings" to (income - expense)
                    ))
                }
            }
        }
    }
}
