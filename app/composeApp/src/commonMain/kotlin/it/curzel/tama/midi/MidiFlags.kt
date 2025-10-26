package it.curzel.tama.midi

data class MidiChannel(
    val composition: String,
    val volume: Float?,
    val adsr: Boolean,
    val vibrato: Boolean
)

data class ParsedComposition(
    val channels: List<MidiChannel>,
    val bpm: Int?
)

object MidiFlags {
    fun parseChannels(input: String): ParsedComposition {
        val tokens = input.trim().split(Regex("\\s+"))

        if (tokens.isEmpty()) {
            throw IllegalArgumentException("Empty input")
        }

        val globalFlags = GlobalFlags()
        var currentFlags = ChannelFlags()
        var bpm: Int? = null
        val channels = mutableListOf<MidiChannel>()
        var i = 0
        var foundChannel = false

        while (i < tokens.size) {
            val token = tokens[i]

            when {
                token == "--volume" -> {
                    if (i + 1 >= tokens.size) {
                        throw IllegalArgumentException("Missing value for --volume")
                    }
                    val vol = tokens[i + 1].toFloatOrNull()
                        ?: throw IllegalArgumentException("Invalid volume value: '${tokens[i + 1]}'")
                    if (vol < 0.0f || vol > 1.0f) {
                        throw IllegalArgumentException("Volume must be between 0.0 and 1.0, got $vol")
                    }

                    if (foundChannel) {
                        currentFlags.volume = vol
                    } else {
                        globalFlags.volume = vol
                    }
                    i += 2
                }

                token == "--bpm" -> {
                    if (i + 1 >= tokens.size) {
                        throw IllegalArgumentException("Missing value for --bpm")
                    }
                    val bpmVal = tokens[i + 1].toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid BPM value: '${tokens[i + 1]}'")
                    if (bpmVal < 1 || bpmVal > 300) {
                        throw IllegalArgumentException("BPM must be between 1 and 300, got $bpmVal")
                    }
                    bpm = bpmVal
                    i += 2
                }

                token == "--adsr" -> {
                    if (foundChannel) {
                        currentFlags.adsr = true
                    } else {
                        globalFlags.adsr = true
                    }
                    i += 1
                }

                token == "--vibrato" -> {
                    if (foundChannel) {
                        currentFlags.vibrato = true
                    } else {
                        globalFlags.vibrato = true
                    }
                    i += 1
                }

                token == "--channel" -> {
                    if (foundChannel && channels.isEmpty()) {
                        throw IllegalArgumentException("--channel found but no composition provided")
                    }
                    foundChannel = true
                    currentFlags = ChannelFlags()
                    i += 1
                }

                else -> {
                    if (foundChannel) {
                        var composition = ""
                        while (i < tokens.size && !tokens[i].startsWith("--")) {
                            if (composition.isNotEmpty()) {
                                composition += " "
                            }
                            composition += tokens[i]
                            i++
                        }

                        if (composition.isEmpty()) {
                            throw IllegalArgumentException("Empty composition for channel")
                        }

                        val volume = currentFlags.volume ?: globalFlags.volume
                        val adsr = currentFlags.adsr || globalFlags.adsr
                        val vibrato = currentFlags.vibrato || globalFlags.vibrato

                        channels.add(MidiChannel(composition, volume, adsr, vibrato))
                        foundChannel = false
                    } else {
                        var composition = ""
                        while (i < tokens.size && !tokens[i].startsWith("--")) {
                            if (composition.isNotEmpty()) {
                                composition += " "
                            }
                            composition += tokens[i]
                            i++
                        }

                        if (composition.isNotEmpty()) {
                            channels.add(
                                MidiChannel(
                                    composition,
                                    globalFlags.volume,
                                    globalFlags.adsr,
                                    globalFlags.vibrato
                                )
                            )
                        }
                    }
                }
            }
        }

        if (foundChannel) {
            throw IllegalArgumentException("--channel found but no composition provided")
        }

        if (channels.isEmpty()) {
            throw IllegalArgumentException("No channels or compositions found")
        }

        return ParsedComposition(channels, bpm)
    }

    private data class GlobalFlags(
        var volume: Float? = null,
        var adsr: Boolean = false,
        var vibrato: Boolean = false
    )

    private data class ChannelFlags(
        var volume: Float? = null,
        var adsr: Boolean = false,
        var vibrato: Boolean = false
    )
}
