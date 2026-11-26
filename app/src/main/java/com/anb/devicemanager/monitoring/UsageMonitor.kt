package com.anb.devicemanager.monitoring

import android.app.usage.UsageStatsManager
import android.content.Context

data class AppUsage(
    val packageName: String,
    val usageTime: Long,
    val lastUsed: Long
)

data class UsageSummary(
    val periodDays: Int,
    val totalUsageMs: Long,
    val appCount: Int,
    val apps: List<AppUsage>
)

class UsageMonitor(private val context: Context) {

    fun getDailyUsage(): List<AppUsage> {
        return getUsageForDays(1)
    }

    fun getUsageForDays(days: Int): List<AppUsage> {
        val manager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - days.coerceIn(1, 30) * 24L * 60 * 60 * 1000

        return manager.queryAndAggregateUsageStats(
            start, end
        ).map { (packageName, stats) ->
            AppUsage(
                packageName = packageName,
                usageTime = stats.totalTimeInForeground,
                lastUsed = stats.lastTimeUsed
            )
        }.filter { it.usageTime > 0 }
            .sortedByDescending { it.usageTime }
    }

    fun getSummary(days: Int): UsageSummary {
        val apps = getUsageForDays(days)

        return UsageSummary(
            periodDays = days,
            totalUsageMs = apps.sumOf { it.usageTime },
            appCount = apps.size,
            apps = apps
        )
    }
}
