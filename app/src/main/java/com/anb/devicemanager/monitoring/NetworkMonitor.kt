package com.anb.devicemanager.monitoring

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process

data class NetworkInfo(
    val connected: Boolean,
    val connectionType: String,
    val receivedBytes: Long,
    val transmittedBytes: Long
)

class NetworkMonitor(private val context: Context) {

    fun getNetworkInfo(): NetworkInfo {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork
        val capabilities = network?.let {
            manager.getNetworkCapabilities(it)
        }

        val type = when {
            capabilities == null -> "Disconnected"
            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) -> "Wi-Fi"
            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) -> "Mobile"
            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            ) -> "Ethernet"
            else -> "Other"
        }

        val rx = TrafficStats.getUidRxBytes(Process.myUid())
        val tx = TrafficStats.getUidTxBytes(Process.myUid())

        return NetworkInfo(
            connected = capabilities != null,
            connectionType = type,
            receivedBytes = rx.coerceAtLeast(0),
            transmittedBytes = tx.coerceAtLeast(0)
        )
    }
}
