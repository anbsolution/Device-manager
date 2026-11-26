package com.anb.devicemanager.monitoring

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

data class PerformanceInfo(
    val totalRam: Long,
    val availableRam: Long,
    val lowMemory: Boolean,
    val cpuCores: Int,
    val thermalStatus: String
) {
    val usedRam: Long get() = totalRam - availableRam
}

class PerformanceMonitor(private val context: Context) {

    fun getMetrics(): PerformanceInfo {
        val activityManager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager

        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)

        val powerManager = context.getSystemService(
            Context.POWER_SERVICE
        ) as PowerManager

        val thermal = if (Build.VERSION.SDK_INT >= 29) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "Unknown"
            }
        } else {
            "Unsupported"
        }

        return PerformanceInfo(
            totalRam = memory.totalMem,
            availableRam = memory.availMem,
            lowMemory = memory.lowMemory,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            thermalStatus = thermal
        )
    }
}
