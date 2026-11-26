package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.readNetworkSignal
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class SignalSnapshot(
    val time: Long,
    val connection: String,
    val level: Int,
    val label: String,
    val validated: Boolean
)

private fun loadSignalHistory(context: Context): List<SignalSnapshot> {
    val raw = context.getSharedPreferences(
        "anb_signal_history",
        Context.MODE_PRIVATE
    ).getString("records", "[]") ?: "[]"

    return try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            SignalSnapshot(
                time = item.optLong("time"),
                connection = item.optString("connection", "Unknown"),
                level = item.optInt("level", -1),
                label = item.optString("label", "N/A"),
                validated = item.optBoolean("validated", false)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveSignalHistory(
    context: Context,
    records: List<SignalSnapshot>
) {
    val array = JSONArray()
    records.takeLast(200).forEach { record ->
        array.put(
            JSONObject().apply {
                put("time", record.time)
                put("connection", record.connection)
                put("level", record.level)
                put("label", record.label)
                put("validated", record.validated)
            }
        )
    }

    context.getSharedPreferences(
        "anb_signal_history",
        Context.MODE_PRIVATE
    ).edit().putString("records", array.toString()).apply()
}

@Composable
fun NetworkSignalHistoryPanel(context: Context) {
    var records by remember {
        mutableStateOf(loadSignalHistory(context))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val signal = readNetworkSignal(context)
            val snapshot = SignalSnapshot(
                time = System.currentTimeMillis(),
                connection = signal.connectionType,
                level = signal.signalLevel ?: -1,
                label = signal.signalLabel,
                validated = signal.validated
            )

            val latest = records.lastOrNull()
            val changed = latest == null ||
                latest.connection != snapshot.connection ||
                latest.level != snapshot.level ||
                latest.validated != snapshot.validated ||
                System.currentTimeMillis() - latest.time >= 30_000L

            if (changed) {
                records = (records + snapshot).takeLast(200)
                saveSignalHistory(context, records)
            }

            delay(30_000L)
        }
    }

    val known = records.filter { it.level in 0..4 }
    val weakCount = known.count { it.level <= 1 }
    val average = if (known.isNotEmpty()) {
        known.map { it.level }.average()
    } else null
    val validatedCount = records.count { it.validated }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Network Signal History",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Local signal snapshots • 30-second sampling",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Records")
                    Text(
                        "${records.size}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Column {
                    Text("Average signal")
                    Text(
                        average?.let {
                            String.format(Locale.US, "%.1f / 4", it)
                        } ?: "N/A",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Column {
                    Text("Weak samples")
                    Text(
                        "$weakCount",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Text("Signal quality trend")
            if (known.size >= 2) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val points = known.takeLast(40)
                    val stepX = size.width / (points.size - 1).toFloat()
                    val path = Path()

                    points.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = size.height -
                            (point.level.coerceIn(0, 4) / 4f * size.height)

                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = androidx.compose.ui.graphics.Color(0xFF246BCE),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx()
                        )
                    )

                    points.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = size.height -
                            (point.level.coerceIn(0, 4) / 4f * size.height)
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color(0xFF246BCE),
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
                Text(
                    "Recent ${known.takeLast(40).size} measurable signal samples",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("Collecting signal samples…")
            }

            HorizontalDivider()

            Text("Connection summary")
            Text("Internet validated samples: $validatedCount")
            Text("Weak signal samples: $weakCount")
            Text("Signal readings unavailable: ${records.count { it.level < 0 }}")

            HorizontalDivider()

            Text("Recent history")
            if (records.isEmpty()) {
                Text("No history recorded yet.")
            } else {
                records.takeLast(8).reversed().forEach { record ->
                    val time = SimpleDateFormat(
                        "dd MMM, HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date(record.time))

                    ListItem(
                        headlineContent = {
                            Text("${record.connection} • ${record.label}")
                        },
                        supportingContent = {
                            Text(
                                "$time • " +
                                    if (record.validated) "Validated"
                                    else "Not validated"
                            )
                        },
                        trailingContent = {
                            Text(
                                if (record.level >= 0) {
                                    "${record.level}/4"
                                } else "N/A"
                            )
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    records = emptyList()
                    saveSignalHistory(context, records)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Signal History")
            }
        }
    }
}
