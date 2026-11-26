package com.anb.devicemanager.monitoring

import android.os.Environment
import android.os.StatFs

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long
) {
    val usedPercent: Int
        get() = if (totalBytes > 0) {
            (usedBytes * 100 / totalBytes).toInt()
        } else 0
}

class StorageMonitor {

    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(
            Environment.getDataDirectory().path
        )

        val total = stat.totalBytes
        val free = stat.availableBytes

        return StorageInfo(
            totalBytes = total,
            freeBytes = free,
            usedBytes = (total - free).coerceAtLeast(0)
        )
    }
}
