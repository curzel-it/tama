package it.curzel.tama.midi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MidiComposerViewModel {
    var composition by mutableStateOf("4c 4e 4g 2c5 4g 4e 2c")
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun updateComposition(newComposition: String) {
        composition = newComposition
        errorMessage = null
    }

    fun play() {
        if (isPlaying) return

        if (!MidiComposerUseCase.validateComposition(composition)) {
            errorMessage = "Invalid composition"
            return
        }

        isPlaying = true
        errorMessage = null

        CoroutineScope(Dispatchers.Default).launch {
            try {
                MidiComposerUseCase.playMidiComposition(composition, loop = true)
            } catch (e: Exception) {
                errorMessage = "Failed to play: ${e.message}"
                isPlaying = false
            }
        }
    }

    fun stop() {
        isPlaying = false
        MidiComposerUseCase.stopMidiPlayback()
    }
}
