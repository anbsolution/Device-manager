package com.anb.devicemanager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@Composable
fun NetworkInsightsPanel(context: Context) {
    var refreshKey by remember { mutableIntStateOf(0) }

    val result = remember(refreshKey) {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork
        val caps = network?.let {
            manager.getNetworkCapabilities(it)
        }

        val connected = caps != null
        val validated = caps?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        ) == true

        val connectionType = when {
            caps == null -> "Disconnected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val historyCount = runCatching {
            JSONArray(
                context.getSharedPreferences(
                    "anb_network_history",
                    Context.MODE_PRIVATE
                ).getString("records", "[]") ?: "[]"
            ).length()
        }.getOrDefault(0)

        val stabilityEvents = runCatching {
            JSONArray(
                context.getSharedPreferences(
                    "anb_network_stability",
                    Context.MODE_PRIVATE
                ).getString("events", "[]") ?: "[]"
            )
        }.getOrDefault(JSONArray())

        val recentEvents = (0 until stabilityEvents.length())
            .mapNotNull { index ->
                runCatching {
                    stabilityEvents.getJSONObject(index)
                }.getOrNull()
            }
            .filter {
                System.currentTimeMillis() -
                    it.optLong("time", 0L) <= 24 * 60 * 60 * 1000L
            }

        val disconnects = recentEvents.count {
            it.optString("event") == "Disconnected"
        }

        var score = 0
        val reasons = mutableListOf<String>()

        if (connected) {
            score += 30
        } else {
            reasons.add("No active network connection")
        }

        if (validated) {
            score += 40
        } else {
            reasons.add("Internet validation is unavailable")
        }

        if (historyCount >= 5) {
            score += 15
        } else {
            reasons.add("Save more network snapshots for better analysis")
        }

        if (recentEvents.isNotEmpty()) {
            val stabilityScore = when {
                disconnects == 0 -> 15
                disconnects <= 2 -> 10
                disconnects <= 5 -> 5
                else -> 0
            }
            score += stabilityScore

            if (disconnects > 0) {
                reasons.add("$disconnects disconnection events recorded in the last 24 hours")
            }
        } else {
            reasons.add("No recent connection events available")
        }

        Triple(
            score.coerceIn(0, 100),
            listOf(connectionType, connected.toString(), validated.toString()),
            reasons
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Insights & Quality Score",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "${result.first} / 100",
                style = MaterialTheme.typography.headlineLarge
            )

            LinearProgressIndicator(
                progress = { result.first / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Connection: ${result.second[0]}")
            Text("Connected: ${result.second[1]}")
            Text("Internet validated: ${result.second[2]}")

            HorizontalDivider()

            Text(
                "Analysis & Recommendations",
                style = MaterialTheme.typography.titleMedium
            )

            if (result.third.isEmpty()) {
                Text("No issues detected by available indicators.")
            } else {
                result.third.forEach { reason ->
                    Text("• $reason")
                }
            }

            OutlinedButton(
                onClick = { refreshKey++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Network Insights")
            }

            Text(
                "This is a heuristic score based on current connectivity and locally saved records. It is not a measured speed test or guaranteed network-quality rating.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
