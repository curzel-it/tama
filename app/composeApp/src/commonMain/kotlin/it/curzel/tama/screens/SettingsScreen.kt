package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.curzel.tama.model.TamaConfig
import it.curzel.tama.model.ThemePreference
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.ThemeManager
import it.curzel.tama.theme.TamaButton
import it.curzel.tama.utils.PrivacyPolicyManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    var serverUrl by remember { mutableStateOf("") }
    var serversText by remember { mutableStateOf("") }
    var serverOverride by remember { mutableStateOf(false) }
    var themePreference by remember { mutableStateOf(ThemePreference.SYSTEM) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var showPrivacyWebView by remember { mutableStateOf(false) }
    var showTermsWebView by remember { mutableStateOf(false) }
    var channelName by remember { mutableStateOf<String?>(null) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountLoading by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val config = ConfigStorage.loadConfig()
        if (config != null) {
            serverUrl = config.server_url
            serversText = config.servers.joinToString("\n")
            serverOverride = config.server_override
            themePreference = config.getThemePreference()
        }

        val channel = ConfigStorage.loadChannelInfo()
        val token = ConfigStorage.loadToken()
        isLoggedIn = channel != null && token != null
        channelName = channel?.name
    }

    if (showPrivacyWebView) {
        WebViewScreen(
            url = PrivacyPolicyManager.PRIVACY_POLICY_URL,
            title = "Privacy Policy",
            onBack = { showPrivacyWebView = false }
        )
        return
    }

    if (showTermsWebView) {
        WebViewScreen(
            url = PrivacyPolicyManager.TERMS_AND_CONDITIONS_URL,
            title = "Terms and Conditions",
            onBack = { showTermsWebView = false }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MyNavigationBar(
            title = "Settings"
        )

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://example.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use custom list of servers")
                Switch(
                    checked = serverOverride,
                    onCheckedChange = { serverOverride = it }
                )
            }

            OutlinedTextField(
                value = serversText,
                onValueChange = { serversText = it },
                label = { Text("Servers (one per line)") },
                placeholder = { Text("https://server1.com\nhttps://server2.com", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.extraSmall,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (isLoggedIn && channelName != null) {
                Text(
                    text = "Logged in as: $channelName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TamaButton(
                    onClick = {
                        PrivacyPolicyManager.opener.openPrivacyPolicy(
                            onShowWebView = { showPrivacyWebView = true }
                        )
                    }
                ) {
                    Text("Privacy Policy")
                }

                TamaButton(
                    onClick = {
                        PrivacyPolicyManager.opener.openTermsAndConditions(
                            onShowWebView = { showTermsWebView = true }
                        )
                    }
                ) {
                    Text("Terms and Conditions")
                }

                if (isLoggedIn) {
                    TamaButton(
                        onClick = {
                            scope.launch {
                                ConfigStorage.clearToken()
                                ConfigStorage.clearChannelInfo()
                                isLoggedIn = false
                                channelName = null
                                saveMessage = "Logged out successfully"
                            }
                        }
                    ) {
                        Text("Logout")
                    }

                    TamaButton(
                        onClick = { showDeleteAccountDialog = true }
                    ) {
                        Text("Delete Account")
                    }
                }
            }
        }

        if (showDeleteAccountDialog) {
            it.curzel.tama.settings.DeleteAccountDialog(
                onDismiss = {
                    showDeleteAccountDialog = false
                    deleteAccountError = null
                },
                onConfirm = {
                    scope.launch {
                        deleteAccountLoading = true
                        deleteAccountError = null

                        try {
                            val config = ConfigStorage.loadConfig()
                            if (config == null) {
                                deleteAccountError = "Configuration not found"
                                deleteAccountLoading = false
                                return@launch
                            }

                            val client = it.curzel.tama.api.ApiManager.getClient(config.server_url)
                            val result = client.deleteAccount()

                            if (result.isSuccess) {
                                ConfigStorage.clearToken()
                                ConfigStorage.clearChannelInfo()

                                isLoggedIn = false
                                channelName = null
                                showDeleteAccountDialog = false
                                saveMessage = "Account deleted successfully"
                            } else {
                                deleteAccountError = result.exceptionOrNull()?.message ?: "Unknown error"
                            }
                        } catch (e: Exception) {
                            deleteAccountError = "Unexpected error: ${e.message}"
                        } finally {
                            deleteAccountLoading = false
                        }
                    }
                },
                isLoading = deleteAccountLoading,
                errorMessage = deleteAccountError
            )
        }
    }
}
