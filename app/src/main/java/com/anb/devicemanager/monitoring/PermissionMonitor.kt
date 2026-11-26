package com.anb.devicemanager.monitoring

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo

data class AppPermission(
    val name: String,
    val granted: Boolean
)

data class AppPermissionInfo(
    val appName: String,
    val packageName: String,
    val permissions: List<AppPermission>
) {
    val grantedCount: Int
        get() = permissions.count { it.granted }

    val deniedCount: Int
        get() = permissions.count { !it.granted }
}

class PermissionMonitor(private val context: Context) {

    fun getPermissionReport(): List<AppPermissionInfo> {
        val pm = context.packageManager

        return try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                .mapNotNull { pkg ->
                    val names = pkg.requestedPermissions
                        ?: return@mapNotNull null
                    val flags = pkg.requestedPermissionsFlags

                    val dangerous = names.mapIndexedNotNull { index, name ->
                        val permissionInfo = try {
                            pm.getPermissionInfo(name, 0)
                        } catch (_: Exception) {
                            null
                        } ?: return@mapIndexedNotNull null

                        val isDangerous =
                            (permissionInfo.protectionLevel and
                                PermissionInfo.PROTECTION_MASK_BASE) ==
                                PermissionInfo.PROTECTION_DANGEROUS

                        if (!isDangerous) {
                            return@mapIndexedNotNull null
                        }

                        val permissionFlags = flags?.getOrNull(index) ?: 0

                        AppPermission(
                            name = name,
                            granted = (permissionFlags and
                                PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                        )
                    }

                    if (dangerous.isEmpty()) {
                        return@mapNotNull null
                    }

                    AppPermissionInfo(
                        appName = pkg.applicationInfo?.loadLabel(pm)?.toString()
                            ?: pkg.packageName,
                        packageName = pkg.packageName,
                        permissions = dangerous
                    )
                }
                .sortedBy { it.appName.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
