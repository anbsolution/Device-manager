package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.*

@Composable
fun DeviceHealthPanel(context: Context) {
    var report by remember {
        mutableStateOf<DeviceHealthReport?>(null)
    }
    var error by remember { mutableStateOf(false) }

    fun refresh() {
        try {
            val performance = PerformanceMonitor(context).getMetrics()
            val storage = StorageMonitor(context).getStorageInfo()
            val battery = BatteryMonitor(context).getBatteryInfo()
            val network = NetworkMonitor(context).getNetworkInfo()

            report = DeviceHealthMonitor().evaluate(
                performance,
                storage,
                battery,
                network
            )
            error = false
        } catch (_: Exception) {
            error = true
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Device Health Monitor",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Rule-based device status",
                style = MaterialTheme.typography.bodySmall
            )

            if (error) {
                Text("Unable to read some device metrics.")
            }

            report?.let { result ->
                Text(
                    "Overall Status: ${result.overallStatus}",
                    style = MaterialTheme.typography.headlineSmall
                )

                HorizontalDivider()

                result.checks.forEach { check ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "${check.title} • ${check.status}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            check.detail,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider()
                }
            } ?: Text("Reading device status...")

            Button(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Health Check")
            }

            Text(
                "Assessment uses Android-reported metrics and fixed thresholds. It is not a hardware diagnostic certification.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
