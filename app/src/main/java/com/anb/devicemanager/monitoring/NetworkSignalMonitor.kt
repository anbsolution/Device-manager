package com.anb.devicemanager.monitoring

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class NetworkSignalInfo(
    val connectionType: String = "Unknown",
    val signalLevel: Int? = null,
    val signalLabel: String = "N/A",
    val validated: Boolean = false,
    val vpnActive: Boolean = false,
    val metered: Boolean = false
)

fun readNetworkSignal(context: Context): NetworkSignalInfo {
    val manager = context.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager

    val network = manager.activeNetwork
    val caps = network?.let {
        manager.getNetworkCapabilities(it)
    }

    val type = when {
        caps == null -> "Offline"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Other"
    }

    var level: Int? = null

    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
        val wifi = context.applicationContext.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager

        @Suppress("DEPRECATION")
        val rssi = wifi.connectionInfo.rssi

        if (rssi in -100..-1) {
            level = WifiManager.calculateSignalLevel(rssi, 5)
        }
    }

    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
        val hasPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPhonePermission) {
            val telephony = context.getSystemService(
                Context.TELEPHONY_SERVICE
            ) as TelephonyManager

            level = try {
                @Suppress("DEPRECATION")
                telephony.signalStrength?.level
            } catch (_: SecurityException) {
                null
            }
        }
    }

    val label = when (level) {
        0 -> "Very Weak"
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Excellent"
        else -> "N/A"
    }

    return NetworkSignalInfo(
        connectionType = type,
        signalLevel = level,
        signalLabel = label,
        validated = caps?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        ) == true,
        vpnActive = caps?.hasTransport(
            NetworkCapabilities.TRANSPORT_VPN
        ) == true,
        metered = network != null && manager.isActiveNetworkMetered
    )
}
