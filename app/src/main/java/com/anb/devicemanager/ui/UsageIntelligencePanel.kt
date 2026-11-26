package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.UsageMonitor

@Composable
fun UsageIntelligencePanel(
    context: Context,
    usageAccess: Boolean,
    appNames: Map<String, String>
) {
    var selectedDays by remember { mutableIntStateOf(1) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val summary = remember(selectedDays, refreshKey, usageAccess) {
        if (usageAccess) {
            UsageMonitor(context).getSummary(selectedDays)
        } else null
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "App Usage Intelligence",
                style = MaterialTheme.typography.titleLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 7, 30).forEach { days ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { selectedDays = days },
                        label = {
                            Text(
                                when (days) {
                                    1 -> "24 Hours"
                                    7 -> "7 Days"
                                    else -> "30 Days"
                                }
                            )
                        }
                    )
                }
            }

            if (!usageAccess) {
                Text("Usage Access is required to display statistics.")
            } else if (summary != null) {
                Text(
                    "Total foreground usage: ${formatDuration(summary.totalUsageMs)}"
                )
                Text("Apps used: ${summary.appCount}")

                HorizontalDivider()

                Text(
                    "Top 10 Applications",
                    style = MaterialTheme.typography.titleMedium
                )

                summary.apps.take(10).forEachIndexed { index, app ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "${index + 1}. ${
                                appNames[app.packageName] ?: app.packageName
                            }"
                        )
                        Text(
                            "Usage: ${formatDuration(app.usageTime)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = {
                                (app.usageTime.toFloat() /
                                    (summary.apps.firstOrNull()?.usageTime
                                        ?: 1L).coerceAtLeast(1L))
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider()
                }

                if (summary.apps.isEmpty()) {
                    Text("No usage records found for this period.")
                }
            }

            TextButton(onClick = { refreshKey++ }) {
                Text("Refresh Usage")
            }

            Text(
                "Usage values are Android system estimates for foreground activity, not exact screen-on time.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val minutes = milliseconds / 60_000
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${remainingMinutes}m"
    }
}
