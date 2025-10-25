package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.curzel.tama.api.ApiManager
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton
import it.curzel.tama.utils.PrivacyPolicyManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
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
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MyNavigationBar(
            title = ""
        )

        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
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