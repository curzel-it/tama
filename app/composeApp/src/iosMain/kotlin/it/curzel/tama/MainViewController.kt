package it.curzel.tama

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import it.curzel.tama.api.ApiManager
import it.curzel.tama.content.ContentWipStorageIos
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerIos
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerIos
import it.curzel.tama.screens.UrlOpenerHolder
import it.curzel.tama.screens.UrlOpenerIos
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageIos
import it.curzel.tama.storage.ReportedContentStorage
import it.curzel.tama.storage.ReportedContentStorageIos
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerIos
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.sharing.ContentSharerIos
import it.curzel.tama.version.Platform
import it.curzel.tama.version.VersionUseCase

// Global state to hold deep link content ID for iOS
private var iosDeepLinkContentId by mutableStateOf<Long?>(null)

fun MainViewController() = ComposeUIViewController {
    // Initialize platform dependencies only once, not on every recomposition
    // This prevents creating multiple audio backend instances during screen rotation
    remember {
        MidiPlayer.provider = MidiPlayerIos()
        MidiComposer.backend = MidiComposerIos()
        ConfigStorage.provider = ConfigStorageIos()
        ReportedContentStorage.provider = ReportedContentStorageIos()
        PrivacyPolicyManager.opener = PrivacyPolicyOpenerIos()
        ContentSharingManager.sharer = ContentSharerIos()
        ContentWipUseCase.storageProvider = ContentWipStorageIos()
        UrlOpenerHolder.urlOpener = UrlOpenerIos()

        // Initialize version checking with default URL
        val defaultServerUrl = "https://tama.curzel.it"
        VersionUseCase.apiClient = ApiManager.getClient(defaultServerUrl)
        VersionUseCase.currentPlatform = Platform.IOS
        Unit // remember block must return a value
    }

    // Load config and update API client if needed
    LaunchedEffect(Unit) {
        val config = ConfigStorage.loadConfig()
        config?.server_url?.let { serverUrl ->
            if (serverUrl != "https://tama.curzel.it") {
                VersionUseCase.apiClient = ApiManager.getClient(serverUrl)
            }
        }
    }
    App(deepLinkContentId = iosDeepLinkContentId)
}

// Function to be called from Swift when a deep link is opened
fun handleDeepLink(url: String) {
    iosDeepLinkContentId = extractContentIdFromUrl(url)
}

private fun extractContentIdFromUrl(url: String): Long? {
    // Parse URLs like: https://tama.curzel.it/view/content/123
    return try {
        val pathPattern = Regex("/view/content/(\\d+)")
        pathPattern.find(url)?.groupValues?.get(1)?.toLongOrNull()
    } catch (e: Exception) {
        null
    }
}