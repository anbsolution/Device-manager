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
import com.anb.devicemanager.ui.DashboardQuickStatusPanel
import com.anb.devicemanager.ui.DeviceIdentityPanel
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
import com.anb.devicemanager.ui.NetworkHistoryPanel
import com.anb.devicemanager.ui.ConnectivityDiagnosticsPanel
import com.anb.devicemanager.ui.NetworkStabilityPanel
import com.anb.devicemanager.ui.NetworkTrafficAnalyticsPanel
import com.anb.devicemanager.ui.NetworkExportPanel
import com.anb.devicemanager.ui.NetworkBackupPanel
import com.anb.devicemanager.ui.NetworkInsightsPanel
import com.anb.devicemanager.ui.LiveNetworkSpeedPanel
import com.anb.devicemanager.ui.LiveNetworkSpeedIndicator
import com.anb.devicemanager.ui.NetworkSignalHistoryPanel
import com.anb.devicemanager.ui.NetworkSignalAlertPanel
import com.anb.devicemanager.ui.NetworkQualityDiagnosticsPanel
import com.anb.devicemanager.ui.ThemeSettingsPanel
import com.anb.devicemanager.ui.theme.AnbThemeMode
import com.anb.devicemanager.ui.theme.AnbDeviceManagerTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themePrefs = remember {
                getSharedPreferences("anb_ui_preferences", Context.MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    runCatching {
                        AnbThemeMode.valueOf(
                            themePrefs.getString("theme_mode", "ADAPTIVE") ?: "ADAPTIVE"
                        )
                    }.getOrDefault(AnbThemeMode.ADAPTIVE)
                )
            }

            AnbDeviceManagerTheme(mode = themeMode) {
                Dashboard(
                    context = this,
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        themeMode = mode
                        themePrefs.edit().putString("theme_mode", mode.name).apply()
                    },
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
    themeMode: AnbThemeMode,
    onThemeChange: (AnbThemeMode) -> Unit,
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

    var selectedTab by remember {
        mutableStateOf(0)
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
            Column {
                TopAppBar(
                    title = { Text("ANB Device Manager") },
                    actions = {
                        TextButton(onClick = { refresh() }) {
                            Text("Refresh")
                        }
                    }
                )
                LiveNetworkSpeedIndicator(context)
            }
        },
        bottomBar = {
            NavigationBar {
                val labels = listOf("Home", "Network", "Battery", "Apps", "Tools")
                val symbols = listOf("⌂", "◉", "▰", "▦", "⚙")

                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(symbols[index]) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        Text("Device Overview", style = MaterialTheme.typography.headlineMedium)
                    }
                    item { DeviceIdentityPanel() }
                    item {
                        DashboardQuickStatusPanel(
                            appCount = apps.size,
                            battery = battery
                        )
                    }
                    item { SystemMetricsPanel(context) }
                    item { DeviceHealthPanel(context) }
                    item { StorageIntelligencePanel(context) }
                    item { HealthTrendsPanel(context) }
                    item { MaintenanceRecommendationsPanel(context) }
                    item { DeviceBenchmarkPanel(context) }
                }

                1 -> {
                    item {
                        Text("Network Intelligence", style = MaterialTheme.typography.headlineMedium)
                    }
                    item { NetworkHistoryPanel(context) }
                    item { ConnectivityDiagnosticsPanel(context) }
                    item { NetworkStabilityPanel(context) }
                    item { NetworkTrafficAnalyticsPanel(context) }
                    item { NetworkExportPanel(context) }
                    item { NetworkBackupPanel(context) }
                    item { NetworkInsightsPanel(context) }
                    item { LiveNetworkSpeedPanel(context) }
                    item { NetworkSignalHistoryPanel(context) }
                    item { NetworkSignalAlertPanel(context) }
                    item { NetworkQualityDiagnosticsPanel(context) }
                }

                2 -> {
                    item {
                        Text("Battery & Power", style = MaterialTheme.typography.headlineMedium)
                    }
                    item { BatteryIntelligencePanel(context) }
                    item { BatteryAnalyticsPanel(context) }
                }

                3 -> {
                    item {
                        Text("Applications", style = MaterialTheme.typography.headlineMedium)
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
                        AppManagerPanel(context = context, apps = apps)
                    }
                    item {
                        UsageIntelligencePanel(
                            context = context,
                            usageAccess = usageAccess,
                            appNames = apps.associate { it.packageName to it.name }
                        )
                    }
                    item {
                        Text("Top App Usage (Last 24 Hours)", style = MaterialTheme.typography.titleLarge)
                    }
                    items(usage.take(10)) { appUsage ->
                        val appName = apps.find {
                            it.packageName == appUsage.packageName
                        }?.name ?: appUsage.packageName

                        ListItem(
                            headlineContent = { Text(appName) },
                            supportingContent = {
                                Text("${appUsage.usageTime / 60000} minutes")
                            }
                        )
                    }
                }

                4 -> {
                    item {
                        Text("Device Tools & Settings", style = MaterialTheme.typography.headlineMedium)
                    }
                    item {
                        ThemeSettingsPanel(
                            context = context,
                            selectedMode = themeMode,
                            onModeChange = onThemeChange
                        )
                    }
                    item { PermissionIntelligencePanel(context) }
                    item { DeviceDiagnosticsPanel(context) }
                    item { BackgroundMonitoringPanel(context) }
                    item { DeviceAlertPanel(context) }
                    item { DeviceSecurityInsightsPanel(context) }
                    item { DeviceCapabilityPanel(context) }
                    item { DeviceActivityTimelinePanel(context) }
                    item {
                        ReportExportPanel(
                            context = context,
                            usageAccess = usageAccess
                        )
                    }
                    item { LocalBackupPanel(context) }
                }
            }
        }
    }
}
