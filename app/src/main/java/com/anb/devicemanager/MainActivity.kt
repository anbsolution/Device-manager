package com.anb.devicemanager

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.monitoring.*
import com.anb.devicemanager.ui.SystemMetricsPanel
import com.anb.devicemanager.ui.BatteryIntelligencePanel
import com.anb.devicemanager.ui.AppManagerPanel
import com.anb.devicemanager.ui.StorageIntelligencePanel
import com.anb.devicemanager.ui.PermissionIntelligencePanel

import com.anb.devicemanager.ui.DeviceDiagnosticsPanel
import com.anb.devicemanager.ui.DeviceHealthPanel
import com.anb.devicemanager.ui.BackgroundMonitoringPanel
import com.anb.devicemanager.ui.HealthTrendsPanel
import com.anb.devicemanager.ui.DeviceAlertPanel
import com.anb.devicemanager.ui.ReportExportPanel
import com.anb.devicemanager.ui.LocalBackupPanel
import com.anb.devicemanager.ui.UsageIntelligencePanel
import com.anb.devicemanager.ui.MaintenanceRecommendationsPanel
import com.anb.devicemanager.ui.DeviceSecurityInsightsPanel
import com.anb.devicemanager.ui.DeviceCapabilityPanel
import com.anb.devicemanager.ui.DeviceActivityTimelinePanel
import com.anb.devicemanager.ui.DeviceBenchmarkPanel
import com.anb.devicemanager.ui.BatteryAnalyticsPanel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Dashboard(
                    context = this,
                    openUsageSettings = {
                        startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(
    context: Context,
    openUsageSettings: () -> Unit
) {
    var apps by remember {
        mutableStateOf(emptyList<InstalledApp>())
    }

    var usage by remember {
        mutableStateOf(emptyList<AppUsage>())
    }

    var battery by remember {
        mutableStateOf<BatteryInfo?>(null)
    }

    var usageAccess by remember {
        mutableStateOf(false)
    }

    fun refresh() {
        apps = AppInventoryMonitor(context).getInstalledApps()
        battery = BatteryMonitor(context).getBatteryInfo()

        val appOps = context.getSystemService(
            Context.APP_OPS_SERVICE
        ) as AppOpsManager

        usageAccess = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED

        usage = if (usageAccess) {
            UsageMonitor(context).getDailyUsage()
        } else {
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ANB Device Manager") },
                actions = {
                    TextButton(onClick = { refresh() }) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Device Overview",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                SystemMetricsPanel(context)
            }

            item {
                BatteryIntelligencePanel(context)
            }

            item {
                StorageIntelligencePanel(context)
            }

            item {
                PermissionIntelligencePanel(context)
            }

            item {
                DeviceDiagnosticsPanel(context)
            }

            item {
                DeviceHealthPanel(context)
            }

            item {
                BackgroundMonitoringPanel(context)
            }

            item {
                HealthTrendsPanel(context)
            }

            item {
                DeviceAlertPanel(context)
            }

            item {
                MaintenanceRecommendationsPanel(context)
            }

            item {
                DeviceSecurityInsightsPanel(context)
            }

            item {
                DeviceCapabilityPanel(context)
            }

            item {
                DeviceActivityTimelinePanel(context)
            }

            item {
                DeviceBenchmarkPanel(context)
            }

            item {
                BatteryAnalyticsPanel(context)
            }

            item {
                ReportExportPanel(
                    context = context,
                    usageAccess = usageAccess
                )
            }

            item {
                LocalBackupPanel(context)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Installed Applications")
                        Text(
                            "${apps.size} Apps",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("App Usage Monitoring")
                        Text(
                            if (usageAccess) "Usage Access Enabled"
                            else "Usage Access Required"
                        )
                        if (!usageAccess) {
                            Button(onClick = openUsageSettings) {
                                Text("Grant Usage Access")
                            }
                        }
                    }
                }
            }

            item {
                AppManagerPanel(
                    context = context,
                    apps = apps
                )
            }

            item {
                UsageIntelligencePanel(
                    context = context,
                    usageAccess = usageAccess,
                    appNames = apps.associate {
                        it.packageName to it.name
                    }
                )
            }

            item {
                Text(
                    "Top App Usage (Last 24 Hours)",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(usage.take(10)) { item ->
                val appName = apps.find {
                    it.packageName == item.packageName
                }?.name ?: item.packageName

                ListItem(
                    headlineContent = { Text(appName) },
                    supportingContent = {
                        Text(
                            "${item.usageTime / 60000} minutes"
                        )
                    }
                )
            }
        }
    }
}
