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
import com.anb.devicemanager.monitoring.*

@Composable
fun ReportExportPanel(
    context: Context,
    usageAccess: Boolean
) {
    var message by remember { mutableStateOf("Choose Export to create a report.") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri == null) {
            message = "Export cancelled."
        } else {
            message = try {
                val csv = createDeviceReport(context, usageAccess)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()
                    ?.use { it.write(csv) }
                    ?: throw IllegalStateException("Unable to open selected file")
                "Report exported successfully."
            } catch (e: Exception) {
                "Export failed: ${e.message ?: "Unknown error"}"
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Reports & Export Center",
                style = MaterialTheme.typography.titleLarge
            )

            Text("Export a local device report in CSV format.")

            Text("Report includes:")
            Text("• Device and Android diagnostics")
            Text("• Available hardware sensors")
            Text("• Saved battery history")
            Text("• App usage summary (when Usage Access is enabled)")

            Button(
                onClick = {
                    launcher.launch("ANB_Device_Report.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Device Report")
            }

            Text(
                message,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "The report is generated locally. Choose where to save it using Android's document picker.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun createDeviceReport(
    context: Context,
    usageAccess: Boolean
): String {
    val diagnostics = DeviceDiagnosticsMonitor(context).getDiagnostics()
    val batteryHistory = BatteryHistoryStore(context).getHistory()
    val usage = if (usageAccess) {
        UsageMonitor(context).getUsageForDays(30)
    } else {
        emptyList()
    }

    val rows = mutableListOf<List<String>>()

    fun add(vararg values: Any?) {
        rows.add(values.map { it?.toString() ?: "" })
    }

    add("ANB DEVICE MANAGER REPORT")
    add("Generated", System.currentTimeMillis())
    add("")
    add("DEVICE DIAGNOSTICS")
    add("Manufacturer", diagnostics.manufacturer)
    add("Model", diagnostics.model)
    add("Android Version", diagnostics.androidVersion)
    add("SDK", diagnostics.sdkVersion)
    add("Security Patch", diagnostics.securityPatch)
    add("Kernel", diagnostics.kernelVersion)
    add("Processor Cores", diagnostics.processorCores)
    add("Uptime Hours", diagnostics.uptimeHours)
    add("Screen Resolution", "${diagnostics.screenWidthPx}x${diagnostics.screenHeightPx}")
    add("Density DPI", diagnostics.densityDpi)
    add("")
    add("AVAILABLE SENSORS")
    add("Name", "Vendor", "Type", "Wake-up")
    diagnostics.sensors.forEach {
        add(it.name, it.vendor, it.type, it.wakeUp)
    }
    add("")
    add("BATTERY HISTORY")
    add("Timestamp", "Percentage", "Temperature C", "Charging")
    batteryHistory.forEach {
        add(it.timestamp, it.percentage, it.temperature, it.charging)
    }
    add("")
    add("APP USAGE - LAST 30 DAYS")
    add("Package", "Usage Milliseconds", "Last Used Timestamp")
    if (usageAccess) {
        usage.forEach {
            add(it.packageName, it.usageTime, it.lastUsed)
        }
    } else {
        add("Usage Access not enabled")
    }

    return rows.joinToString("\n") { row ->
        row.joinToString(",") { value ->
            "\"" + value.replace("\"", "\"\"") + "\""
        }
    }
}
