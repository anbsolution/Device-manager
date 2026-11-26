package com.anb.devicemanager.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DeviceIdentityPanel() {
    val manufacturer = Build.MANUFACTURER
        .replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val androidVersion = Build.VERSION.RELEASE
    val apiLevel = Build.VERSION.SDK_INT

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Device Identity",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "$manufacturer $model",
                style = MaterialTheme.typography.headlineSmall
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Android Version", style = MaterialTheme.typography.bodySmall)
                    Text(androidVersion, style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text("API Level", style = MaterialTheme.typography.bodySmall)
                    Text(apiLevel.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                "Device information is read locally from Android system properties.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
