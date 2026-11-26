package com.anb.devicemanager.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.InstalledApp

enum class AppFilter {
    ALL, SYSTEM, USER
}

@Composable
fun AppManagerPanel(
    context: Context,
    apps: List<InstalledApp>
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }

    val filteredApps = apps.filter { app ->
        val matchesSearch =
            app.name.contains(search, ignoreCase = true) ||
            app.packageName.contains(search, ignoreCase = true)

        val matchesFilter = when (filter) {
            AppFilter.ALL -> true
            AppFilter.SYSTEM -> app.systemApp
            AppFilter.USER -> !app.systemApp
        }

        matchesSearch && matchesFilter
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Advanced App Manager",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search installed apps") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.name) }
                )
            }
        }

        Text("${filteredApps.size} applications")

        filteredApps.forEach { app ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        app.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (app.systemApp) "System App" else "User App",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                context.packageManager
                                    .getLaunchIntentForPackage(
                                        app.packageName
                                    )?.let {
                                        context.startActivity(it)
                                    }
                            }
                        ) {
                            Text("Open")
                        }

                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    android.provider.Settings
                                        .ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${app.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Text("App Info")
                        }

                        if (!app.systemApp) {
                            TextButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_DELETE,
                                        Uri.parse("package:${app.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Uninstall")
                            }
                        }
                    }
                }
            }
        }
    }
}
