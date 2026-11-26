package com.anb.devicemanager.monitoring

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NetworkSignalAlertMonitor {
    private const val CHANNEL_ID = "anb_network_signal_alerts"
    private const val COOLDOWN_MS = 15L * 60L * 1000L

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Network Signal Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Local alerts for network signal and connectivity changes"
                }
            )
        }
    }

    fun evaluate(context: Context, signal: NetworkSignalInfo) {
        val prefs = context.getSharedPreferences(
            "anb_network_signal_alerts",
            Context.MODE_PRIVATE
        )

        if (!prefs.getBoolean("enabled", false)) return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)

        val threshold = prefs.getInt("weak_threshold", 1).coerceIn(0, 3)
        val previousConnection = prefs.getString("last_connection", null)
        val previousValidated = prefs.getBoolean("last_validated", signal.validated)
        val previousLevel = prefs.getInt("last_level", -1)

        val alerts = mutableListOf<Pair<String, String>>()

        if (signal.connectionType == "Offline" && previousConnection != "Offline") {
            alerts += "connection_lost" to "Network connection lost"
        }

        if (
            signal.connectionType != "Offline" &&
            previousConnection == "Offline"
        ) {
            alerts += "connection_restored" to
                "Network connection restored: ${signal.connectionType}"
        }

        val level = signal.signalLevel
        if (level != null && level <= threshold && previousLevel > threshold) {
            alerts += "weak_signal" to
                "Weak ${signal.connectionType} signal: ${signal.signalLabel}"
        }

        if (level != null && level >= 3 && previousLevel in 0..threshold) {
            alerts += "signal_recovered" to
                "${signal.connectionType} signal improved: ${signal.signalLabel}"
        }

        if (!signal.validated && previousValidated && signal.connectionType != "Offline") {
            alerts += "internet_unvalidated" to
                "Network connected, but internet validation is unavailable"
        }

        if (signal.validated && !previousValidated) {
            alerts += "internet_restored" to "Internet connectivity validated again"
        }

        val now = System.currentTimeMillis()

        alerts.forEach { (key, message) ->
            val lastTime = prefs.getLong("last_alert_$key", 0L)
            if (now - lastTime >= COOLDOWN_MS) {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(com.anb.devicemanager.R.mipmap.ic_launcher)
                    .setContentTitle("ANB Network Alert")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(context).notify(
                    key.hashCode(),
                    notification
                )
                prefs.edit().putLong("last_alert_$key", now).apply()
            }
        }

        prefs.edit()
            .putString("last_connection", signal.connectionType)
            .putBoolean("last_validated", signal.validated)
            .putInt("last_level", level ?: -1)
            .apply()
    }
}
