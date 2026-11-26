package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.BatteryHistoryStore
import com.anb.devicemanager.monitoring.BatterySnapshot
import java.text.DateFormat
import java.util.Date

@Composable
fun BatteryAnalyticsPanel(context: Context) {
    var refreshKey by remember { mutableIntStateOf(0) }

    val history = remember(refreshKey) {
        BatteryHistoryStore(context).getHistory()
            .sortedByDescending { it.timestamp }
    }

    val average = if (history.isNotEmpty()) {
        history.map { it.percentage }.average().toInt()
    } else 0

    val highest = history.maxOfOrNull { it.percentage } ?: 0
    val lowest = history.minOfOrNull { it.percentage } ?: 0

    val validTemperatures = history.filter {
        it.temperature >= 0f && it.temperature.isFinite()
    }

    val averageTemperature = validTemperatures
        .map { it.temperature }
        .average()
        .takeIf { it.isFinite() }

    val chargingRecords = history.count { it.charging }
    val dischargingRecords = history.size - chargingRecords

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Battery Analytics & Charging History",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Analysis based on locally saved battery records",
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedButton(onClick = { refreshKey++ }) {
                Text("Refresh Battery Analytics")
            }

            if (history.isEmpty()) {
                Text(
                    "No battery history found. Use the app and allow battery readings to be saved."
                )
            } else {
                Text("Total recorded readings: ${history.size}")
                Text("Average battery level: $average%")
                Text("Highest recorded level: $highest%")
                Text("Lowest recorded level: $lowest%")

                Text(
                    "Average recorded temperature: ${
                        averageTemperature?.let { "%.1f °C".format(it) }
                            ?: "Unavailable"
                    }"
                )

                HorizontalDivider()

                Text(
                    "Charging Records",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Charging readings: $chargingRecords")
                Text("Not charging readings: $dischargingRecords")

                HorizontalDivider()

                Text(
                    "Recent Battery History",
                    style = MaterialTheme.typography.titleMedium
                )

                history.take(20).forEach { item ->
                    BatteryHistoryRow(item)
                    HorizontalDivider()
                }

                Text(
                    "These are recorded samples, not exact charge-cycle counts or battery capacity measurements. Results depend on when the app saved readings.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BatteryHistoryRow(item: BatterySnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "${item.percentage}% battery",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            DateFormat.getDateTimeInstance().format(Date(item.timestamp)),
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Temperature: ${
                if (item.temperature >= 0f)
                    "${item.temperature} °C"
                else "Unavailable"
            } • ${if (item.charging) "Charging" else "Not charging"}"
        )
    }
}
