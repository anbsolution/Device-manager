package com.anb.devicemanager.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectivityDiagnosticsPanel(context: Context) {
    var refreshKey by remember { mutableIntStateOf(0) }

    val diagnostics = remember(refreshKey) {
        val manager = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val network = manager.activeNetwork
        val caps = network?.let {
            manager.getNetworkCapabilities(it)
        }
        val links = network?.let {
            manager.getLinkProperties(it)
        }

        val type = when {
            caps == null -> "Disconnected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }

        val addresses = links?.linkAddresses
            ?.mapNotNull { it.address.hostAddress }
            ?.distinct()
            ?: emptyList()

        val dns = links?.dnsServers
            ?.mapNotNull { it.hostAddress }
            ?: emptyList()

        val routes = links?.routes
            ?.filter { it.destination.prefixLength == 0 }
            ?.mapNotNull { it.gateway?.hostAddress }
            ?: emptyList()

        val validated = caps?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        ) == true

        val internet = caps?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) == true

        val metered = if (network != null) {
            manager.isActiveNetworkMetered
        } else {
            false
        }

        listOf(
            "Connection" to type,
            "Connected" to if (caps != null) "Yes" else "No",
            "Internet capability" to if (internet) "Available" else "Not available",
            "Internet validated" to if (validated) "Yes" else "No",
            "Metered network" to if (metered) "Yes" else "No",
            "VPN active" to if (
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            ) "Yes" else "No",
            "Interface" to (links?.interfaceName ?: "Unavailable"),
            "Local IP" to addresses.joinToString("\n").ifBlank { "Unavailable" },
            "DNS servers" to dns.joinToString("\n").ifBlank { "Unavailable" },
            "Default gateway" to routes.joinToString("\n").ifBlank { "Unavailable" },
            "Download link estimate" to
                "${caps?.linkDownstreamBandwidthKbps ?: 0} Kbps",
            "Upload link estimate" to
                "${caps?.linkUpstreamBandwidthKbps ?: 0} Kbps"
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Connectivity Diagnostics",
                style = MaterialTheme.typography.titleLarge
            )

            diagnostics.forEach { (label, value) ->
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedButton(
                onClick = { refreshKey++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Network Diagnostics")
            }

            Text(
                "Network details are read locally. Some values may be unavailable on certain devices.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
