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
import java.text.DateFormat
import java.util.Date

@Composable
fun NetworkExportPanel(context: Context) {
    var message by remember {
        mutableStateOf("Export saved network snapshots to CSV.")
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) {
            message = "Export cancelled."
        } else {
            message = runCatching {
                val prefs = context.getSharedPreferences(
                    "anb_network_history",
                    Context.MODE_PRIVATE
                )

                val json = JSONArray(
                    prefs.getString("records", "[]") ?: "[]"
                )

                val rows = mutableListOf(
                    listOf(
                        "Timestamp",
                        "Connection Type",
                        "Received Bytes",
                        "Transmitted Bytes",
                        "Internet Validated",
                        "Metered"
                    )
                )

                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    rows.add(
                        listOf(
                            DateFormat.getDateTimeInstance().format(
                                Date(item.getLong("timestamp"))
                            ),
                            item.optString("type"),
                            item.optLong("received").toString(),
                            item.optLong("transmitted").toString(),
                            item.optBoolean("validated").toString(),
                            item.optBoolean("metered").toString()
                        )
                    )
                }

                val csv = rows.joinToString("\n") { row ->
                    row.joinToString(",") { value ->
                        "\"" + value.replace("\"", "\"\"") + "\""
                    }
                }

                context.contentResolver.openOutputStream(uri)
                    ?.bufferedWriter()
                    ?.use { it.write(csv) }
                    ?: throw IllegalStateException(
                        "Unable to write selected file"
                    )

                "Export successful: ${json.length()} snapshots saved."
            }.getOrElse {
                "Export failed: ${it.message ?: "Unknown error"}"
            }
        }
    }

    val snapshotCount = remember {
        runCatching {
            JSONArray(
                context.getSharedPreferences(
                    "anb_network_history",
                    Context.MODE_PRIVATE
                ).getString("records", "[]") ?: "[]"
            ).length()
        }.getOrDefault(0)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network History Export",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Saved snapshots: $snapshotCount")
            Text("Format: CSV")
            Text("Data: timestamp, connection, traffic counters and network status")

            Button(
                onClick = {
                    launcher.launch("ANB_Network_History.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Network History")
            }

            Text(
                message,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Export is generated locally. No cloud or third-party service is used.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
