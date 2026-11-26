package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.DeviceMonitoringScheduler
import java.text.DateFormat
import java.util.Date

@Composable
fun BackgroundMonitoringPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_device_monitoring",
            Context.MODE_PRIVATE
        )
    }

    var enabled by remember {
        mutableStateOf(prefs.getBoolean("monitoring_enabled", false))
    }

    val lastCheck = remember {
        prefs.getLong("last_check", 0L)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Background Device Monitoring",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (enabled) "Monitoring Enabled" else "Monitoring Disabled")
                    Text(
                        "Automatic check approximately every 6 hours",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        enabled = checked
                        prefs.edit()
                            .putBoolean("monitoring_enabled", checked)
                            .apply()

                        if (checked) {
                            DeviceMonitoringScheduler.enable(context)
                        } else {
                            DeviceMonitoringScheduler.disable(context)
                        }
                    }
                )
            }

            Text(
                if (lastCheck > 0L) {
                    "Last check: ${
                        DateFormat.getDateTimeInstance().format(Date(lastCheck))
                    }"
                } else {
                    "Last check: Not available yet"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                "Data is stored locally on this device. Android may delay background checks.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
