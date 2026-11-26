package com.anb.devicemanager.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class BackgroundActivityMonitor(
    private val context: Context
) {
    fun getBackgroundTransitions(): Map<String, Int> {
        val manager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - 24L * 60 * 60 * 1000

        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val result = mutableMapOf<String, Int>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.eventType ==
                UsageEvents.Event.MOVE_TO_BACKGROUND
            ) {
                val pkg = event.packageName ?: continue
                result[pkg] = (result[pkg] ?: 0) + 1
            }
        }

        return result
    }
}
