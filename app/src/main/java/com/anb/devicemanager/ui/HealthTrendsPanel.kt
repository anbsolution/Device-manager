package com.anb.devicemanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.DeviceSnapshot
import com.anb.devicemanager.monitoring.MonitoringDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun HealthTrendsPanel(context: android.content.Context) {
    var snapshots by remember { mutableStateOf(emptyList<DeviceSnapshot>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        snapshots = withContext(Dispatchers.IO) {
            MonitoringDatabase.get(context)
                .snapshotDao()
                .getRecent()
                .asReversed()
        }
        loading = false
    }

    val latest = snapshots.lastOrNull()
    val first = snapshots.firstOrNull()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Advanced Device Analytics",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Historical readings saved locally",
                style = MaterialTheme.typography.bodySmall
            )

            Button(onClick = { refreshKey++ }) {
                Text("Refresh Analytics")
            }

            if (loading) {
                Text("Loading analytics...")
            } else if (snapshots.isEmpty()) {
                Text("No monitoring history yet. Enable Background Device Monitoring and wait for its first check.")
            } else {
                Text("Recorded snapshots: ${snapshots.size}")

                Text(
                    "Latest reading: ${
                        DateFormat.getDateTimeInstance().format(
                            Date(latest!!.timestamp)
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall
                )

                MetricTrend(
                    title = "RAM Usage",
                    values = snapshots.map { it.ramUsedPercent.toFloat() },
                    suffix = "%"
                )

                MetricTrend(
                    title = "Storage Usage",
                    values = snapshots.map { it.storageUsedPercent.toFloat() },
                    suffix = "%"
                )

                MetricTrend(
                    title = "Battery Level",
                    values = snapshots
                        .filter { it.batteryPercent in 0..100 }
                        .map { it.batteryPercent.toFloat() },
                    suffix = "%"
                )

                Text("Latest device readings")
                Text("RAM: ${latest!!.ramUsedPercent}%")
                Text("Storage: ${latest.storageUsedPercent}%")
                Text(
                    "Battery: ${
                        if (latest.batteryPercent >= 0)
                            "${latest.batteryPercent}%"
                        else "Unavailable"
                    }"
                )
                Text(
                    "Battery temperature: ${
                        if (latest.batteryTemperature >= 0f)
                            "${latest.batteryTemperature} °C"
                        else "Unavailable"
                    }"
                )
                Text("Thermal status: ${latest.thermalStatus}")
                Text("Charging: ${if (latest.charging) "Yes" else "No"}")

                if (first != null && first.id != latest.id) {
                    Text(
                        "Trend comparison: latest reading compared with the oldest available snapshot."
                    )
                    Text(
                        "RAM change: ${latest.ramUsedPercent - first.ramUsedPercent} percentage points"
                    )
                    Text(
                        "Storage change: ${latest.storageUsedPercent - first.storageUsedPercent} percentage points"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTrend(
    title: String,
    values: List<Float>,
    suffix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        if (values.isEmpty()) {
            Text("Not enough readings available")
            return@Column
        }

        Text(
            "Latest: ${values.last().toInt()}$suffix",
            style = MaterialTheme.typography.bodySmall
        )

        val lineColor = MaterialTheme.colorScheme.primary
        val gridColor = MaterialTheme.colorScheme.outlineVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val left = 8f
            val right = size.width - 8f
            val top = 8f
            val bottom = size.height - 8f
            val chartHeight = bottom - top
            val chartWidth = right - left

            for (i in 0..4) {
                val y = top + chartHeight * i / 4f
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1f
                )
            }

            if (values.size == 1) {
                val y = bottom - (values[0].coerceIn(0f, 100f) / 100f) * chartHeight
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset((left + right) / 2f, y)
                )
            } else {
                val path = Path()
                values.forEachIndexed { index, value ->
                    val x = left + chartWidth * index / (values.size - 1).toFloat()
                    val y = bottom -
                        (value.coerceIn(0f, 100f) / 100f) * chartHeight

                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}
