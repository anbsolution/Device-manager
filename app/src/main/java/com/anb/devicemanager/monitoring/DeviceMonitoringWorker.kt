package com.anb.devicemanager.monitoring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DeviceMonitoringWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val performance = PerformanceMonitor(applicationContext).getMetrics()
            val storage = StorageMonitor(applicationContext).getStorageInfo()
            val battery = BatteryMonitor(applicationContext).getBatteryInfo()

            val ramPercent = if (performance.totalRam > 0) {
                ((performance.totalRam - performance.availableRam) * 100 /
                    performance.totalRam).toInt().coerceIn(0, 100)
            } else 0

            val snapshot = DeviceSnapshot(
                timestamp = System.currentTimeMillis(),
                ramUsedPercent = ramPercent,
                storageUsedPercent = storage.usedPercent,
                batteryPercent = battery?.percentage ?: -1,
                batteryTemperature = battery?.temperature ?: -1f,
                charging = battery?.charging ?: false,
                thermalStatus = performance.thermalStatus
            )

            MonitoringDatabase.get(applicationContext)
                .snapshotDao()
                .insert(snapshot)

            DeviceAlertNotifier.evaluateAndNotify(
                context = applicationContext,
                ramPercent = ramPercent,
                storagePercent = storage.usedPercent,
                batteryTemperature = battery?.temperature ?: -1f,
                thermalStatus = performance.thermalStatus
            )

            applicationContext.getSharedPreferences(
                "anb_device_monitoring",
                Context.MODE_PRIVATE
            ).edit()
                .putLong("last_check", snapshot.timestamp)
                .apply()

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
