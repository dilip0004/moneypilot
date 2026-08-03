package com.yourname.moneypilot.api

import com.yourname.moneypilot.data.local.database.entities.TransactionEntity
import com.yourname.moneypilot.data.local.database.entities.TransactionType
import com.yourname.moneypilot.data.repository.CategoryRepository
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
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
import java.time.LocalDateTime
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Singleton
class MoneyPilotApi @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
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
                walletRepository.getAllWallets()
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
                
                get("/wallets") {
                    call.respond(walletRepository.getAllWallets().first())
                }
                
                get("/categories") {
                    call.respond(categoryRepository.getAllCategories().first())
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
                        val type = TransactionType.valueOf(req["type"] ?: "Expense")
                        val walletId = req["walletId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val categoryId = req["categoryId"]?.toLongOrNull()
                        val note = req["note"] ?: ""

                        val transaction = TransactionEntity(
                            id = UUID.randomUUID().toString(),
                            amount = amount,
                            type = type,
                            walletFromId = if (type == TransactionType.Expense) walletId else null,
                            walletToId = if (type == TransactionType.Income) walletId else null,
                            categoryId = categoryId,
                            note = note,
                            dateTime = LocalDateTime.now(),
                            transactionSourceType = "WEB_DESKTOP"
                        )
                        transactionRepository.insertTransaction(transaction)
                        call.respond(HttpStatusCode.Created, mapOf("id" to transaction.id))
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
