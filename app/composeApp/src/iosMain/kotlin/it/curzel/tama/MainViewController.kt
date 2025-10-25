package it.curzel.tama

import androidx.compose.ui.window.ComposeUIViewController
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

fun MainViewController() = ComposeUIViewController {
    MidiPlayer.provider = MidiPlayerIos()
    MidiComposer.backend = MidiComposerIos()
    ConfigStorage.provider = ConfigStorageIos()
    ReportedContentStorage.provider = ReportedContentStorageIos()
    PrivacyPolicyManager.opener = PrivacyPolicyOpenerIos()
    App()
}