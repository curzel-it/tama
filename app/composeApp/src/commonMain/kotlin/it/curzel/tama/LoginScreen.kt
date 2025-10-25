package it.curzel.tama

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.curzel.tama.screens.WebViewScreen
import it.curzel.tama.utils.PrivacyPolicyManager

@Composable
fun LoginScreen() {
    var showWebView by remember { mutableStateOf(false) }

    if (showWebView) {
        WebViewScreen(
            url = PrivacyPolicyManager.PRIVACY_POLICY_URL,
            title = "Privacy Policy",
            onBack = { showWebView = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Login or Signup", style = MaterialTheme.typography.headlineMedium)
        Text("Username field")
        Text("Password field")
        Text("Login button")
        Text("Signup button")

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = {
                PrivacyPolicyManager.opener.openPrivacyPolicy(
                    onShowWebView = { showWebView = true }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Privacy Policy",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
