package com.anb.devicemanager.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.StorageMonitor

private fun formatStorage(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    val mb = bytes / (1024.0 * 1024.0)
    return if (gb >= 1) "%.2f GB".format(gb)
    else "%.1f MB".format(mb)
}

@Composable
fun StorageIntelligencePanel(context: Context) {
    val monitor = remember { StorageMonitor(context) }
    var info by remember { mutableStateOf(monitor.getStorageInfo()) }
    var confirmCleanup by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Storage Intelligence",
                style = MaterialTheme.typography.titleLarge)

            Text("Used: ${formatStorage(info.usedBytes)}")
            Text("Available: ${formatStorage(info.freeBytes)}")
            Text("Total: ${formatStorage(info.totalBytes)}")

            LinearProgressIndicator(
                progress = { info.usedPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("${info.usedPercent}% Storage Used")

            HorizontalDivider()

            Text("ANB App Cache")
            Text(formatStorage(info.appCacheBytes))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    info = monitor.getStorageInfo()
                    message = "Storage refreshed"
                }) {
                    Text("Refresh")
                }

                OutlinedButton(onClick = {
                    confirmCleanup = true
                }) {
                    Text("Clear App Cache")
                }
            }

            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            ) {
                Text("Open Android Storage Settings")
            }

            if (message.isNotBlank()) {
                Text(message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (confirmCleanup) {
        AlertDialog(
            onDismissRequest = { confirmCleanup = false },
            title = { Text("Clear ANB App Cache?") },
            text = {
                Text("Only temporary cache files belonging to ANB Device Manager will be cleared. App data and other apps will not be deleted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val success = monitor.clearAppCache()
                    info = monitor.getStorageInfo()
                    message = if (success) "ANB app cache cleared"
                    else "Some cache files could not be cleared"
                    confirmCleanup = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmCleanup = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
