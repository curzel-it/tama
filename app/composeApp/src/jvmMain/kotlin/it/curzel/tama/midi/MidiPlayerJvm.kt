package it.curzel.tama.midi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer
import javax.sound.midi.MidiChannel

class MidiPlayerJvm : NativeMidiPlayer {
    private var synthesizer: Synthesizer? = null
    private var channel: MidiChannel? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        try {
            synthesizer = MidiSystem.getSynthesizer()
            synthesizer?.open()
            channel = synthesizer?.channels?.get(0)
        } catch (e: Exception) {
            println("Failed to initialize MIDI synthesizer: ${e.message}")
        }
    }

    override fun playNote(note: Int, velocity: Int, duration: Long) {
        channel?.let { ch ->
            scope.launch {
                ch.noteOn(note, velocity)
                delay(duration)
                ch.noteOff(note)
            }
        }
    }

    override fun stop() {
        channel?.allNotesOff()
    }

    fun cleanup() {
        synthesizer?.close()
    }
}
