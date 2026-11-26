package com.anb.devicemanager.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray

@Composable
fun NetworkBackupPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences(
            "anb_network_history",
            Context.MODE_PRIVATE
        )
    }

    var message by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        message = if (uri == null) {
            "Backup cancelled."
        } else {
            runCatching {
                val records = JSONArray(
                    prefs.getString("records", "[]") ?: "[]"
                )
                val backup = org.json.JSONObject().apply {
                    put("format", "ANB_NETWORK_BACKUP")
                    put("version", 1)
                    put("records", records)
                }

                context.contentResolver.openOutputStream(uri)
                    ?.bufferedWriter()
                    ?.use { it.write(backup.toString(2)) }
                    ?: throw IllegalStateException("Cannot write backup")

                "Network backup created: ${records.length()} snapshots."
            }.getOrElse {
                "Backup failed: ${it.message ?: "Unknown error"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        message = if (uri == null) {
            "Restore cancelled."
        } else {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IllegalStateException("Cannot read backup")

                val backup = org.json.JSONObject(raw)
                require(backup.optString("format") == "ANB_NETWORK_BACKUP") {
                    "Invalid backup format"
                }
                require(backup.optInt("version") == 1) {
                    "Unsupported backup version"
                }

                val records = backup.getJSONArray("records")
                require(records.length() <= 10000) {
                    "Backup contains too many records"
                }

                for (i in 0 until records.length()) {
                    val item = records.getJSONObject(i)
                    require(item.has("timestamp")) {
                        "Missing timestamp in record"
                    }
                    require(item.has("received") && item.has("transmitted")) {
                        "Missing traffic counters"
                    }
                    require(item.has("type")) {
                        "Missing connection type"
                    }
                }

                prefs.edit()
                    .putString("records", records.toString())
                    .commit()
                    .also { saved ->
                        if (!saved) throw IllegalStateException("Unable to save restored data")
                    }

                "Restore successful: ${records.length()} snapshots."
            }.getOrElse {
                "Restore failed. Existing history was preserved. ${it.message ?: ""}"
            }
        }
    }

    val count = remember(message) {
        runCatching {
            JSONArray(
                prefs.getString("records", "[]") ?: "[]"
            ).length()
        }.getOrDefault(0)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Backup & Restore",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Saved network snapshots: $count")

            Button(
                onClick = {
                    exportLauncher.launch("ANB_Network_Backup.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Network Backup")
            }

            OutlinedButton(
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Network Backup")
            }

            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "Backup and restore run locally. Restore replaces current network history.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore Network Backup?") },
            text = {
                Text(
                    "Current network history will be replaced by the selected valid backup."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    importLauncher.launch(
                        arrayOf("application/json", "text/json", "text/plain")
                    )
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmRestore = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
