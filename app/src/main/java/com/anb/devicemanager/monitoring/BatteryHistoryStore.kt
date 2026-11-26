package com.anb.devicemanager.monitoring

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BatterySnapshot(
    val timestamp: Long,
    val percentage: Int,
    val temperature: Float,
    val charging: Boolean
)

class BatteryHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences(
        "anb_battery_history", Context.MODE_PRIVATE
    )

    fun save(info: BatteryInfo) {
        if (info.percentage !in 0..100) return

        val history = getHistory().toMutableList()
        val now = System.currentTimeMillis()

        if (history.lastOrNull()?.let {
                now - it.timestamp < 60_000
            } == true) return

        history.add(
            BatterySnapshot(
                timestamp = now,
                percentage = info.percentage,
                temperature = info.temperature,
                charging = info.charging
            )
        )

        val json = JSONArray()
        history.takeLast(1000).forEach {
            json.put(
                JSONObject().apply {
                    put("time", it.timestamp)
                    put("level", it.percentage)
                    put("temperature", it.temperature.toDouble())
                    put("charging", it.charging)
                }
            )
        }

        prefs.edit().putString("history", json.toString()).apply()
    }

    fun getHistory(): List<BatterySnapshot> {
        val raw = prefs.getString("history", null) ?: return emptyList()

        return try {
            val json = JSONArray(raw)
            (0 until json.length()).map { index ->
                val item = json.getJSONObject(index)
                BatterySnapshot(
                    timestamp = item.getLong("time"),
                    percentage = item.getInt("level"),
                    temperature = item.getDouble("temperature").toFloat(),
                    charging = item.getBoolean("charging")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
