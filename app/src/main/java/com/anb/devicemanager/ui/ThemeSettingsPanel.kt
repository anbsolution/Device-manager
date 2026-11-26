package com.anb.devicemanager.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.ui.theme.AnbThemeMode

@Composable
fun ThemeSettingsPanel(
    context: Context,
    selectedMode: AnbThemeMode,
    onModeChange: (AnbThemeMode) -> Unit
) {
    val options = listOf(
        AnbThemeMode.LIGHT to "Premium Light",
        AnbThemeMode.DARK to "Premium Dark",
        AnbThemeMode.ADAPTIVE to "Adaptive"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Appearance & Theme", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose your preferred app appearance.",
                style = MaterialTheme.typography.bodySmall
            )

            options.forEach { (mode, title) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title)
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = { onModeChange(mode) }
                    )
                }
            }

            Text(
                "Your selection is saved locally on this device.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
