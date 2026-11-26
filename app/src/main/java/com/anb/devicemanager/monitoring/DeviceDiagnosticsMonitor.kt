package com.anb.devicemanager.monitoring

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock

data class SensorDetails(
    val name: String,
    val vendor: String,
    val type: String,
    val wakeUp: Boolean
)

data class DeviceDiagnosticsInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val securityPatch: String,
    val kernelVersion: String,
    val processorCores: Int,
    val uptimeHours: Long,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val densityDpi: Int,
    val sensors: List<SensorDetails>
)

class DeviceDiagnosticsMonitor(private val context: Context) {

    fun getDiagnostics(): DeviceDiagnosticsInfo {
        val metrics = context.resources.displayMetrics
        val sensorManager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            .map { sensor ->
                SensorDetails(
                    name = sensor.name,
                    vendor = sensor.vendor,
                    type = sensorTypeName(sensor.type),
                    wakeUp = sensor.isWakeUpSensor
                )
            }
            .sortedBy { it.name.lowercase() }

        return DeviceDiagnosticsInfo(
            manufacturer = Build.MANUFACTURER.orUnknown(),
            model = Build.MODEL.orUnknown(),
            androidVersion = Build.VERSION.RELEASE.orUnknown(),
            sdkVersion = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orUnknown(),
            kernelVersion = System.getProperty("os.version").orUnknown(),
            processorCores = Runtime.getRuntime().availableProcessors(),
            uptimeHours = SystemClock.elapsedRealtime() / 3_600_000L,
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            sensors = sensors
        )
    }

    private fun sensorTypeName(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_PRESSURE -> "Pressure"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion"
            else -> "Sensor Type $type"
        }
    }

    private fun String?.orUnknown(): String =
        this?.takeIf { it.isNotBlank() } ?: "Unknown"
}
