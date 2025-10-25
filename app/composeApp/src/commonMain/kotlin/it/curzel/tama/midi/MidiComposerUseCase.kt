package it.curzel.tama.midi

import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object MidiComposerUseCase {
    suspend fun playMidiComposition(composition: String, loop: Boolean = true) {
        try {
            MidiComposer.play(composition, loop)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to play composition: ${e.message}")
        }
    }

    fun stopMidiPlayback() {
        MidiComposer.stop()
    }

    fun validateComposition(composition: String): Boolean {
        if (composition.isBlank()) return false

        return try {
            val notes = MidiParser.parseComposition(composition)
            notes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
