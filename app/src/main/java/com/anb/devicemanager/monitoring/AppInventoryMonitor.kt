package com.anb.devicemanager.monitoring

import android.content.Context
import android.content.pm.ApplicationInfo

data class InstalledApp(
    val name: String,
    val packageName: String,
    val systemApp: Boolean
)

class AppInventoryMonitor(private val context: Context) {

    fun getInstalledApps(): List<InstalledApp> {
        return context.packageManager
            .getInstalledApplications(0)
            .map {
                InstalledApp(
                    name = context.packageManager
                        .getApplicationLabel(it).toString(),
                    packageName = it.packageName,
                    systemApp = (it.flags and
                        ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
