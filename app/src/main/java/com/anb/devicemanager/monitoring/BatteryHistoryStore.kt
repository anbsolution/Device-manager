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

        writeHistory(history.takeLast(1000))
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

    fun exportBackup(): String {
        val snapshots = JSONArray()

        getHistory().forEach {
            snapshots.put(
                JSONObject().apply {
                    put("time", it.timestamp)
                    put("level", it.percentage)
                    put("temperature", it.temperature.toDouble())
                    put("charging", it.charging)
                }
            )
        }

        return JSONObject().apply {
            put("format", "ANB_BATTERY_BACKUP")
            put("version", 1)
            put("snapshots", snapshots)
        }.toString(2)
    }

    fun restoreBackup(raw: String): Int {
        val root = JSONObject(raw)

        require(root.optString("format") == "ANB_BATTERY_BACKUP") {
            "Invalid backup format"
        }

        require(root.optInt("version", -1) == 1) {
            "Unsupported backup version"
        }

        val array = root.getJSONArray("snapshots")
        require(array.length() <= 1000) {
            "Backup contains too many records"
        }

        val restored = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)

            val timestamp = item.getLong("time")
            val level = item.getInt("level")
            val temperature = item.getDouble("temperature").toFloat()
            val charging = item.getBoolean("charging")

            require(timestamp > 0)
            require(level in 0..100)
            require(temperature.isFinite() && temperature >= 0f)

            BatterySnapshot(
                timestamp = timestamp,
                percentage = level,
                temperature = temperature,
                charging = charging
            )
        }.sortedBy { it.timestamp }

        writeHistory(restored)
        return restored.size
    }

    private fun writeHistory(history: List<BatterySnapshot>) {
        val json = JSONArray()

        history.forEach {
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
}
