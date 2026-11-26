package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.*

private fun formatBytes(bytes: Long): String {
    return String.format(
        "%.2f GB", bytes / (1024.0 * 1024 * 1024)
    )
}

@Composable
fun SystemMetricsPanel(context: Context) {
    var performance by remember {
        mutableStateOf<PerformanceInfo?>(null)
    }
    var storage by remember {
        mutableStateOf<StorageInfo?>(null)
    }
    var network by remember {
        mutableStateOf<NetworkInfo?>(null)
    }
    var backgroundEvents by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }

    fun refresh() {
        performance = PerformanceMonitor(context).getMetrics()
        storage = StorageMonitor(context).getStorageInfo()
        network = NetworkMonitor(context).getNetworkInfo()

        backgroundEvents = try {
            BackgroundActivityMonitor(context)
                .getBackgroundTransitions()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "System Intelligence",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = { refresh() }) {
                Text("Refresh")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("RAM Monitoring")
                Text(
                    performance?.let {
                        "${formatBytes(it.usedRam)} used / " +
                            "${formatBytes(it.totalRam)} total"
                    } ?: "Unavailable",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    performance?.let {
                        "${formatBytes(it.availableRam)} available"
                    } ?: ""
                )
                Text(
                    if (performance?.lowMemory == true)
                        "Low memory warning"
                    else "Memory status normal"
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Processor & Thermal")
                Text(
                    performance?.let {
                        "${it.cpuCores} available CPU cores"
                    } ?: "Unavailable"
                )
                Text(
                    "Thermal: ${performance?.thermalStatus ?: "Unavailable"}"
                )
                Text("CPU utilization: Not available from standard API")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Internal Storage")
                Text(
                    storage?.let { "${it.usedPercent}% used" }
                        ?: "Unavailable",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    storage?.let {
                        "${formatBytes(it.usedBytes)} used"
                    } ?: ""
                )
                Text(
                    storage?.let {
                        "${formatBytes(it.freeBytes)} available"
                    } ?: ""
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Background Activity • 24 Hours")
                if (backgroundEvents.isEmpty()) {
                    Text("No events available. Check Usage Access.")
                } else {
                    backgroundEvents.entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .forEach { entry ->
                            Text(
                                "${entry.key}: ${entry.value} transitions"
                            )
                        }
                }
                Text("Transitions are not background runtime.")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Network Intelligence",
                    style = MaterialTheme.typography.titleLarge)

                Text(
                    network?.connectionType ?: "Unavailable",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    if (network?.internetValidated == true)
                        "Internet: Validated"
                    else if (network?.internetConfigured == true)
                        "Internet: Not validated"
                    else "Internet: Unavailable"
                )

                Text(
                    "Connection: " +
                        if (network?.connected == true) "Connected"
                        else "Disconnected"
                )

                Text(
                    "Data plan: " +
                        if (network?.metered == true) "Metered"
                        else "Unmetered / Unknown"
                )

                Text("VPN: ${
                    if (network?.vpnActive == true) "Active" else "Not detected"
                }")

                Text("Estimated download: ${
                    network?.downloadKbps?.let { "$it Kbps" } ?: "Unavailable"
                }")

                Text("Estimated upload: ${
                    network?.uploadKbps?.let { "$it Kbps" } ?: "Unavailable"
                }")

                HorizontalDivider()

                Text("ANB app received: ${
                    network?.let { formatBytes(it.receivedBytes) } ?: "Unavailable"
                }")

                Text("ANB app transmitted: ${
                    network?.let { formatBytes(it.transmittedBytes) } ?: "Unavailable"
                }")

                Text("Traffic counters belong to this app only.")
            }
        }    }
}
