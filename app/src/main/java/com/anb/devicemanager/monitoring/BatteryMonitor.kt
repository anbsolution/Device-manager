package com.anb.devicemanager.monitoring

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryInfo(
    val percentage: Int,
    val temperature: Float,
    val charging: Boolean
)

class BatteryMonitor(private val context: Context) {

    fun getBatteryInfo(): BatteryInfo? {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

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

        return BatteryInfo(
            percentage = if (level >= 0 && scale > 0) {
                level * 100 / scale
            } else -1,
            temperature = if (temp >= 0) temp / 10f else -1f,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        )
    }
}
