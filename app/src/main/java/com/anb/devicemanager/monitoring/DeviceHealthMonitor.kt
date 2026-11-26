package com.anb.devicemanager.monitoring

data class HealthCheck(
    val title: String,
    val status: String,
    val detail: String
)

data class DeviceHealthReport(
    val overallStatus: String,
    val checks: List<HealthCheck>
)

class DeviceHealthMonitor {

    fun evaluate(
        performance: PerformanceInfo,
        storage: StorageInfo,
        battery: BatteryInfo?,
        network: NetworkInfo
    ): DeviceHealthReport {

        val checks = mutableListOf<HealthCheck>()

        checks += when {
            performance.lowMemory ->
                HealthCheck("RAM", "Critical", "Android reports low memory")
            performance.totalRam > 0 &&
                performance.availableRam * 100 / performance.totalRam < 10 ->
                HealthCheck("RAM", "Attention", "Available RAM is below 10%")
            else ->
                HealthCheck("RAM", "Normal", "Memory status is normal")
        }

        checks += when {
            storage.usedPercent >= 95 ->
                HealthCheck("Storage", "Critical", "${storage.usedPercent}% used")
            storage.usedPercent >= 85 ->
                HealthCheck("Storage", "Attention", "${storage.usedPercent}% used")
            else ->
                HealthCheck("Storage", "Normal", "${storage.usedPercent}% used")
        }

        checks += when {
            battery == null ->
                HealthCheck("Battery", "Unknown", "Battery data unavailable")
            battery.health == "Overheat" ->
                HealthCheck("Battery", "Critical", "Android reports battery overheat")
            battery.health != "Good" && battery.health != "Unknown" ->
                HealthCheck("Battery", "Attention", battery.health)
            battery.temperature >= 45f ->
                HealthCheck("Battery", "Attention", "Temperature: ${battery.temperature} °C")
            else ->
                HealthCheck("Battery", "Normal", "Battery status: ${battery.health}")
        }

        checks += when (performance.thermalStatus) {
            "Critical", "Emergency", "Shutdown" ->
                HealthCheck("Thermal", "Critical", performance.thermalStatus)
            "Severe", "Moderate" ->
                HealthCheck("Thermal", "Attention", performance.thermalStatus)
            "Normal", "Light" ->
                HealthCheck("Thermal", "Normal", performance.thermalStatus)
            else ->
                HealthCheck("Thermal", "Unknown", performance.thermalStatus)
        }

        checks += when {
            network.internetValidated ->
                HealthCheck("Network", "Normal", "Internet connection validated")
            network.connected ->
                HealthCheck("Network", "Attention", "Connected, but internet is not validated")
            else ->
                HealthCheck("Network", "Attention", "No active network")
        }

        val overall = when {
            checks.any { it.status == "Critical" } -> "Critical"
            checks.any { it.status == "Attention" } -> "Attention"
            checks.all { it.status == "Normal" } -> "Normal"
            else -> "Unknown"
        }

        return DeviceHealthReport(overall, checks)
    }
}
