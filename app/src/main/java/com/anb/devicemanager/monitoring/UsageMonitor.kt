package com.anb.devicemanager.monitoring

import android.app.usage.UsageStatsManager
import android.content.Context

data class AppUsage(
    val packageName: String,
    val usageTime: Long,
    val lastUsed: Long
)

class UsageMonitor(private val context: Context) {

    fun getDailyUsage(): List<AppUsage> {
        val manager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - 24L * 60 * 60 * 1000

        return manager.queryAndAggregateUsageStats(
            start, end
        ).map { (packageName, stats) ->
            AppUsage(
                packageName = packageName,
                usageTime = stats.totalTimeInForeground,
                lastUsed = stats.lastTimeUsed
            )
        }.sortedByDescending { it.usageTime }
    }
}
