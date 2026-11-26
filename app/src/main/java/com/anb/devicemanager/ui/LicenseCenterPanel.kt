package com.anb.devicemanager.ui

import android.content.Context
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anb.devicemanager.license.LicenseManager

@Composable
fun LicenseCenterPanel(context: Context) {
    val prefs = remember {
        context.getSharedPreferences("anb_license_store", Context.MODE_PRIVATE)
    }

    val deviceId = remember {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "Unavailable"
    }

    var ownershipText by remember {
        mutableStateOf(prefs.getString("ownership_certificate", null))
    }
    var customerText by remember {
        mutableStateOf(prefs.getString("customer_license", null))
    }
    var ownershipStatus by remember { mutableStateOf("Not installed") }
    var customerStatus by remember { mutableStateOf("Not installed") }

    fun validateSaved() {
        ownershipStatus = ownershipText?.let {
            val result = LicenseManager.verify(context, it, "ownership")
            if (result.valid) "Verified • ${result.message}" else result.message
        } ?: "Not installed"

        customerStatus = customerText?.let {
            val result = LicenseManager.verify(context, it, "customer")
            if (result.valid) "Verified • ${result.message}" else result.message
        } ?: "Not installed"
    }

    LaunchedEffect(ownershipText, customerText) {
        validateSaved()
    }

    val ownershipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read selected file.")
            }.onSuccess { text ->
                val result = LicenseManager.verify(context, text, "ownership")
                ownershipStatus = if (result.valid) {
                    prefs.edit().putString("ownership_certificate", text).apply()
                    ownershipText = text
                    "Verified • ${result.message}"
                } else result.message
            }.onFailure {
                ownershipStatus = "Unable to read certificate."
            }
        }
    }

    val customerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to read selected file.")
            }.onSuccess { text ->
                val result = LicenseManager.verify(context, text, "customer")
                customerStatus = if (result.valid) {
                    prefs.edit().putString("customer_license", text).apply()
                    customerText = text
                    "Verified • ${result.message}"
                } else result.message
            }.onFailure {
                customerStatus = "Unable to read licence."
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ANB Solution Licence Center", style = MaterialTheme.typography.titleLarge)
            Text("Device ID", style = MaterialTheme.typography.titleMedium)
            SelectionContainer {
                Text(deviceId, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Use this ID when issuing a licence for this installation.",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Text("Ownership Certificate", style = MaterialTheme.typography.titleMedium)
            Text(ownershipStatus, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { ownershipPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Ownership Certificate")
            }

            HorizontalDivider()

            Text("Customer / Device Licence", style = MaterialTheme.typography.titleMedium)
            Text(customerStatus, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { customerPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import Customer Licence")
            }

            Text(
                "Licences are verified offline using the embedded ANB Solution public key. Invalid files are not saved.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
