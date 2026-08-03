package com.yourname.moneypilot.api

import android.content.Context
import com.yourname.moneypilot.data.repository.TransactionRepository
import com.yourname.moneypilot.data.repository.WalletRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.io.*

@Singleton
class MoneyPilotApi @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository
) {
    fun Application.module() {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            anyHost() // Allow access from local network browsers
            allowHeader(io.ktor.http.HttpHeaders.ContentType)
        }
        
        routing {
            get("/") {
                try {
                    val html = this@MoneyPilotApi.context.assets.open("webapp/index.html").bufferedReader().use { it.readText() }
                    call.respondText(html, ContentType.Text.Html)
                } catch (e: Exception) {
                    call.respondText("Error loading index.html: ${e.message}", status = HttpStatusCode.InternalServerError)
                }
            }
            
            route("/api") {
                get("/status") {
                    call.respond(mapOf("status" to "online", "version" to "1.0"))
                }
                
                get("/wallets") {
                    val wallets = walletRepository.getAllWallets().first()
                    call.respond(wallets)
                }
                
                get("/transactions") {
                    val transactions = transactionRepository.getAllTransactionsWithDetails().first()
                    // We need to handle TransactionWithDetails serialization or simplify it
                    call.respond(transactions.map { it.transaction })
                }
            }
        }
    }
}
