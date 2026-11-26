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
    val transmittedBytes: Long,
    val internetConfigured: Boolean,
    val internetValidated: Boolean,
    val metered: Boolean,
    val downloadKbps: Int,
    val uploadKbps: Int,
    val vpnActive: Boolean
)

class NetworkMonitor(private val context: Context) {

    fun getNetworkInfo(): NetworkInfo {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork
        val caps = network?.let {
            manager.getNetworkCapabilities(it)
        }

        val vpn = caps?.hasTransport(
            NetworkCapabilities.TRANSPORT_VPN
        ) == true

        val type = when {
            caps == null -> "Disconnected"
            vpn -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val rx = TrafficStats.getUidRxBytes(Process.myUid())
        val tx = TrafficStats.getUidTxBytes(Process.myUid())

        return NetworkInfo(
            connected = caps != null,
            connectionType = type,
            receivedBytes = rx.coerceAtLeast(0),
            transmittedBytes = tx.coerceAtLeast(0),
            internetConfigured = caps?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true,
            internetValidated = caps?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true,
            metered = manager.isActiveNetworkMetered,
            downloadKbps = caps?.linkDownstreamBandwidthKbps ?: 0,
            uploadKbps = caps?.linkUpstreamBandwidthKbps ?: 0,
            vpnActive = vpn
        )
    }
}
