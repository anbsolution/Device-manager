package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BenchmarkResult(
    val ram: Int,
    val storage: Int,
    val battery: Int,
    val thermal: Int,
    val overall: Int,
    val notes: List<String>
)

private fun score(value: Int): Int = value.coerceIn(0, 100)

@Composable
fun DeviceBenchmarkPanel(context: Context) {
    var result by remember { mutableStateOf<BenchmarkResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true

        result = withContext(Dispatchers.IO) {
            val performance = PerformanceMonitor(context).getMetrics()
            val storage = StorageMonitor(context).getStorageInfo()
            val battery = BatteryMonitor(context).getBatteryInfo()

            val ramUsed = if (performance.totalRam > 0) {
                ((performance.totalRam - performance.availableRam) *
                    100 / performance.totalRam).toInt().coerceIn(0, 100)
            } else 0

            val ramScore = score(100 - ramUsed)
            val storageScore = score(100 - storage.usedPercent)

            val batteryScore = when {
                battery == null -> 0
                battery.health == "Good" -> 100
                battery.health == "Unknown" -> 70
                else -> 35
            }

            val thermalScore = when (performance.thermalStatus) {
                "Normal" -> 100
                "Light" -> 90
                "Moderate" -> 70
                "Severe" -> 40
                "Critical", "Emergency", "Shutdown" -> 15
                else -> 60
            }

            val overall = (
                ramScore + storageScore + batteryScore + thermalScore
            ) / 4

            val notes = mutableListOf<String>()

            if (ramUsed >= 85) notes.add("RAM usage is high.")
            if (storage.usedPercent >= 85) notes.add("Storage is nearly full.")
            if (battery != null && battery.health != "Good") {
                notes.add("Review battery health information.")
            }
            if (performance.thermalStatus in listOf(
                    "Severe", "Critical", "Emergency", "Shutdown"
                )
            ) {
                notes.add("Device thermal status needs attention.")
            }
            if (notes.isEmpty()) notes.add("No major threshold warnings detected.")

            BenchmarkResult(
                ram = ramScore,
                storage = storageScore,
                battery = batteryScore,
                thermal = thermalScore,
                overall = overall,
                notes = notes
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
                "Device Performance Scorecard",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Local rule-based score • Not a standardized benchmark",
                style = MaterialTheme.typography.bodySmall
            )

            Button(onClick = { refreshKey++ }) {
                Text("Run Benchmark")
            }

            if (loading) {
                CircularProgressIndicator()
            }

            result?.let { data ->
                Text(
                    "${data.overall}/100",
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    when {
                        data.overall >= 85 -> "Good device condition"
                        data.overall >= 65 -> "Moderate device condition"
                        data.overall >= 40 -> "Needs attention"
                        else -> "Several checks need attention"
                    },
                    style = MaterialTheme.typography.titleMedium
                )

                HorizontalDivider()

                ScoreRow("RAM", data.ram)
                ScoreRow("Storage", data.storage)
                ScoreRow("Battery", data.battery)
                ScoreRow("Thermal", data.thermal)

                HorizontalDivider()

                Text("Recommendations", style = MaterialTheme.typography.titleMedium)
                data.notes.forEach { Text("• $it") }
            }

            Text(
                "Scores are heuristic indicators based on current Android readings. They do not measure CPU speed, battery capacity degradation, or hardware reliability.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ScoreRow(name: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name)
            Text("$value/100")
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
