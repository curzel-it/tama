package it.curzel.tama

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.curzel.tama.api.ApiManager
import it.curzel.tama.midi.MidiComposerScreen
import it.curzel.tama.screens.WebViewScreen
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton
import it.curzel.tama.utils.PrivacyPolicyManager
import kotlinx.coroutines.launch

enum class EditorScreen {
    Main,
    MidiComposer,
    PixelEditor
}

@Composable
fun ContentEditorScreen() {
    var currentScreen by remember { mutableStateOf(EditorScreen.Main) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var checkLoginTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(checkLoginTrigger) {
        val channel = ConfigStorage.loadChannelInfo()
        val token = ConfigStorage.loadToken()
        isLoggedIn = channel != null && token != null
    }

    if (!isLoggedIn) {
        LoginPromptScreen(onLoginSuccess = {
            checkLoginTrigger++
        })
        return
    }

    when (currentScreen) {
        EditorScreen.Main -> MainEditorScreen(
            onNavigateToMidi = { currentScreen = EditorScreen.MidiComposer },
            onNavigateToPixel = { currentScreen = EditorScreen.PixelEditor }
        )
        EditorScreen.MidiComposer -> MidiComposerScreen(
            onBack = { currentScreen = EditorScreen.Main }
        )
        EditorScreen.PixelEditor -> {
            // TODO: Implement Pixel Editor Screen
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MyNavigationBar(
                    title = "Pixel Editor",
                    onBackClick = { currentScreen = EditorScreen.Main }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text("Pixel Editor - Coming Soon")
                }
            }
        }
    }
}

@Composable
fun LoginPromptScreen(onLoginSuccess: () -> Unit) {
    var channelName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showWebView by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (showWebView) {
        WebViewScreen(
            url = PrivacyPolicyManager.PRIVACY_POLICY_URL,
            title = "Privacy Policy",
            onBack = { showWebView = false }
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = "Login / Sign Up"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Login or Create Account",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Access the content editor",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = channelName,
                onValueChange = { channelName = it },
                label = { Text("Channel Name") },
                placeholder = { Text("Enter your channel name") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("Enter your password") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                isError = errorMessage != null
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TamaButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        val config = ConfigStorage.loadConfig()
                        if (config == null) {
                            errorMessage = "Please configure server URL in Settings"
                            isLoading = false
                            return@launch
                        }

                        val client = ApiManager.getClient(config.server_url)
                        val result = client.loginOrSignup(channelName, password)

                        if (result.isSuccess) {
                            val authResponse = result.getOrNull()!!
                            ConfigStorage.saveToken(authResponse.token)
                            ConfigStorage.saveChannelInfo(authResponse.channel)
                            onLoginSuccess()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                        }

                        isLoading = false
                    }
                },
                enabled = !isLoading && channelName.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    PrivacyPolicyManager.opener.openPrivacyPolicy(
                        onShowWebView = { showWebView = true }
                    )
                }
            ) {
                Text(
                    text = "Privacy Policy",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MainEditorScreen(
    onNavigateToMidi: () -> Unit,
    onNavigateToPixel: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = "Content Editor"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FileSection()

            EditorToolsSection(
                onNavigateToMidi = onNavigateToMidi,
                onNavigateToPixel = onNavigateToPixel
            )
        }
    }
}

@Composable
fun FileSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "File",
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TamaButton(
                onClick = { /* TODO: Handle upload */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Upload")
            }

            TamaButton(
                onClick = { /* TODO: Handle download */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Download")
            }
        }

        TamaButton(
            onClick = { /* TODO: Handle publish */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publish")
        }
    }
}

@Composable
fun EditorToolsSection(
    onNavigateToMidi: () -> Unit,
    onNavigateToPixel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tools",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedCard(
            onClick = onNavigateToMidi,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "MIDI Composer",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Create music for your content",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedCard(
            onClick = onNavigateToPixel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Pixel Art Editor",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Draw and animate pixel art",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
