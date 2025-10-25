package it.curzel.tama.midi

import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object MidiComposerUseCase {
    private var playbackJob: Job? = null
    private var isPlaying = false

    /**
     * Play a MIDI composition with optional looping
     * Format: [duration][note][octave]
     * Example: "4c 4e 4g 2c5"
     */
    suspend fun playMidiComposition(composition: String, loop: Boolean = true) {
        stopMidiPlayback()

        val notes = parseComposition(composition)
        if (notes.isEmpty()) {
            throw IllegalArgumentException("No valid notes found in composition")
        }

        isPlaying = true

        coroutineScope {
            playbackJob = launch {
                do {
                    playNotesSequence(notes)
                } while (loop && isActive && isPlaying)
            }
        }
    }

    /**
     * Stop MIDI playback
     */
    fun stopMidiPlayback() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        stopAllNotes()
    }

    /**
     * Validate a MIDI composition string
     */
    fun validateComposition(composition: String): Boolean {
        if (composition.isBlank()) return false

        val tokens = composition.trim().split(Regex("\\s+"))
        return tokens.all { isValidNoteToken(it) }
    }

    /**
     * Parse composition string into list of MIDI notes
     * Format: [duration][note][octave]
     */
    private fun parseComposition(composition: String): List<MidiNote> {
        val tokens = composition.trim().split(Regex("\\s+"))
        return tokens.mapNotNull { parseNoteToken(it) }
    }

    /**
     * Parse a single note token (e.g., "4c", "8e5", "2g#4")
     */
    private fun parseNoteToken(token: String): MidiNote? {
        if (token.isBlank()) return null

        // Match pattern: [duration][note][sharp?][octave?]
        val pattern = Regex("""^(\d+)([a-gA-G])(#?)(\d?)$""")
        val match = pattern.find(token) ?: return null

        val (duration, note, sharp, octave) = match.destructured

        val durationValue = parseDuration(duration.toInt())
        val noteNumber = noteToMidiNumber(note, sharp, octave.toIntOrNull() ?: 4)

        return MidiNote(
            note = noteNumber,
            velocity = 100,
            duration = durationValue
        )
    }

    /**
     * Convert note duration notation to milliseconds
     * 1 = whole note, 2 = half note, 4 = quarter note, etc.
     */
    private fun parseDuration(duration: Int): Long {
        val wholeNoteMs = 2000L // Duration of a whole note in ms
        return wholeNoteMs / duration
    }

    /**
     * Convert note name to MIDI note number
     * Middle C (C4) = 60
     */
    private fun noteToMidiNumber(note: String, sharp: String, octave: Int): Int {
        val noteMap = mapOf(
            "c" to 0, "d" to 2, "e" to 4, "f" to 5,
            "g" to 7, "a" to 9, "b" to 11
        )

        val baseNote = noteMap[note.lowercase()] ?: 0
        val sharpOffset = if (sharp == "#") 1 else 0

        // MIDI note 60 is C4 (middle C)
        return 12 + (octave * 12) + baseNote + sharpOffset
    }

    /**
     * Check if a note token is valid
     */
    private fun isValidNoteToken(token: String): Boolean {
        val pattern = Regex("""^(\d+)([a-gA-G])(#?)(\d?)$""")
        return pattern.matches(token)
    }

    /**
     * Play a sequence of notes
     */
    private suspend fun playNotesSequence(notes: List<MidiNote>) {
        for (note in notes) {
            if (!isPlaying) break

            playNote(note)
            delay(note.duration)
        }
    }

    /**
     * Play a single MIDI note
     * TODO: Connect to MidiPlayer.provider when available
     */
    private fun playNote(note: MidiNote) {
        // TODO: Implement actual audio playback
        // This should call MidiPlayer.playNote(note) when the provider is set
        // For now, this is a stub
        println("Playing note: ${note.note} (velocity: ${note.velocity}, duration: ${note.duration}ms)")
    }

    /**
     * Stop all currently playing notes
     * TODO: Connect to MidiPlayer.stop() when available
     */
    private fun stopAllNotes() {
        // TODO: Implement actual audio stop
        // This should call MidiPlayer.stop() when the provider is set
        // For now, this is a stub
        println("Stopping all notes")
    }
}
