package com.anb.devicemanager.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun DeviceIdentityPanel() {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics

    val manufacturer = Build.MANUFACTURER
        .replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val androidVersion = Build.VERSION.RELEASE ?: "Unavailable"
    val apiLevel = Build.VERSION.SDK_INT
    val resolution = "${metrics.widthPixels} × ${metrics.heightPixels} px"
    val density = "${metrics.densityDpi} dpi"
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unavailable"
    val securityPatch = Build.VERSION.SECURITY_PATCH
        .takeIf { it.isNotBlank() } ?: "Unavailable"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Device Information & Diagnostics",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "$manufacturer $model",
                style = MaterialTheme.typography.headlineSmall
            )

            HorizontalDivider()

            DiagnosticRow("Android Version", androidVersion)
            DiagnosticRow("API Level", apiLevel.toString())
            DiagnosticRow("Screen Resolution", resolution)
            DiagnosticRow("Screen Density", density)
            DiagnosticRow("Primary Hardware ABI", abi)
            DiagnosticRow("Security Patch", securityPatch)

            HorizontalDivider()

            Text(
                "All information is read locally from Android system properties and display metrics.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
