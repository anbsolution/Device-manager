package com.anb.devicemanager.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.BatteryHistoryStore

@Composable
fun LocalBackupPanel(context: Context) {
    val store = remember { BatteryHistoryStore(context) }

    var message by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            message = "Backup cancelled."
        } else {
            message = try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()
                    ?.use { it.write(store.exportBackup()) }
                    ?: throw IllegalStateException("Cannot write backup")
                "Battery history backup created."
            } catch (e: Exception) {
                "Backup failed: ${e.message ?: "Unknown error"}"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            message = "Restore cancelled."
        } else {
            message = try {
                val raw = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Cannot read backup")

                val count = store.restoreBackup(raw)
                "Restore successful: $count battery records restored."
            } catch (e: Exception) {
                "Restore failed. Existing history was not changed. ${e.message ?: ""}"
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Local Backup & Restore",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Saved battery records: ${store.getHistory().size}")

            Button(
                onClick = {
                    exportLauncher.launch("ANB_Battery_Backup.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Backup")
            }

            OutlinedButton(
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Backup")
            }

            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodySmall)
            }

            Text(
                "Restore replaces the current battery history with the selected backup. Keep a backup before restoring.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore Battery Backup?") },
            text = {
                Text(
                    "Select a valid ANB backup file. Current battery history will be replaced after the file is validated."
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
