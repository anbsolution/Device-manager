package com.anb.devicemanager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private data class StabilityEvent(
    val time: Long,
    val event: String,
    val connection: String
)

@Composable
fun NetworkStabilityPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_network_stability",
            Context.MODE_PRIVATE
        )
    }

    var connected by remember { mutableStateOf(false) }
    var validated by remember { mutableStateOf(false) }
    var connectionType by remember { mutableStateOf("Unknown") }
    var sessionStart by remember { mutableLongStateOf(0L) }
    var refreshKey by remember { mutableIntStateOf(0) }

    fun saveEvent(event: String, type: String) {
        val records = runCatching {
            JSONArray(prefs.getString("events", "[]") ?: "[]")
        }.getOrDefault(JSONArray())

        val updated = JSONArray()
        for (i in (records.length() - 99).coerceAtLeast(0) until records.length()) {
            updated.put(records.getJSONObject(i))
        }

        updated.put(JSONObject().apply {
            put("time", System.currentTimeMillis())
            put("event", event)
            put("connection", type)
        })

        prefs.edit().putString("events", updated.toString()).apply()
    }

    DisposableEffect(context) {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        fun updateNetwork(network: Network?) {
            val caps = network?.let {
                manager.getNetworkCapabilities(it)
            }

            connected = caps != null
            validated = caps?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true

            connectionType = when {
                caps == null -> "Disconnected"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }

            if (connected && sessionStart == 0L) {
                sessionStart = System.currentTimeMillis()
            }
        }

        updateNetwork(manager.activeNetwork)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val wasConnected = connected
                updateNetwork(network)

                if (!wasConnected) {
                    sessionStart = System.currentTimeMillis()
                    saveEvent("Connected", connectionType)
                }
                refreshKey++
            }

            override fun onLost(network: Network) {
                if (manager.activeNetwork == null) {
                    val previousType = connectionType
                    connected = false
                    validated = false
                    connectionType = "Disconnected"
                    sessionStart = 0L
                    saveEvent("Disconnected", previousType)
                    refreshKey++
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (manager.activeNetwork == network) {
                    updateNetwork(network)
                    refreshKey++
                }
            }
        }

        manager.registerDefaultNetworkCallback(callback)

        onDispose {
            runCatching {
                manager.unregisterNetworkCallback(callback)
            }
        }
    }

    val events = remember(refreshKey) {
        runCatching {
            val array = JSONArray(
                prefs.getString("events", "[]") ?: "[]"
            )
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                StabilityEvent(
                    time = item.getLong("time"),
                    event = item.getString("event"),
                    connection = item.getString("connection")
                )
            }.sortedByDescending { it.time }
        }.getOrDefault(emptyList())
    }

    val duration = if (connected && sessionStart > 0L) {
        ((System.currentTimeMillis() - sessionStart) / 1000L)
            .coerceAtLeast(0L)
    } else 0L

    val durationText = "${duration / 3600}h ${(duration % 3600) / 60}m ${duration % 60}s"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Stability Monitor",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Status: ${if (connected) "Connected" else "Disconnected"}")
            Text("Connection: $connectionType")
            Text("Internet validated: ${if (validated) "Yes" else "No"}")
            Text("Current session: $durationText")

            OutlinedButton(
                onClick = {
                    val manager = context.getSystemService(
                        Context.CONNECTIVITY_SERVICE
                    ) as ConnectivityManager
                    val network = manager.activeNetwork
                    val caps = network?.let {
                        manager.getNetworkCapabilities(it)
                    }
                    connected = caps != null
                    validated = caps?.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    ) == true
                    refreshKey++
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Status")
            }

            HorizontalDivider()

            Text(
                "Connection Events (${events.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (events.isEmpty()) {
                Text("No connection events recorded yet.")
            } else {
                events.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            item.event,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("Network: ${item.connection}")
                        Text(
                            DateFormat.getDateTimeInstance().format(
                                Date(item.time)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider()
                }
            }

            Text(
                "Events are recorded locally while this app process is active. Android may limit callbacks during process shutdown.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
