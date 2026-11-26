package com.anb.devicemanager.monitoring

import android.content.Context
import android.os.Environment
import android.os.StatFs

data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val appCacheBytes: Long
) {
    val usedPercent: Int
        get() = if (totalBytes > 0) {
            (usedBytes * 100 / totalBytes).toInt()
        } else 0
}

class StorageMonitor(private val context: Context) {

    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes

        return StorageInfo(
            totalBytes = total,
            freeBytes = free,
            usedBytes = (total - free).coerceAtLeast(0),
            appCacheBytes = getCacheSize(context.cacheDir) +
                (context.externalCacheDir?.let { getCacheSize(it) } ?: 0L)
        )
    }

    fun clearAppCache(): Boolean {
        return try {
            context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            context.externalCacheDir?.listFiles()?.forEach {
                it.deleteRecursively()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getCacheSize(file: java.io.File): Long {
        if (!file.exists()) return 0L
        return if (file.isFile) file.length()
        else file.listFiles()?.sumOf { getCacheSize(it) } ?: 0L
    }
}
