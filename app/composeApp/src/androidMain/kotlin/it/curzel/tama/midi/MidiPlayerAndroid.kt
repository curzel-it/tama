package it.curzel.tama.midi

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class MidiPlayerAndroid : NativeMidiPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentTrack: AudioTrack? = null

    override fun playNote(note: Int, velocity: Int, duration: Long) {
        scope.launch {
            try {
                stop()

                val frequency = midiNoteToFrequency(note)
                val sampleRate = 44100
                val numSamples = (duration * sampleRate / 1000).toInt()
                val samples = ShortArray(numSamples)

                val amplitude = (velocity / 127.0 * Short.MAX_VALUE * 0.5).toInt()

                for (i in samples.indices) {
                    val angle = 2.0 * PI * i / (sampleRate / frequency)
                    samples[i] = (amplitude * sin(angle)).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                currentTrack = audioTrack
                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()

                delay(duration)
                audioTrack.stop()
                audioTrack.release()
                currentTrack = null
            } catch (e: Exception) {
                println("Error playing note: ${e.message}")
            }
        }
    }

    override fun stop() {
        currentTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                println("Error stopping audio: ${e.message}")
            }
        }
        currentTrack = null
    }

    private fun midiNoteToFrequency(note: Int): Double {
        return 440.0 * 2.0.pow((note - 69) / 12.0)
    }
}
