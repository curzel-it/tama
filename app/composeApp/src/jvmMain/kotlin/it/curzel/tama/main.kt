package it.curzel.tama

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerJvm
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageJvm

fun main() = application {
    MidiPlayer.provider = MidiPlayerJvm()
    ConfigStorage.provider = ConfigStorageJvm()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Tama",
    ) {
        App()
    }
}