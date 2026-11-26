package com.anb.devicemanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.PermissionMonitor
import com.anb.devicemanager.monitoring.AppPermissionInfo

@Composable
fun PermissionIntelligencePanel(context: Context) {
    var reports by remember {
        mutableStateOf(PermissionMonitor(context).getPermissionReport())
    }
    var search by remember { mutableStateOf("") }

    val filtered = reports.filter {
        it.appName.contains(search, true) ||
            it.packageName.contains(search, true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Privacy & Permission Intelligence",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${filtered.size} apps with dangerous permissions")
            TextButton(onClick = {
                reports = PermissionMonitor(context).getPermissionReport()
            }) {
                Text("Refresh")
            }
        }

        filtered.forEach { app ->
            PermissionAppCard(context, app)
        }

        if (filtered.isEmpty()) {
            Text("No matching permission records found.")
        }

        Text(
            "Permission records show declared dangerous permissions and their reported grant state. They do not prove when an app accessed a sensor.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionAppCard(
    context: Context,
    app: AppPermissionInfo
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(app.appName, style = MaterialTheme.typography.titleMedium)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)

            Text("Granted: ${app.grantedCount}  •  Denied: ${app.deniedCount}")

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide permissions" else "View permissions")
            }

            if (expanded) {
                app.permissions.forEach { permission ->
                    Text(
                        permission.name.substringAfterLast(".") +
                            if (permission.granted) " • Granted" else " • Denied"
                    )
                }
            }

            OutlinedButton(onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${app.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }) {
                Text("Open App Settings")
            }
        }
    }
}
