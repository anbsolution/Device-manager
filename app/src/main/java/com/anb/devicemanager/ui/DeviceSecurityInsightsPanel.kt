package com.anb.devicemanager.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.PermissionMonitor

@Composable
fun DeviceSecurityInsightsPanel(context: Context) {
    var refreshKey by remember { mutableIntStateOf(0) }

    val apps = remember(refreshKey) {
        context.packageManager.getInstalledApplications(0)
    }

    val reports = remember(refreshKey) {
        PermissionMonitor(context).getPermissionReport()
    }

    val userApps = apps.filter {
        (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }

    val systemApps = apps.size - userApps.size

    val cameraCount = reports.count { app ->
        app.permissions.any {
            it.name.contains("CAMERA") && it.granted
        }
    }

    val microphoneCount = reports.count { app ->
        app.permissions.any {
            it.name.contains("RECORD_AUDIO") && it.granted
        }
    }

    val locationCount = reports.count { app ->
        app.permissions.any {
            (it.name.contains("ACCESS_FINE_LOCATION") ||
                it.name.contains("ACCESS_COARSE_LOCATION")) &&
                it.granted
        }
    }

    val contactsCount = reports.count { app ->
        app.permissions.any {
            (it.name.contains("READ_CONTACTS") ||
                it.name.contains("WRITE_CONTACTS")) &&
                it.granted
        }
    }

    val highPermissionApps = reports.filter {
        it.grantedCount >= 5
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Device Security & App Insights",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Local analysis • Read-only • No cloud connection",
                style = MaterialTheme.typography.bodySmall
            )

            Button(onClick = { refreshKey++ }) {
                Text("Refresh Security Analysis")
            }

            HorizontalDivider()

            Text("Installed applications", style = MaterialTheme.typography.titleMedium)
            Text("Total apps: ${apps.size}")
            Text("User apps: ${userApps.size}")
            Text("System apps: $systemApps")

            HorizontalDivider()

            Text("Granted permission overview", style = MaterialTheme.typography.titleMedium)
            Text("Camera access: $cameraCount apps")
            Text("Microphone access: $microphoneCount apps")
            Text("Location access: $locationCount apps")
            Text("Contacts access: $contactsCount apps")

            HorizontalDivider()

            Text("Permission Review", style = MaterialTheme.typography.titleMedium)
            Text(
                "${highPermissionApps.size} apps have five or more granted dangerous permissions."
            )

            if (highPermissionApps.isEmpty()) {
                Text("No apps matched this permission-count review.")
            } else {
                highPermissionApps.take(10).forEach { app ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            app.appName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${app.grantedCount} granted permissions",
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${app.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }) {
                            Text("Review App Settings")
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                "Important: These counts are informational, not a malware scan or proof of misuse. Apps may need permissions for legitimate features. Review permissions according to your needs.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
