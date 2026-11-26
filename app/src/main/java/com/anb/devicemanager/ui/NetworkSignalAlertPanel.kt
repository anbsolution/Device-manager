package com.anb.devicemanager.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anb.devicemanager.monitoring.NetworkSignalAlertMonitor

@Composable
fun NetworkSignalAlertPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_network_signal_alerts",
            Context.MODE_PRIVATE
        )
    }

    var enabled by remember {
        mutableStateOf(prefs.getBoolean("enabled", false))
    }

    var threshold by remember {
        mutableFloatStateOf(
            prefs.getInt("weak_threshold", 1).toFloat()
        )
    }

    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        enabled = granted
        prefs.edit().putBoolean("enabled", granted).apply()
    }

    LaunchedEffect(Unit) {
        NetworkSignalAlertMonitor.createChannel(context)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Network Signal Alerts",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Receive local notifications for weak signal, connection loss, recovery and internet validation changes."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (enabled) "Alerts Enabled" else "Alerts Disabled")
                    Text(
                        "15-minute cooldown per alert type",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            enabled = false
                            prefs.edit().putBoolean("enabled", false).apply()
                        } else if (
                            Build.VERSION.SDK_INT >= 33 &&
                            !permissionGranted
                        ) {
                            permissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            enabled = true
                            prefs.edit().putBoolean("enabled", true).apply()
                        }
                    }
                )
            }

            Text("Weak signal threshold: ${threshold.toInt()}/4")

            Slider(
                value = threshold,
                onValueChange = {
                    threshold = it
                    prefs.edit().putInt(
                        "weak_threshold",
                        it.toInt()
                    ).apply()
                },
                valueRange = 0f..3f,
                steps = 2
            )

            Text(
                when (threshold.toInt()) {
                    0 -> "Alert only at very weak signal"
                    1 -> "Alert at very weak or weak signal"
                    2 -> "Alert at fair or weaker signal"
                    else -> "Alert at good or weaker signal"
                },
                style = MaterialTheme.typography.bodySmall
            )

            if (Build.VERSION.SDK_INT >= 33 && !permissionGranted) {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow Notifications")
                }
            }

            Text(
                "Monitoring runs while the app's signal monitor is active. Android notification permission and device notification settings apply.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
