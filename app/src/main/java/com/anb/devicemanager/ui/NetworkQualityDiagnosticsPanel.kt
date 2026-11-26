package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.NetworkDiagnosis
import com.anb.devicemanager.monitoring.NetworkQualityDiagnostics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NetworkQualityDiagnosticsPanel(context: Context) {
    var diagnosis by remember {
        mutableStateOf<NetworkDiagnosis?>(null)
    }
    var history by remember {
        mutableStateOf(NetworkQualityDiagnostics.history(context))
    }

    fun runDiagnosis() {
        val result = NetworkQualityDiagnostics.analyze(context)
        NetworkQualityDiagnostics.save(context, result)
        diagnosis = result
        history = NetworkQualityDiagnostics.history(context)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Quality Diagnostics",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Local network assessment and smart recommendations."
            )

            Button(
                onClick = { runDiagnosis() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Network Diagnosis")
            }

            diagnosis?.let { result ->
                HorizontalDivider()

                Text(
                    "Network Quality Score",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "${result.score}/100",
                    style = MaterialTheme.typography.headlineLarge
                )

                LinearProgressIndicator(
                    progress = { result.score / 100f },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Connection: ${result.connection}")
                Text(
                    "Signal: ${
                        if (result.signalLevel >= 0) {
                            "${result.signalLevel}/4"
                        } else "Unavailable"
                    }"
                )
                Text(
                    "Internet validated: ${
                        if (result.validated) "Yes" else "No"
                    }"
                )
                Text("Disconnections (24h): ${result.disconnects}")

                HorizontalDivider()

                Text(
                    "Detected Issues",
                    style = MaterialTheme.typography.titleMedium
                )

                if (result.issues.isEmpty()) {
                    Text("No issues detected by available indicators.")
                } else {
                    result.issues.forEach {
                        Text("• $it")
                    }
                }

                HorizontalDivider()

                Text(
                    "Smart Recommendations",
                    style = MaterialTheme.typography.titleMedium
                )

                result.recommendations.forEachIndexed { index, recommendation ->
                    Text("${index + 1}. $recommendation")
                }

                Text(
                    "Diagnosis time: ${
                        SimpleDateFormat(
                            "dd MMM yyyy, HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date(result.time))
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            } ?: Text("Run diagnosis to view your current network assessment.")

            HorizontalDivider()

            Text(
                "Previous Diagnosis History",
                style = MaterialTheme.typography.titleMedium
            )

            if (history.isEmpty()) {
                Text("No saved diagnosis history.")
            } else {
                history.take(10).forEach { item ->
                    ListItem(
                        headlineContent = {
                            Text("Quality score: ${item.second}/100")
                        },
                        supportingContent = {
                            Text(
                                SimpleDateFormat(
                                    "dd MMM, HH:mm",
                                    Locale.getDefault()
                                ).format(Date(item.first))
                            )
                        }
                    )
                }
            }

            Text(
                "Scores are heuristic estimates from local Android connectivity indicators, not a measured ISP speed or guaranteed network rating.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
