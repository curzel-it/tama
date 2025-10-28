package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.curzel.tama.Constants
import it.curzel.tama.theme.TamaButton
import it.curzel.tama.version.Platform
import it.curzel.tama.version.VersionCheckResult
import it.curzel.tama.version.VersionUseCase

interface UrlOpener {
    fun openUrl(url: String)
}

object UrlOpenerHolder {
    lateinit var urlOpener: UrlOpener
}

@Composable
fun UpdateRequiredScreen(versionCheckResult: VersionCheckResult) {
    val platform = VersionUseCase.currentPlatform
    val storeUrl = when (platform) {
        Platform.IOS -> Constants.IOS_APP_STORE_URL
        Platform.ANDROID -> Constants.ANDROID_APP_STORE_URL
        Platform.JVM -> Constants.ANDROID_APP_STORE_URL // JVM fallback
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Update Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Your current version (${versionCheckResult.currentVersion}) is outdated.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Minimum required version: ${versionCheckResult.minimumVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            TamaButton(
                onClick = { UrlOpenerHolder.urlOpener.openUrl(storeUrl) }
            ) {
                Text("Update Now")
            }
        }
    }
}
