package com.anb.devicemanager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

private data class LiveSpeed(
    val downloadBytes: Long = 0L,
    val uploadBytes: Long = 0L,
    val available: Boolean = false,
    val connection: String = "Checking",
    val validated: Boolean = false
)

private fun formatLiveSpeed(bytesPerSecond: Long): String {
    val mbps = bytesPerSecond * 8.0 / 1_000_000.0
    return String.format(Locale.US, "%.2f Mbps", mbps)
}

private fun formatLiveBytes(bytesPerSecond: Long): String {
    return if (bytesPerSecond >= 1024L) {
        String.format(
            Locale.US,
            "%.1f KB/s",
            bytesPerSecond / 1024.0
        )
    } else {
        "$bytesPerSecond B/s"
    }
}

@Composable
fun LiveNetworkSpeedPanel(context: Context) {
    var speed by remember { mutableStateOf(LiveSpeed()) }

    LaunchedEffect(Unit) {
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        var previousTime = SystemClock.elapsedRealtime()

        while (true) {
            delay(1000L)

            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            val currentTime = SystemClock.elapsedRealtime()

            val elapsed = currentTime - previousTime

            val available = previousRx >= 0L &&
                previousTx >= 0L &&
                currentRx >= previousRx &&
                currentTx >= previousTx &&
                elapsed > 0L

            val rxRate = if (available) {
                (currentRx - previousRx) * 1000L / elapsed
            } else 0L

            val txRate = if (available) {
                (currentTx - previousTx) * 1000L / elapsed
            } else 0L

            val manager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

            val network = manager.activeNetwork
            val caps = network?.let {
                manager.getNetworkCapabilities(it)
            }

            val connection = when {
                caps == null -> "Disconnected"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }

            val validated = caps?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true

            speed = LiveSpeed(
                downloadBytes = rxRate,
                uploadBytes = txRate,
                available = available,
                connection = connection,
                validated = validated
            )

            previousRx = currentRx
            previousTx = currentTx
            previousTime = currentTime
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Live Internet Speed Monitor",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Connection")
                Text(speed.connection)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Internet Status")
                Text(if (speed.validated) "Validated" else "Not validated")
            }

            HorizontalDivider()

            Text(
                "↓ Download",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                if (speed.available) formatLiveSpeed(speed.downloadBytes)
                else "Unavailable",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                if (speed.available) formatLiveBytes(speed.downloadBytes)
                else "Waiting for traffic data"
            )

            HorizontalDivider()

            Text(
                "↑ Upload",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                if (speed.available) formatLiveSpeed(speed.uploadBytes)
                else "Unavailable",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                if (speed.available) formatLiveBytes(speed.uploadBytes)
                else "Waiting for traffic data"
            )

            Text(
                "Updates every second while this panel is active. Measures aggregate device network traffic, not maximum ISP speed. Android traffic counters may be unavailable on some devices.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
