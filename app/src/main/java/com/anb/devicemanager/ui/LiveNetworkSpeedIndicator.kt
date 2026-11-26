package com.anb.devicemanager.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anb.devicemanager.monitoring.readNetworkSignal
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LiveNetworkSpeedIndicator(context: Context) {
    var download by remember { mutableLongStateOf(0L) }
    var upload by remember { mutableLongStateOf(0L) }
    var available by remember { mutableStateOf(false) }
    var signal by remember {
        mutableStateOf(readNetworkSignal(context))
    }

    var phonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        phonePermission = granted
        signal = readNetworkSignal(context)
    }

    LaunchedEffect(Unit) {
        var oldRx = TrafficStats.getTotalRxBytes()
        var oldTx = TrafficStats.getTotalTxBytes()
        var oldTime = SystemClock.elapsedRealtime()

        while (true) {
            delay(1000L)

            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - oldTime

            val valid = oldRx >= 0L &&
                oldTx >= 0L &&
                rx >= oldRx &&
                tx >= oldTx &&
                elapsed > 0L

            download = if (valid) {
                (rx - oldRx) * 1000L / elapsed
            } else 0L

            upload = if (valid) {
                (tx - oldTx) * 1000L / elapsed
            } else 0L

            available = valid
            signal = readNetworkSignal(context)

            oldRx = rx
            oldTx = tx
            oldTime = now
        }
    }

    fun speed(value: Long): String {
        return if (value >= 1_000_000L) {
            String.format(
                Locale.US,
                "%.2f Mbps",
                value * 8.0 / 1_000_000.0
            )
        } else if (value >= 1024L) {
            String.format(
                Locale.US,
                "%.1f KB/s",
                value / 1024.0
            )
        } else {
            "$value B/s"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "↓ ${if (available) speed(download) else "--"}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "↑ ${if (available) speed(upload) else "--"}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    signal.connectionType,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Signal",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (signal.signalLevel != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            repeat(4) { index ->
                                Surface(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height((6 + index * 3).dp),
                                    color = if (index < signal.signalLevel) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {}
                            }
                        }
                    } else {
                        Text(
                            "N/A",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        signal.signalLabel,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text(
                    if (signal.validated) "Internet: Connected"
                    else "Internet: Not validated",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VPN: ${if (signal.vpnActive) "On" else "Off"}  •  Metered: ${if (signal.metered) "Yes" else "No"}",
                    style = MaterialTheme.typography.labelSmall
                )

                if (signal.connectionType == "Mobile" && !phonePermission) {
                    TextButton(
                        onClick = {
                            permissionLauncher.launch(
                                Manifest.permission.READ_PHONE_STATE
                            )
                        }
                    ) {
                        Text("Enable signal")
                    }
                }
            }
        }
    }
}
