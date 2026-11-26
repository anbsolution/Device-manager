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

object DeviceAlertNotifier {
    private const val CHANNEL_ID = "anb_device_health_alerts"
    private const val COOLDOWN = 24L * 60L * 60L * 1000L

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Health Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts about device resource and thermal conditions"
            }

            manager.createNotificationChannel(channel)
        }
    }

    fun evaluateAndNotify(
        context: Context,
        ramPercent: Int,
        storagePercent: Int,
        batteryTemperature: Float,
        thermalStatus: String
    ) {
        val prefs = context.getSharedPreferences(
            "anb_device_monitoring",
            Context.MODE_PRIVATE
        )

        if (!prefs.getBoolean("alerts_enabled", false)) return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)

        val alerts = mutableListOf<Pair<String, String>>()

        if (ramPercent >= 90) {
            alerts += "ram" to "RAM usage is high: $ramPercent%"
        }

        if (storagePercent >= 90) {
            alerts += "storage" to "Storage usage is high: $storagePercent%"
        }

        if (batteryTemperature >= 45f) {
            alerts += "battery_temp" to
                "Battery temperature is ${batteryTemperature}°C"
        }

        if (thermalStatus in listOf("Severe", "Critical", "Emergency", "Shutdown")) {
            alerts += "thermal" to "Device thermal status: $thermalStatus"
        }

        val now = System.currentTimeMillis()

        alerts.forEachIndexed { index, alert ->
            val key = "alert_last_${alert.first}"
            val previous = prefs.getLong(key, 0L)

            if (now - previous >= COOLDOWN) {
                val notification = NotificationCompat.Builder(
                    context,
                    CHANNEL_ID
                )
                    .setSmallIcon(com.anb.devicemanager.R.mipmap.ic_launcher)
                    .setContentTitle("ANB Device Manager Alert")
                    .setContentText(alert.second)
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(alert.second)
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(context).notify(
                    alert.first.hashCode(),
                    notification
                )

                prefs.edit().putLong(key, now).apply()
            }
        }
    }
}
