package it.curzel.tama

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import it.curzel.tama.content.ContentWipStorageIos
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerIos
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerIos
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageIos
import it.curzel.tama.storage.ReportedContentStorage
import it.curzel.tama.storage.ReportedContentStorageIos
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerIos
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.sharing.ContentSharerIos

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
        Unit // remember block must return a value
    }
    App()
}