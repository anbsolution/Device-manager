package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.DeviceSnapshot
import com.anb.devicemanager.monitoring.MonitoringDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private data class TimelineEvent(
    val id: Long,
    val timestamp: Long,
    val category: String,
    val title: String,
    val detail: String
)

@Composable
fun DeviceActivityTimelinePanel(context: Context) {
    var snapshots by remember { mutableStateOf(emptyList<DeviceSnapshot>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(refreshKey) {
        loading = true
        snapshots = withContext(Dispatchers.IO) {
            MonitoringDatabase.get(context)
                .snapshotDao()
                .getRecent()
        }
        loading = false
    }

    val events = remember(snapshots) {
        snapshots.map { snapshot ->
            val issues = mutableListOf<String>()

            if (snapshot.ramUsedPercent >= 90) {
                issues.add("High RAM usage: ${snapshot.ramUsedPercent}%")
            }
            if (snapshot.storageUsedPercent >= 90) {
                issues.add("High storage usage: ${snapshot.storageUsedPercent}%")
            }
            if (snapshot.batteryTemperature >= 45f) {
                issues.add("High battery temperature: ${snapshot.batteryTemperature} °C")
            }
            if (snapshot.thermalStatus in listOf(
                    "Severe", "Critical", "Emergency", "Shutdown"
                )
            ) {
                issues.add("Thermal status: ${snapshot.thermalStatus}")
            }

            TimelineEvent(
                id = snapshot.id,
                timestamp = snapshot.timestamp,
                category = "All",
                title = if (issues.isEmpty()) {
                    "Device monitoring check"
                } else {
                    "Device health warning"
                },
                detail = buildString {
                    append("RAM ${snapshot.ramUsedPercent}% • ")
                    append("Storage ${snapshot.storageUsedPercent}% • ")
                    append(
                        "Battery ${
                            if (snapshot.batteryPercent >= 0)
                                "${snapshot.batteryPercent}%"
                            else "N/A"
                        }"
                    )
                    appendLine()
                    append("Temperature: ${
                        if (snapshot.batteryTemperature >= 0f)
                            "${snapshot.batteryTemperature} °C"
                        else "N/A"
                    } • Thermal: ${snapshot.thermalStatus}")
                    if (issues.isNotEmpty()) {
                        appendLine()
                        append("Review: ${issues.joinToString("; ")}")
                    }
                }
            )
        }.sortedByDescending { it.timestamp }
    }

    val filtered = events.filter { event ->
        when (selectedFilter) {
            "RAM" -> event.detail.contains("RAM")
            "Storage" -> event.detail.contains("Storage")
            "Battery" -> event.detail.contains("Battery") ||
                event.detail.contains("Temperature")
            "Thermal" -> event.detail.contains("Thermal")
            else -> true
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Device Activity Timeline",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Events generated from locally saved monitoring snapshots",
                style = MaterialTheme.typography.bodySmall
            )

            Button(onClick = { refreshKey++ }) {
                Text("Refresh Timeline")
            }

            Text("Recorded checks: ${events.size}")

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "RAM", "Storage").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Battery", "Thermal").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            HorizontalDivider()

            if (loading) {
                CircularProgressIndicator()
            } else if (filtered.isEmpty()) {
                Text(
                    if (events.isEmpty()) {
                        "No history available. Enable Background Device Monitoring and wait for a check."
                    } else {
                        "No entries for this filter."
                    }
                )
            } else {
                filtered.forEach { event ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            DateFormat.getDateTimeInstance().format(
                                Date(event.timestamp)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(event.detail)
                    }
                    HorizontalDivider()
                }
            }

            Text(
                "Timeline entries represent saved periodic checks, not a complete Android system event log. Android may delay background checks.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
