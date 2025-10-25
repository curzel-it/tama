package it.curzel.tama.midi

class MidiComposerIos : NativeMidiComposer {
    private var playing = false

    override fun play(samples: FloatArray, sampleRate: Int, loop: Boolean) {
        println("iOS MIDI Composer play stub: ${samples.size} samples at ${sampleRate}Hz, loop=$loop")
        playing = true
    }

    override fun stop() {
        println("iOS MIDI Composer stop stub")
        playing = false
    }

    override fun isPlaying(): Boolean {
        return playing
    }
}
