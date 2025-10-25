package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.curzel.tama.model.TamaConfig
import it.curzel.tama.model.ThemePreference
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.theme.ThemeManager
import it.curzel.tama.theme.TamaButton
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    var serverUrl by remember { mutableStateOf("") }
    var serversText by remember { mutableStateOf("") }
    var serverOverride by remember { mutableStateOf(false) }
    var themePreference by remember { mutableStateOf(ThemePreference.SYSTEM) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val config = ConfigStorage.loadConfig()
        if (config != null) {
            serverUrl = config.server_url
            serversText = config.servers.joinToString("\n")
            serverOverride = config.server_override
            themePreference = config.getThemePreference()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            placeholder = { Text("https://example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = serversText,
            onValueChange = { serversText = it },
            label = { Text("Servers (one per line)") },
            placeholder = { Text("https://server1.com\nhttps://server2.com") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            minLines = 5,
            maxLines = 5
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreference.entries.forEach { preference ->
                    FilterChip(
                        selected = themePreference == preference,
                        onClick = { themePreference = preference },
                        label = { Text(preference.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Server Override")
            Switch(
                checked = serverOverride,
                onCheckedChange = { serverOverride = it }
            )
        }

        TamaButton(
            onClick = {
                scope.launch {
                    isSaving = true
                    saveMessage = null
                    try {
                        val servers = serversText
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        val config = TamaConfig(
                            server_url = serverUrl,
                            servers = servers,
                            server_override = serverOverride,
                            theme = themePreference.name
                        )

                        val validation = config.validate()
                        if (validation.isSuccess) {
                            ConfigStorage.saveConfig(config)
                            ThemeManager.setThemePreference(themePreference)
                            saveMessage = "Settings saved successfully"
                        } else {
                            saveMessage = "Error: ${validation.exceptionOrNull()?.message}"
                        }
                    } catch (e: Exception) {
                        saveMessage = "Error saving settings: ${e.message}"
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text("Save Settings")
            }
        }

        saveMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.startsWith("Error")) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
