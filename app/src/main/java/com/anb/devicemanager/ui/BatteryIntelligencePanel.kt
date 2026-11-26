package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatteryIntelligencePanel(context: Context) {
    val monitor = remember { BatteryMonitor(context) }
    val historyStore = remember { BatteryHistoryStore(context) }

    var info by remember { mutableStateOf<BatteryInfo?>(null) }
    var history by remember {
        mutableStateOf(emptyList<BatterySnapshot>())
    }

    fun refresh() {
        info = monitor.getBatteryInfo()
        info?.let { historyStore.save(it) }
        history = historyStore.getHistory()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Battery Intelligence",
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = { refresh() }) {
                Text("Refresh")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Battery Level")
                Text(
                    info?.let {
                        if (it.percentage >= 0) "${it.percentage}%" else "Unavailable"
                    } ?: "Loading",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    if (info?.charging == true) "Charging" else "Discharging / Not charging"
                )
                Text("Battery condition: ${info?.health ?: "Unavailable"}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Electrical & Thermal Data")
                Text(
                    "Temperature: ${
                        info?.let {
                            if (it.temperature >= 0) "${it.temperature} °C"
                            else "Unavailable"
                        } ?: "Unavailable"
                    }"
                )
                Text("Voltage: ${info?.voltageMv?.takeIf { it > 0 }?.let { "$it mV" } ?: "Unavailable"}")
                Text("Charge counter: ${info?.chargeCounterUah?.let { "$it µAh" } ?: "Unsupported"}")
                Text("Current now: ${info?.currentNowUa?.let { "$it µA" } ?: "Unsupported"}")
                Text("Average current: ${info?.currentAverageUa?.let { "$it µA" } ?: "Unsupported"}")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Battery History")
                Text("${history.size} local snapshots saved")
                if (history.size >= 2) {
                    val first = history.first()
                    val last = history.last()
                    Text("Recorded change: ${last.percentage - first.percentage}%")
                    Text("From first saved sample to latest sample")
                } else {
                    Text("More samples are needed for history analysis.")
                }
            }
        }

        Text("Recent Samples", style = MaterialTheme.typography.titleMedium)

        history.takeLast(10).reversed().forEach { sample ->
            ListItem(
                headlineContent = {
                    Text("${sample.percentage}% • ${if (sample.charging) "Charging" else "Not charging"}")
                },
                supportingContent = {
                    Text(
                        SimpleDateFormat(
                            "dd MMM, HH:mm",
                            Locale.getDefault()
                        ).format(Date(sample.timestamp)) +
                        " • ${sample.temperature} °C"
                    )
                }
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Per-App Battery Usage")
                Text(
                    "Not available through the standard app API on this device. " +
                    "No estimated per-app percentages are invented."
                )
            }
        }
    }
}
