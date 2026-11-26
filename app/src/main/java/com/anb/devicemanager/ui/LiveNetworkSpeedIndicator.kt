package com.anb.devicemanager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LiveNetworkSpeedIndicator(context: Context) {
    var download by remember { mutableLongStateOf(0L) }
    var upload by remember { mutableLongStateOf(0L) }
    var connection by remember { mutableStateOf("Checking") }
    var available by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var oldRx = TrafficStats.getTotalRxBytes()
        var oldTx = TrafficStats.getTotalTxBytes()
        var oldTime = SystemClock.elapsedRealtime()

        while (true) {
            delay(1000L)

            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - oldTime

            val valid = oldRx >= 0L &&
                oldTx >= 0L &&
                rx >= oldRx &&
                tx >= oldTx &&
                elapsed > 0L

            download = if (valid) {
                (rx - oldRx) * 1000L / elapsed
            } else 0L

            upload = if (valid) {
                (tx - oldTx) * 1000L / elapsed
            } else 0L

            available = valid

            val manager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

            val caps = manager.activeNetwork?.let {
                manager.getNetworkCapabilities(it)
            }

            connection = when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Network"
            }

            oldRx = rx
            oldTx = tx
            oldTime = now
        }
    }

    fun speed(value: Long): String {
        return if (value >= 1_000_000L) {
            String.format(Locale.US, "%.2f Mbps", value * 8.0 / 1_000_000.0)
        } else if (value >= 1024L) {
            String.format(Locale.US, "%.1f KB/s", value / 1024.0)
        } else {
            "$value B/s"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "↓ ${if (available) speed(download) else "--"}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "Download",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column {
                Text(
                    "↑ ${if (available) speed(upload) else "--"}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "Upload",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Text(
                connection,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
