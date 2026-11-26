package com.anb.devicemanager.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anb.devicemanager.monitoring.DeviceAlertNotifier

@Composable
fun DeviceAlertPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_device_monitoring",
            Context.MODE_PRIVATE
        )
    }

    var enabled by remember {
        mutableStateOf(prefs.getBoolean("alerts_enabled", false))
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
        prefs.edit().putBoolean("alerts_enabled", granted).apply()
    }

    LaunchedEffect(Unit) {
        DeviceAlertNotifier.createChannel(context)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Device Health Alerts",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Get local alerts for high RAM, storage, battery temperature and thermal status."
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (enabled) "Alerts Enabled" else "Alerts Disabled")
                    Text(
                        "Duplicate alerts have a 24-hour cooldown.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            enabled = false
                            prefs.edit().putBoolean("alerts_enabled", false).apply()
                        } else if (Build.VERSION.SDK_INT >= 33 && !permissionGranted) {
                            permissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            enabled = true
                            prefs.edit().putBoolean("alerts_enabled", true).apply()
                        }
                    }
                )
            }

            if (Build.VERSION.SDK_INT >= 33 && !permissionGranted) {
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                ) {
                    Text("Allow Notifications")
                }
            }

            Text(
                "Alerts are generated locally. No internet connection is used.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
