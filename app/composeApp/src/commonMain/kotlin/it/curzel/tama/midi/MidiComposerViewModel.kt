package it.curzel.tama.midi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.curzel.tama.content.ContentWipUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MidiComposerViewModel {
    var composition by mutableStateOf("4c 4e 4g 2c5 4g 4e 2c")
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            loadFromWip()
        }
    }

    suspend fun reload() {
        loadFromWip()
    }

    private suspend fun loadFromWip() {
        val loadedComposition = ContentWipUseCase.loadMidiComposition()
        println("[MIDI_COMPOSER] Loaded composition from WIP: '${loadedComposition}'")
        if (loadedComposition != null) {
            withContext(Dispatchers.Main) {
                composition = loadedComposition
                println("[MIDI_COMPOSER] Updated composition state on Main dispatcher")
            }
        }
    }

    private fun saveWip() {
        scope.launch {
            println("[MIDI_COMPOSER] Saving composition to WIP: '${composition}'")
            ContentWipUseCase.saveMidi(composition)
        }
    }

    fun updateComposition(newComposition: String) {
        println("[MIDI_COMPOSER] Composition updated: '${newComposition}'")
        composition = newComposition

        if (newComposition.isBlank()) {
            errorMessage = null
            return
        }

        if (MidiComposerUseCase.validateComposition(newComposition)) {
            errorMessage = null
            saveWip()
        } else {
            errorMessage = "Invalid MIDI composition - check note format"
        }
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
