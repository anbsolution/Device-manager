package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.DeviceDiagnosticsInfo
import com.anb.devicemanager.monitoring.DeviceDiagnosticsMonitor

@Composable
fun DeviceDiagnosticsPanel(context: Context) {
    var info by remember {
        mutableStateOf(
            DeviceDiagnosticsMonitor(context).getDiagnostics()
        )
    }
    var showSensors by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                "Hardware & Device Diagnostics",
                style = MaterialTheme.typography.titleLarge
            )

            DiagnosticRow("Manufacturer", info.manufacturer)
            DiagnosticRow("Model", info.model)
            DiagnosticRow("Android", info.androidVersion)
            DiagnosticRow("SDK Version", info.sdkVersion.toString())
            DiagnosticRow("Security Patch", info.securityPatch)
            DiagnosticRow("Kernel", info.kernelVersion)
            DiagnosticRow("Processor Cores", info.processorCores.toString())
            DiagnosticRow("Device Uptime", "${info.uptimeHours} hours")
            DiagnosticRow(
                "Display",
                "${info.screenWidthPx} × ${info.screenHeightPx} px"
            )
            DiagnosticRow("Density", "${info.densityDpi} dpi")
            DiagnosticRow("Available Sensors", info.sensors.size.toString())

            OutlinedButton(
                onClick = { showSensors = !showSensors },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showSensors) "Hide Sensors" else "View Sensors")
            }

            if (showSensors) {
                info.sensors.forEach { sensor ->
                    HorizontalDivider()
                    Text(
                        sensor.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${sensor.type} • ${sensor.vendor}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        if (sensor.wakeUp) "Wake-up sensor" else "Non wake-up sensor",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            TextButton(
                onClick = {
                    info = DeviceDiagnosticsMonitor(context).getDiagnostics()
                }
            ) {
                Text("Refresh Diagnostics")
            }

            Text(
                "Information is read locally from Android system APIs. No IMEI, serial number or sensor readings are collected.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
