package it.curzel.tama.midi

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

class MidiComposerJvm : NativeMidiComposer {
    private var sourceDataLine: SourceDataLine? = null
    private var isCurrentlyPlaying = false
    private var playbackThread: Thread? = null

    override fun play(samples: FloatArray, sampleRate: Int, loop: Boolean) {
        stop()

        val audioFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate.toFloat(),
            16,
            1,
            2,
            sampleRate.toFloat(),
            false
        )

        val line = AudioSystem.getSourceDataLine(audioFormat)
        line.open(audioFormat)
        sourceDataLine = line
        isCurrentlyPlaying = true

        playbackThread = Thread {
            try {
                line.start()

                val byteBuffer = ByteArray(samples.size * 2)
                for (i in samples.indices) {
                    val sample = (samples[i] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    byteBuffer[i * 2] = (sample and 0xFF).toByte()
                    byteBuffer[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                }

                do {
                    if (!isCurrentlyPlaying) break
                    line.write(byteBuffer, 0, byteBuffer.size)
                } while (loop && isCurrentlyPlaying)

            } catch (e: Exception) {
                println("Audio playback error: ${e.message}")
            } finally {
                line.drain()
                line.stop()
                line.close()
                isCurrentlyPlaying = false
            }
        }
        playbackThread?.start()
    }

    override fun stop() {
        isCurrentlyPlaying = false
        playbackThread?.interrupt()
        playbackThread = null

        sourceDataLine?.let {
            try {
                it.stop()
                it.close()
            } catch (e: Exception) {
                println("Error stopping audio: ${e.message}")
            }
        }
        sourceDataLine = null
    }

    override fun isPlaying(): Boolean {
        return isCurrentlyPlaying
    }
}
