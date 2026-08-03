package com.yourname.moneypilot.ui.features.webapp

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.yourname.moneypilot.service.WebAppService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject

data class WebAppState(
    val isRunning: Boolean = false,
    val ipAddress: String? = null,
    val port: Int = 8080
)

@HiltViewModel
class WebAppAccessViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(WebAppState())
    val state = _state.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val isRunning = isServiceRunning(getApplication(), WebAppService::class.java)
        val ip = getLocalIpAddress()
        _state.update { it.copy(isRunning = isRunning, ipAddress = ip) }
    }

    fun toggleServer() {
        val intent = Intent(getApplication(), WebAppService::class.java)
        if (_state.value.isRunning) {
            intent.action = "STOP"
            getApplication<Application>().startService(intent)
        } else {
            getApplication<Application>().startForegroundService(intent)
        }
        // Small delay to allow service to start/stop before refreshing
        _state.update { it.copy(isRunning = !_state.value.isRunning) }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
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
                        if (ip != null && !ip.contains(":")) return ip
                    }
                }
            }
        } catch (ex: Exception) { }
        return null
    }
}
