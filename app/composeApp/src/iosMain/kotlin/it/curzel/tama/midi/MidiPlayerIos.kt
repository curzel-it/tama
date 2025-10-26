package it.curzel.tama.midi

class MidiPlayerIos : NativeMidiPlayer {
    override fun playNote(note: Int, velocity: Int, duration: Long) {
        println("iOS MIDI playNote stub: note=$note, velocity=$velocity, duration=$duration")
    }

    override fun stop() {
        println("iOS MIDI stop stub")
    }
}
