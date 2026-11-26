package com.anb.devicemanager.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryInfo(
    val percentage: Int,
    val temperature: Float,
    val charging: Boolean,
    val health: String,
    val voltageMv: Int,
    val chargeCounterUah: Int?,
    val currentNowUa: Int?,
    val currentAverageUa: Int?
)

class BatteryMonitor(private val context: Context) {

    fun getBatteryInfo(): BatteryInfo? {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        val manager = context.getSystemService(
            Context.BATTERY_SERVICE
        ) as BatteryManager

        val level = intent.getIntExtra(
            BatteryManager.EXTRA_LEVEL, -1
        )
        val scale = intent.getIntExtra(
            BatteryManager.EXTRA_SCALE, -1
        )
        val temp = intent.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, -1
        )
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS, -1
        )
        val health = intent.getIntExtra(
            BatteryManager.EXTRA_HEALTH, -1
        )

        fun property(id: Int): Int? {
            val value = manager.getIntProperty(id)
            return value.takeIf { it != Int.MIN_VALUE && it >= 0 }
        }

        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> "Unknown"
        }

        return BatteryInfo(
            percentage = if (level >= 0 && scale > 0) {
                level * 100 / scale
            } else -1,
            temperature = if (temp >= 0) temp / 10f else -1f,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            health = healthText,
            voltageMv = intent.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE, -1
            ),
            chargeCounterUah = property(
                BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER
            ),
            currentNowUa = property(
                BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
            )?.let { kotlin.math.abs(it) },
            currentAverageUa = property(
                BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE
            )?.let { kotlin.math.abs(it) }
        )
    }
}
