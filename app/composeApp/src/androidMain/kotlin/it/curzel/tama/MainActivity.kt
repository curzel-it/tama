package it.curzel.tama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import it.curzel.tama.api.ApiManager
import it.curzel.tama.content.ContentWipStorageAndroid
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerAndroid
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerAndroid
import it.curzel.tama.screens.UrlOpenerAndroid
import it.curzel.tama.screens.UrlOpenerHolder
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageAndroid
import it.curzel.tama.storage.ReportedContentStorage
import it.curzel.tama.storage.ReportedContentStorageAndroid
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerAndroid
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.sharing.ContentSharerAndroid
import it.curzel.tama.version.Platform
import it.curzel.tama.version.VersionUseCase
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var deepLinkContentId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MidiPlayer.provider = MidiPlayerAndroid()
        MidiComposer.backend = MidiComposerAndroid()
        ConfigStorage.provider = ConfigStorageAndroid(applicationContext)
        ReportedContentStorage.provider = ReportedContentStorageAndroid(applicationContext)
        PrivacyPolicyManager.opener = PrivacyPolicyOpenerAndroid()
        ContentSharingManager.sharer = ContentSharerAndroid(applicationContext)
        ContentWipUseCase.storageProvider = ContentWipStorageAndroid(applicationContext)
        UrlOpenerHolder.urlOpener = UrlOpenerAndroid(applicationContext)

        // Initialize version checking with default URL, then update with config
        val defaultServerUrl = "https://tama.curzel.it"
        VersionUseCase.apiClient = ApiManager.getClient(defaultServerUrl)
        VersionUseCase.currentPlatform = Platform.ANDROID

        // Load config and update API client if needed
        lifecycleScope.launch {
            val config = ConfigStorage.loadConfig()
            config?.server_url?.let { serverUrl ->
                if (serverUrl != defaultServerUrl) {
                    VersionUseCase.apiClient = ApiManager.getClient(serverUrl)
                }
            }
        }

        deepLinkContentId = extractContentIdFromIntent(intent)

        setContent {
            App(deepLinkContentId = deepLinkContentId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkContentId = extractContentIdFromIntent(intent)
    }

    private fun extractContentIdFromIntent(intent: Intent?): Long? {
        return intent?.data?.let { uri ->
            // Parse URLs like: https://tama.curzel.it/view/content/123
            val pathSegments = uri.pathSegments
            if (pathSegments.size >= 3 &&
                pathSegments[0] == "view" &&
                pathSegments[1] == "content") {
                pathSegments[2].toLongOrNull()
            } else {
                null
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}