package it.curzel.tama.midi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

data class MidiNote(
    val note: Int,           // 0-127 (middle C = 60)
    val velocity: Int = 100, // 0-127 (volume)
    val duration: Long = 500 // milliseconds
)

interface NativeMidiPlayer {
    fun playNote(note: Int, velocity: Int, duration: Long)
    fun stop()
}

object MidiPlayer {
    lateinit var provider: NativeMidiPlayer

    private val scope = CoroutineScope(Dispatchers.Default)

    fun playNote(midiNote: MidiNote) {
        provider.playNote(midiNote.note, midiNote.velocity, midiNote.duration)
    }

    fun playNote(note: Int, velocity: Int = 100, duration: Long = 500) {
        provider.playNote(note, velocity, duration)
    }

    fun stop() {
        provider.stop()
    }

    fun midiNoteToFrequency(note: Int): Double {
        return 440.0 * 2.0.pow((note - 69) / 12.0)
    }
}
