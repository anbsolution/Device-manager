package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.NetworkMonitor
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private data class NetworkRecord(
    val timestamp: Long,
    val type: String,
    val received: Long,
    val transmitted: Long,
    val validated: Boolean,
    val metered: Boolean
)

private fun formatNetworkBytes(bytes: Long): String {
    if (bytes < 0) return "Unavailable"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024.0)
    } else {
        "%.2f MB".format(mb)
    }
}

@Composable
fun NetworkHistoryPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_network_history",
            Context.MODE_PRIVATE
        )
    }

    var refreshKey by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }

    val network = remember(refreshKey) {
        NetworkMonitor(context).getNetworkInfo()
    }

    val history = remember(refreshKey) {
        runCatching {
            val raw = prefs.getString("records", "[]") ?: "[]"
            val json = JSONArray(raw)

            (0 until json.length()).map { index ->
                val item = json.getJSONObject(index)
                NetworkRecord(
                    timestamp = item.getLong("timestamp"),
                    type = item.getString("type"),
                    received = item.getLong("received"),
                    transmitted = item.getLong("transmitted"),
                    validated = item.getBoolean("validated"),
                    metered = item.getBoolean("metered")
                )
            }.sortedByDescending { it.timestamp }
        }.getOrDefault(emptyList())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Usage & Connectivity History",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Local network diagnostics • No cloud • No internet permission added",
                style = MaterialTheme.typography.bodySmall
            )

            Button(onClick = { refreshKey++ }) {
                Text("Refresh Network Status")
            }

            HorizontalDivider()

            Text("Current Connection", style = MaterialTheme.typography.titleMedium)
            Text("Connection: ${network.connectionType}")
            Text("Connected: ${if (network.connected) "Yes" else "No"}")
            Text("Internet capability: ${network.internetConfigured}")
            Text("Internet validated: ${network.internetValidated}")
            Text("Metered network: ${network.metered}")
            Text("VPN active: ${network.vpnActive}")

            Text(
                "Reported downstream bandwidth: ${network.downloadKbps} Kbps"
            )
            Text(
                "Reported upstream bandwidth: ${network.uploadKbps} Kbps"
            )

            HorizontalDivider()

            Text("App Network Traffic", style = MaterialTheme.typography.titleMedium)
            Text("Received: ${formatNetworkBytes(network.receivedBytes)}")
            Text("Transmitted: ${formatNetworkBytes(network.transmittedBytes)}")

            OutlinedButton(
                onClick = {
                    if (network.receivedBytes < 0 ||
                        network.transmittedBytes < 0
                    ) {
                        message = "Traffic counters are unavailable on this device."
                    } else {
                        val record = JSONObject().apply {
                            put("timestamp", System.currentTimeMillis())
                            put("type", network.connectionType)
                            put("received", network.receivedBytes)
                            put("transmitted", network.transmittedBytes)
                            put("validated", network.internetValidated)
                            put("metered", network.metered)
                        }

                        val records = JSONArray()
                        history.asReversed().take(99).forEach { old ->
                            records.put(JSONObject().apply {
                                put("timestamp", old.timestamp)
                                put("type", old.type)
                                put("received", old.received)
                                put("transmitted", old.transmitted)
                                put("validated", old.validated)
                                put("metered", old.metered)
                            })
                        }
                        records.put(record)

                        prefs.edit()
                            .putString("records", records.toString())
                            .apply()

                        message = "Network snapshot saved locally."
                        refreshKey++
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Network Snapshot")
            }

            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            Text(
                "Saved Network History (${history.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (history.isEmpty()) {
                Text("No snapshots saved yet.")
            } else {
                history.forEach { item ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            item.type,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            DateFormat.getDateTimeInstance().format(
                                Date(item.timestamp)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Received: ${formatNetworkBytes(item.received)}")
                        Text("Transmitted: ${formatNetworkBytes(item.transmitted)}")
                        Text("Validated: ${item.validated} • Metered: ${item.metered}")
                    }
                    HorizontalDivider()
                }
            }

            Text(
                "Traffic counters are for this app UID, not total phone-wide data usage. Bandwidth values are Android-reported link estimates, not measured speed-test results. Snapshots must be saved manually.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
