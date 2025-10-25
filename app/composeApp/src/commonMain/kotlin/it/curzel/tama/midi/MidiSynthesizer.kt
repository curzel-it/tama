package it.curzel.tama.midi

object MidiSynthesizer {
    const val SAMPLE_RATE = 48000

    fun generateAudioBuffer(composition: String): FloatArray {
        if (hasFlags(composition)) {
            return generateAudioBufferWithFlags(composition)
        }

        val notes = MidiParser.parseComposition(composition)

        if (notes.isEmpty()) {
            return FloatArray(0)
        }

        val allSamples = mutableListOf<Float>()

        for (note in notes) {
            val noteSamples = generateNoteSamples(note)
            allSamples.addAll(noteSamples.toList())
        }

        return allSamples.toFloatArray()
    }

    private fun hasFlags(composition: String): Boolean {
        return composition.contains("--channel") ||
                composition.contains("--bpm") ||
                composition.contains("--volume") ||
                composition.contains("--adsr") ||
                composition.contains("--vibrato")
    }

    private fun generateAudioBufferWithFlags(composition: String): FloatArray {
        val parsed = MidiFlags.parseChannels(composition)

        if (parsed.bpm != null) {
            MidiParser.bpm = parsed.bpm
        }

        if (parsed.channels.size == 1 && !composition.contains("--channel")) {
            val channel = parsed.channels[0]
            var notes = MidiParser.parseComposition(channel.composition)

            notes = notes.map { note ->
                note.copy(
                    volume = if (channel.volume != null && note.volume == 1.0f) channel.volume else note.volume,
                    adsr = channel.adsr,
                    vibrato = channel.vibrato
                )
            }

            val allSamples = mutableListOf<Float>()
            for (note in notes) {
                val noteSamples = generateNoteSamples(note)
                allSamples.addAll(noteSamples.toList())
            }
            return allSamples.toFloatArray()
        } else {
            val channelSamples = mutableListOf<FloatArray>()
            var maxLength = 0

            for (channel in parsed.channels) {
                var notes = MidiParser.parseComposition(channel.composition)

                notes = notes.map { note ->
                    note.copy(
                        volume = if (channel.volume != null && note.volume == 1.0f) channel.volume else note.volume,
                        adsr = channel.adsr,
                        vibrato = channel.vibrato
                    )
                }

                val channelBuffer = mutableListOf<Float>()
                for (note in notes) {
                    val noteSamples = generateNoteSamples(note)
                    channelBuffer.addAll(noteSamples.toList())
                }

                maxLength = maxOf(maxLength, channelBuffer.size)
                channelSamples.add(channelBuffer.toFloatArray())
            }

            val mixedSamples = FloatArray(maxLength) { 0f }

            for (channelBuffer in channelSamples) {
                for (i in channelBuffer.indices) {
                    mixedSamples[i] += channelBuffer[i]
                }
            }

            return mixedSamples
        }
    }

    private fun generateNoteSamples(note: ParsedNote): FloatArray {
        if (note.arpeggioNotes.isNotEmpty()) {
            val allSamples = mutableListOf<Float>()
            val noteDuration = note.duration / note.arpeggioNotes.size

            for (arpPitch in note.arpeggioNotes) {
                val frequency = MidiParser.midiToFrequency(arpPitch)
                val samples = MidiWaveforms.generateWaveformSamples(
                    frequency = frequency,
                    duration = noteDuration,
                    volume = note.volume,
                    waveform = note.waveform,
                    adsr = note.adsr,
                    vibrato = note.vibrato
                )
                allSamples.addAll(samples.toList())
            }

            return allSamples.toFloatArray()
        } else if (note.pitch != null) {
            val frequency = MidiParser.midiToFrequency(note.pitch)
            return MidiWaveforms.generateWaveformSamples(
                frequency = frequency,
                duration = note.duration,
                volume = note.volume,
                waveform = note.waveform,
                adsr = note.adsr,
                vibrato = note.vibrato
            )
        } else {
            val numSamples = (SAMPLE_RATE * note.duration).toInt()
            return FloatArray(numSamples) { 0f }
        }
    }
}
