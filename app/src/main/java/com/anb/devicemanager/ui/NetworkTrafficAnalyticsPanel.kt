package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import java.text.DateFormat
import java.util.Date

private data class TrafficSample(
    val time: Long,
    val received: Long,
    val transmitted: Long
)

private fun trafficSize(bytes: Long): String {
    if (bytes < 0) return "Unavailable"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024.0)
    } else {
        "%.2f MB".format(mb)
    }
}

@Composable
fun NetworkTrafficAnalyticsPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_network_history",
            Context.MODE_PRIVATE
        )
    }

    var refreshKey by remember { mutableIntStateOf(0) }

    val samples = remember(refreshKey) {
        runCatching {
            val json = JSONArray(
                prefs.getString("records", "[]") ?: "[]"
            )

            (0 until json.length()).map { index ->
                val item = json.getJSONObject(index)
                TrafficSample(
                    time = item.getLong("timestamp"),
                    received = item.getLong("received"),
                    transmitted = item.getLong("transmitted")
                )
            }.sortedBy { it.time }
        }.getOrDefault(emptyList())
    }

    val deltas = samples.zipWithNext().mapNotNull { (old, new) ->
        val seconds = (new.time - old.time) / 1000L
        val rx = new.received - old.received
        val tx = new.transmitted - old.transmitted

        if (seconds <= 0 || rx < 0 || tx < 0) {
            null
        } else {
            Triple(seconds, rx, tx)
        }
    }

    val totalReceived = deltas.sumOf { it.second }
    val totalTransmitted = deltas.sumOf { it.third }
    val totalSeconds = deltas.sumOf { it.first }

    val averageDownload = if (totalSeconds > 0) {
        totalReceived / totalSeconds
    } else 0L

    val averageUpload = if (totalSeconds > 0) {
        totalTransmitted / totalSeconds
    } else 0L

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Traffic Analytics",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Saved samples: ${samples.size}")
            Text("Valid intervals: ${deltas.size}")

            HorizontalDivider()

            Text(
                "Recorded Data Transfer",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Downloaded: ${trafficSize(totalReceived)}")
            Text("Uploaded: ${trafficSize(totalTransmitted)}")

            HorizontalDivider()

            Text(
                "Average Transfer Rate",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Download: ${trafficSize(averageDownload)}/sec")
            Text("Upload: ${trafficSize(averageUpload)}/sec")

            HorizontalDivider()

            Text(
                "Recent Sample History",
                style = MaterialTheme.typography.titleMedium
            )

            if (samples.isEmpty()) {
                Text("Save network snapshots to start analytics.")
            } else {
                samples.takeLast(10).asReversed().forEach { sample ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            DateFormat.getDateTimeInstance().format(
                                Date(sample.time)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Received counter: ${trafficSize(sample.received)}")
                        Text("Transmitted counter: ${trafficSize(sample.transmitted)}")
                    }
                    HorizontalDivider()
                }
            }

            OutlinedButton(
                onClick = { refreshKey++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Analytics")
            }

            Text(
                "Analytics use differences between saved app-UID traffic counters. Counter resets, device restarts, or missing samples can affect results. Average rates are estimates across valid saved intervals.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
