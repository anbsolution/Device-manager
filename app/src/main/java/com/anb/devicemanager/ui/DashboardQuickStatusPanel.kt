package com.anb.devicemanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.BatteryInfo

@Composable
fun DashboardQuickStatusPanel(
    appCount: Int,
    battery: BatteryInfo?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Device at a glance",
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusMetric(
                modifier = Modifier.weight(1f),
                title = "Installed Apps",
                value = appCount.toString()
            )
            StatusMetric(
                modifier = Modifier.weight(1f),
                title = "Battery",
                value = battery?.percentage
                    ?.takeIf { it >= 0 }
                    ?.let { "$it%" } ?: "Unavailable"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusMetric(
                modifier = Modifier.weight(1f),
                title = "Power",
                value = battery?.let {
                    if (it.charging) "Charging" else "Not charging"
                } ?: "Unavailable"
            )
            StatusMetric(
                modifier = Modifier.weight(1f),
                title = "Battery Health",
                value = battery?.health ?: "Unavailable"
            )
        }
    }
}

@Composable
private fun StatusMetric(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
