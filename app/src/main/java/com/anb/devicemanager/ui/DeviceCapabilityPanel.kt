package com.anb.devicemanager.ui

import android.content.Context
import android.content.Intent
import android.content.pm.FeatureInfo
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Capability(
    val name: String,
    val feature: String
)

@Composable
fun DeviceCapabilityPanel(context: Context) {
    var refreshKey by remember { mutableIntStateOf(0) }

    val capabilities = remember(refreshKey) {
        val pm = context.packageManager

        listOf(
            Capability("Camera", "android.hardware.camera"),
            Capability("Front Camera", "android.hardware.camera.front"),
            Capability("Flash", "android.hardware.camera.flash"),
            Capability("Bluetooth", "android.hardware.bluetooth"),
            Capability("Bluetooth LE", "android.hardware.bluetooth_le"),
            Capability("GPS", "android.hardware.location.gps"),
            Capability("NFC", "android.hardware.nfc"),
            Capability("Wi-Fi", "android.hardware.wifi"),
            Capability("Microphone", "android.hardware.microphone"),
            Capability("Fingerprint", "android.hardware.fingerprint"),
            Capability("Accelerometer", "android.hardware.sensor.accelerometer"),
            Capability("Gyroscope", "android.hardware.sensor.gyroscope"),
            Capability("Compass", "android.hardware.sensor.compass"),
            Capability("Barometer", "android.hardware.sensor.barometer")
        ).map {
            it to pm.hasSystemFeature(it.feature)
        }
    }

    val available = capabilities.count { it.second }
    val unavailable = capabilities.size - available

    val featureList = remember(refreshKey) {
        context.packageManager.systemAvailableFeatures
            .filter { it.name != null }
            .mapNotNull { it.name }
            .sorted()
    }

    val report = remember(refreshKey, capabilities, featureList) {
        buildString {
            appendLine("ANB Device Manager - Capability Report")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE}")
            appendLine("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Available checked features: $available")
            appendLine("Unavailable checked features: $unavailable")
            appendLine()
            appendLine("Feature Status:")
            capabilities.forEach { (capability, isAvailable) ->
                appendLine("${capability.name}: ${if (isAvailable) "Available" else "Unavailable"}")
            }
            appendLine()
            appendLine("Additional system features:")
            featureList.forEach { appendLine(it) }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Device Capability & Compatibility",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Offline hardware and software feature inventory",
                style = MaterialTheme.typography.bodySmall
            )

            Text("Manufacturer: ${Build.MANUFACTURER}")
            Text("Model: ${Build.MODEL}")
            Text("Android: ${Build.VERSION.RELEASE}")
            Text("Supported ABI: ${Build.SUPPORTED_ABIS.joinToString()}")

            HorizontalDivider()

            Text("Feature Summary", style = MaterialTheme.typography.titleMedium)
            Text("Available: $available")
            Text("Unavailable: $unavailable")

            capabilities.forEach { (capability, isAvailable) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(capability.name, modifier = Modifier.weight(1f))
                    Text(if (isAvailable) "Available" else "Unavailable")
                }
            }

            HorizontalDivider()

            Text(
                "Additional system features: ${featureList.size}",
                style = MaterialTheme.typography.titleMedium
            )

            featureList.take(30).forEach {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }

            if (featureList.size > 30) {
                Text("Showing first 30 additional features.")
            }

            OutlinedButton(
                onClick = { refreshKey++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Capabilities")
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "ANB Device Capability Report")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Share Device Report")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share Capability Report")
            }

            Text(
                "Feature availability is reported by Android PackageManager. It does not guarantee that every hardware feature is fully functional.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
