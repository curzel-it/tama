package it.curzel.tama

import androidx.compose.ui.window.ComposeUIViewController
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerIos
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageIos

fun MainViewController() = ComposeUIViewController {
    MidiPlayer.provider = MidiPlayerIos()
    ConfigStorage.provider = ConfigStorageIos()
    App()
}