package com.yourname.moneypilot.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yourname.moneypilot.R
import com.yourname.moneypilot.api.MoneyPilotApi
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject

@AndroidEntryPoint
class WebAppService : Service() {

    @Inject lateinit var moneyPilotApi: MoneyPilotApi

    private var server: NettyApplicationEngine? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private val CHANNEL_ID = "webapp_server_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        startServer()
        return START_STICKY
    }

    private fun startServer() {
        if (server != null) return

        val ipAddress = getLocalIpAddress() ?: "Unknown IP"
        val port = 8080
        
        startForeground(NOTIFICATION_ID, createNotification(ipAddress, port))

        serviceScope.launch {
            try {
                server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
                    with(moneyPilotApi) { module() }
                }.start(wait = false)
                Timber.d("Ktor Server started at http://$ipAddress:$port")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start Ktor Server")
                updateNotification("Server Error: ${e.message}")
            }
        }
    }

    private fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        Timber.d("Ktor Server stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web App Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running MoneyPilot Desktop View Server"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(ip: String, port: Int): Notification {
        val stopIntent = Intent(this, WebAppService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MoneyPilot Web Server Active")
            .setContentText("Visit http://$ip:$port in your browser")
            .setSmallIcon(android.R.drawable.ic_menu_share) // Temporary icon
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MoneyPilot Web Server Status")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is InetAddress) {
                        val ip = inetAddress.hostAddress
                        if (ip != null && !ip.contains(":")) return ip // Prefer IPv4
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        return null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}
