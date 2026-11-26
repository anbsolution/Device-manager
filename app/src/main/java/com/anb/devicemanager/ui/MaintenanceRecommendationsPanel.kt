package com.anb.devicemanager.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.BatteryMonitor
import com.anb.devicemanager.monitoring.PerformanceMonitor
import com.anb.devicemanager.monitoring.StorageMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class MaintenanceData(
    val ramPercent: Int,
    val storagePercent: Int,
    val freeBytes: Long,
    val cacheBytes: Long,
    val batteryPercent: Int,
    val batteryTemperature: Float,
    val batteryHealth: String,
    val lowMemory: Boolean,
    val thermalStatus: String
)

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    val mb = bytes / (1024.0 * 1024.0)
    return if (gb >= 1) "%.2f GB".format(gb) else "%.0f MB".format(mb)
}

@Composable
fun MaintenanceRecommendationsPanel(context: Context) {
    var data by remember { mutableStateOf<MaintenanceData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        data = withContext(Dispatchers.IO) {
            val performance = PerformanceMonitor(context).getMetrics()
            val storage = StorageMonitor(context).getStorageInfo()
            val battery = BatteryMonitor(context).getBatteryInfo()

            val ram = if (performance.totalRam > 0) {
                ((performance.totalRam - performance.availableRam) * 100 /
                    performance.totalRam).toInt().coerceIn(0, 100)
            } else 0

            MaintenanceData(
                ramPercent = ram,
                storagePercent = storage.usedPercent,
                freeBytes = storage.freeBytes,
                cacheBytes = storage.appCacheBytes,
                batteryPercent = battery?.percentage ?: -1,
                batteryTemperature = battery?.temperature ?: -1f,
                batteryHealth = battery?.health ?: "Unknown",
                lowMemory = performance.lowMemory,
                thermalStatus = performance.thermalStatus
            )
        }
        loading = false
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Smart Device Maintenance",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "On-device analysis • No cloud • No automatic deletion",
                style = MaterialTheme.typography.bodySmall
            )

            Button(onClick = { refreshKey++ }) {
                Text("Analyze Again")
            }

            if (loading) {
                CircularProgressIndicator()
            } else {
                val current = data

                if (current == null) {
                    Text("Device information unavailable.")
                } else {
                    Text("RAM usage: ${current.ramPercent}%")
                    Text("Storage usage: ${current.storagePercent}%")
                    Text("Free storage: ${formatBytes(current.freeBytes)}")
                    Text("App cache: ${formatBytes(current.cacheBytes)}")
                    Text(
                        "Battery: ${
                            if (current.batteryPercent >= 0)
                                "${current.batteryPercent}%"
                            else "Unavailable"
                        }"
                    )
                    Text(
                        "Battery temperature: ${
                            if (current.batteryTemperature >= 0f)
                                "${current.batteryTemperature} °C"
                            else "Unavailable"
                        }"
                    )
                    Text("Battery health: ${current.batteryHealth}")
                    Text("Thermal status: ${current.thermalStatus}")

                    HorizontalDivider()

                    Text(
                        "Maintenance Recommendations",
                        style = MaterialTheme.typography.titleMedium
                    )

                    var recommendationCount = 0

                    if (current.storagePercent >= 90) {
                        recommendationCount++
                        Text("• Storage is critically high. Review large files and unused apps in Settings.")
                    } else if (current.storagePercent >= 80) {
                        recommendationCount++
                        Text("• Storage is getting full. Review files you no longer need.")
                    }

                    if (current.ramPercent >= 90 || current.lowMemory) {
                        recommendationCount++
                        Text("• RAM pressure detected. Review running apps and close apps you are not using.")
                    }

                    if (current.batteryTemperature >= 45f) {
                        recommendationCount++
                        Text("• Battery temperature is high. Pause demanding activity and let the device cool naturally.")
                    } else if (current.batteryTemperature >= 42f) {
                        recommendationCount++
                        Text("• Battery temperature is elevated. Monitor temperature during heavy use.")
                    }

                    if (current.batteryHealth != "Good" &&
                        current.batteryHealth != "Unknown"
                    ) {
                        recommendationCount++
                        Text("• Battery health reports: ${current.batteryHealth}. Check the device manufacturer's guidance.")
                    }

                    if (current.thermalStatus in listOf(
                            "Severe", "Critical", "Emergency", "Shutdown"
                        )
                    ) {
                        recommendationCount++
                        Text("• Device thermal status is ${current.thermalStatus}. Stop demanding tasks and allow cooling.")
                    }

                    if (recommendationCount == 0) {
                        Text("No immediate maintenance issues detected.")
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    ) {
                        Text("Open Android Settings")
                    }
                }
            }
        }
    }
}
