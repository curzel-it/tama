package it.curzel.tama.midi

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

enum class Waveform {
    SQUARE, TRIANGLE, SAWTOOTH, PULSE
}

data class ParsedNote(
    val pitch: Int?,
    val duration: Double,
    val waveform: Waveform,
    val volume: Float,
    val arpeggioNotes: List<Int>,
    val adsr: Boolean,
    val vibrato: Boolean
)

object MidiParser {
    var bpm: Int = 120

    fun parseNote(input: String): ParsedNote {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Empty note string")
        }

        var i = 0
        var durationStr = ""

        while (i < trimmed.length && trimmed[i].isDigit()) {
            durationStr += trimmed[i]
            i++
        }

        if (durationStr.isEmpty()) {
            throw IllegalArgumentException("Missing duration in note: '$input'")
        }

        val durationValue = durationStr.toInt()

        if (i < trimmed.length && trimmed[i] == '(') {
            return parseArpeggio(durationValue, trimmed.substring(i))
        }

        var isSharp = false
        if (i < trimmed.length && trimmed[i] == '#') {
            isSharp = true
            i++
        }

        if (i >= trimmed.length) {
            throw IllegalArgumentException("Missing note letter in: '$input'")
        }

        val noteChar = trimmed[i]

        if (noteChar == '-') {
            val duration = durationToSeconds(durationValue)
            return ParsedNote(
                pitch = null,
                duration = duration,
                waveform = Waveform.SQUARE,
                volume = 1.0f,
                arpeggioNotes = emptyList(),
                adsr = false,
                vibrato = false
            )
        }

        val noteLower = noteChar.lowercase()
        if (noteLower !in listOf("a", "b", "c", "d", "e", "f", "g")) {
            throw IllegalArgumentException("Invalid note letter: '$noteChar'")
        }

        i++

        var octave = 4
        var waveform = Waveform.SQUARE
        var volume = 1.0f

        var octaveStr = ""
        while (i < trimmed.length && trimmed[i].isDigit()) {
            octaveStr += trimmed[i]
            i++
        }
        if (octaveStr.isNotEmpty()) {
            octave = octaveStr.toInt()
        }

        if (i < trimmed.length && trimmed[i] in listOf('q', 't', 's', 'p')) {
            waveform = when (trimmed[i]) {
                'q' -> Waveform.SQUARE
                't' -> Waveform.TRIANGLE
                's' -> Waveform.SAWTOOTH
                'p' -> Waveform.PULSE
                else -> Waveform.SQUARE
            }
            i++
        }

        if (i < trimmed.length && trimmed[i] == '.') {
            i++
            if (i < trimmed.length && trimmed[i].isDigit()) {
                volume = trimmed[i].toString().toInt() / 10.0f
            }
        }

        val midiNote = noteToMidiNumber(noteLower, octave, isSharp)
        val duration = durationToSeconds(durationValue)

        return ParsedNote(
            pitch = midiNote,
            duration = duration,
            waveform = waveform,
            volume = volume,
            arpeggioNotes = emptyList(),
            adsr = false,
            vibrato = false
        )
    }

    private fun parseArpeggio(durationValue: Int, input: String): ParsedNote {
        var i = 1
        var content = ""
        var foundClose = false

        while (i < input.length) {
            if (input[i] == ')') {
                foundClose = true
                i++
                break
            }
            content += input[i]
            i++
        }

        if (!foundClose) {
            throw IllegalArgumentException("Missing closing parenthesis in arpeggio")
        }

        val noteStrings = content.trim().split(Regex("\\s+"))
        if (noteStrings.isEmpty()) {
            throw IllegalArgumentException("Empty arpeggio")
        }

        val arpeggioNotes = mutableListOf<Int>()
        for (noteStr in noteStrings) {
            var idx = 0
            var isSharp = false

            if (noteStr[idx] == '#') {
                isSharp = true
                idx++
            }

            val noteChar = noteStr[idx]
            val noteLower = noteChar.lowercase()

            if (noteLower !in listOf("a", "b", "c", "d", "e", "f", "g")) {
                throw IllegalArgumentException("Invalid arpeggio note: '$noteChar'")
            }

            idx++
            var octaveStr = ""
            while (idx < noteStr.length && noteStr[idx].isDigit()) {
                octaveStr += noteStr[idx]
                idx++
            }

            val octave = if (octaveStr.isNotEmpty()) octaveStr.toInt() else 4
            val midiNote = noteToMidiNumber(noteLower, octave, isSharp)
            arpeggioNotes.add(midiNote)
        }

        var waveform = Waveform.SQUARE
        var volume = 1.0f

        if (i < input.length && input[i] in listOf('q', 't', 's', 'p')) {
            waveform = when (input[i]) {
                'q' -> Waveform.SQUARE
                't' -> Waveform.TRIANGLE
                's' -> Waveform.SAWTOOTH
                'p' -> Waveform.PULSE
                else -> Waveform.SQUARE
            }
            i++
        }

        if (i < input.length && input[i] == '.') {
            i++
            if (i < input.length && input[i].isDigit()) {
                volume = input[i].toString().toInt() / 10.0f
            }
        }

        val duration = durationToSeconds(durationValue)

        return ParsedNote(
            pitch = null,
            duration = duration,
            waveform = waveform,
            volume = volume,
            arpeggioNotes = arpeggioNotes,
            adsr = false,
            vibrato = false
        )
    }

    private fun noteToMidiNumber(note: String, octave: Int, sharp: Boolean): Int {
        val baseNotes = mapOf(
            "c" to 0, "d" to 2, "e" to 4, "f" to 5,
            "g" to 7, "a" to 9, "b" to 11
        )
        val base = baseNotes[note] ?: 0
        val sharpOffset = if (sharp) 1 else 0
        val midiNote = 12 + (octave * 12) + base + sharpOffset
        return midiNote.coerceIn(0, 127)
    }

    private fun durationToSeconds(duration: Int): Double {
        val quarterNoteDuration = 60.0 / bpm
        return (4.0 / duration) * quarterNoteDuration
    }

    fun parseComposition(composition: String): List<ParsedNote> {
        val noteStrings = composition.trim().split(Regex("\\s+"))
        val notes = mutableListOf<ParsedNote>()

        for (noteStr in noteStrings) {
            try {
                val note = parseNote(noteStr)
                notes.add(note)
            } catch (e: Exception) {
                println("Failed to parse note '$noteStr': ${e.message}")
            }
        }

        return notes
    }

    fun midiToFrequency(midiNote: Int): Double {
        return 440.0 * 2.0.pow((midiNote - 69) / 12.0)
    }
}
