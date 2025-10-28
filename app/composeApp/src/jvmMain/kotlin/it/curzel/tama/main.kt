package it.curzel.tama

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import it.curzel.tama.api.ApiManager
import it.curzel.tama.content.ContentWipStorageJvm
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerJvm
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerJvm
import it.curzel.tama.screens.UrlOpenerHolder
import it.curzel.tama.screens.UrlOpenerJvm
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageJvm
import it.curzel.tama.storage.ReportedContentStorage
import it.curzel.tama.storage.ReportedContentStorageJvm
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerJvm
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.sharing.ContentSharerJvm
import it.curzel.tama.version.Platform
import it.curzel.tama.version.VersionUseCase
import kotlinx.coroutines.runBlocking

fun main() = application {
    MidiPlayer.provider = MidiPlayerJvm()
    MidiComposer.backend = MidiComposerJvm()
    ConfigStorage.provider = ConfigStorageJvm()
    ReportedContentStorage.provider = ReportedContentStorageJvm()
    PrivacyPolicyManager.opener = PrivacyPolicyOpenerJvm()
    ContentSharingManager.sharer = ContentSharerJvm()
    ContentWipUseCase.storageProvider = ContentWipStorageJvm()
    UrlOpenerHolder.urlOpener = UrlOpenerJvm()

    val config = runBlocking { ConfigStorage.loadConfig() }
    val serverUrl = config?.server_url ?: "https://tama.curzel.it"
    VersionUseCase.apiClient = ApiManager.getClient(serverUrl)
    VersionUseCase.currentPlatform = Platform.JVM

    Window(
        onCloseRequest = ::exitApplication,
        title = "Tama",
        state = WindowState(size = DpSize(400.dp, 600.dp))
    ) {
        App()
    }
}