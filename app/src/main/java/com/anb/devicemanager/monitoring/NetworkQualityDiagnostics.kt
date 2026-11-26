package com.anb.devicemanager.monitoring

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class NetworkDiagnosis(
    val time: Long,
    val score: Int,
    val connection: String,
    val signalLevel: Int,
    val validated: Boolean,
    val disconnects: Int,
    val issues: List<String>,
    val recommendations: List<String>
)

object NetworkQualityDiagnostics {

    fun analyze(context: Context): NetworkDiagnosis {
        val signal = readNetworkSignal(context)

        val events = runCatching {
            JSONArray(
                context.getSharedPreferences(
                    "anb_network_stability",
                    Context.MODE_PRIVATE
                ).getString("events", "[]") ?: "[]"
            )
        }.getOrDefault(JSONArray())

        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L

        val recentDisconnects = (0 until events.length()).count { index ->
            val event = events.optJSONObject(index) ?: return@count false
            event.optString("event") == "Disconnected" &&
                event.optLong("time") >= cutoff
        }

        val level = signal.signalLevel ?: -1
        var score = 0
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (signal.connectionType == "Offline") {
            issues += "No active network connection"
            recommendations += "Check Wi-Fi or mobile data settings."
        } else {
            score += 25
        }

        if (signal.validated) {
            score += 35
        } else {
            issues += "Internet connection is not validated"
            recommendations += "Check captive portal, data balance, router or ISP connectivity."
        }

        if (level >= 0) {
            score += when (level) {
                0 -> 0
                1 -> 5
                2 -> 12
                3 -> 18
                else -> 20
            }

            if (level <= 1) {
                issues += "Weak network signal"
                recommendations += "Move closer to the Wi-Fi router or try a location with better mobile coverage."
            }
        } else if (signal.connectionType != "Offline") {
            issues += "Signal strength is unavailable"
            recommendations += "Check device signal permissions and hardware support."
        }

        score += when {
            recentDisconnects == 0 -> 20
            recentDisconnects <= 2 -> 12
            recentDisconnects <= 5 -> 5
            else -> 0
        }

        if (recentDisconnects > 0) {
            issues += "$recentDisconnects disconnections recorded in the last 24 hours"
            recommendations += "Check router stability, mobile coverage and power-saving restrictions."
        }

        if (signal.metered) {
            issues += "Connection is marked as metered"
            recommendations += "Review background data usage if your data allowance is limited."
        }

        if (signal.vpnActive) {
            issues += "VPN connection is active"
            recommendations += "If connectivity is slow, compare results with your VPN temporarily disconnected."
        }

        if (issues.isEmpty()) {
            recommendations += "No issue detected by the available local network indicators."
        }

        return NetworkDiagnosis(
            time = System.currentTimeMillis(),
            score = score.coerceIn(0, 100),
            connection = signal.connectionType,
            signalLevel = level,
            validated = signal.validated,
            disconnects = recentDisconnects,
            issues = issues,
            recommendations = recommendations
        )
    }

    fun save(context: Context, diagnosis: NetworkDiagnosis) {
        val prefs = context.getSharedPreferences(
            "anb_network_diagnostics",
            Context.MODE_PRIVATE
        )

        val old = runCatching {
            JSONArray(prefs.getString("history", "[]") ?: "[]")
        }.getOrDefault(JSONArray())

        val updated = JSONArray()
        for (i in (old.length() - 49).coerceAtLeast(0) until old.length()) {
            updated.put(old.getJSONObject(i))
        }

        updated.put(JSONObject().apply {
            put("time", diagnosis.time)
            put("score", diagnosis.score)
            put("connection", diagnosis.connection)
            put("signalLevel", diagnosis.signalLevel)
            put("validated", diagnosis.validated)
            put("disconnects", diagnosis.disconnects)
            put("issues", JSONArray(diagnosis.issues))
            put("recommendations", JSONArray(diagnosis.recommendations))
        })

        prefs.edit().putString("history", updated.toString()).apply()
    }

    fun history(context: Context): List<Pair<Long, Int>> {
        val array = runCatching {
            JSONArray(
                context.getSharedPreferences(
                    "anb_network_diagnostics",
                    Context.MODE_PRIVATE
                ).getString("history", "[]") ?: "[]"
            )
        }.getOrDefault(JSONArray())

        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            item.optLong("time") to item.optInt("score")
        }.reversed()
    }
}
