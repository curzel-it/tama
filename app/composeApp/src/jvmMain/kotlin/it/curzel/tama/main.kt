package it.curzel.tama

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerJvm
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerJvm
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageJvm
import it.curzel.tama.storage.ReportedContentStorage
import it.curzel.tama.storage.ReportedContentStorageJvm
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerJvm
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.sharing.ContentSharerJvm

fun main() = application {
    MidiPlayer.provider = MidiPlayerJvm()
    MidiComposer.backend = MidiComposerJvm()
    ConfigStorage.provider = ConfigStorageJvm()
    ReportedContentStorage.provider = ReportedContentStorageJvm()
    PrivacyPolicyManager.opener = PrivacyPolicyOpenerJvm()
    ContentSharingManager.sharer = ContentSharerJvm()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Tama",
    ) {
        App()
    }
}