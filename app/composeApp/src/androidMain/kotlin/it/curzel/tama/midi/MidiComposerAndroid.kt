package it.curzel.tama.midi

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MidiComposerAndroid : NativeMidiComposer {
    private var audioTrack: AudioTrack? = null
    private var isCurrentlyPlaying = false
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun play(samples: FloatArray, sampleRate: Int, loop: Boolean) {
        stop()

        val shortSamples = ShortArray(samples.size) { i ->
            (samples[i] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, shortSamples.size * 2)

        val track = AudioTrack.Builder()
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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        isCurrentlyPlaying = true

        playbackJob = scope.launch {
            try {
                track.play()

                do {
                    if (!isCurrentlyPlaying || !isActive) break
                    track.write(shortSamples, 0, shortSamples.size)
                } while (loop && isCurrentlyPlaying && isActive)

                track.stop()
                isCurrentlyPlaying = false
            } catch (e: Exception) {
                println("Audio playback error: ${e.message}")
                isCurrentlyPlaying = false
            }
        }
    }

    override fun stop() {
        isCurrentlyPlaying = false
        playbackJob?.cancel()
        playbackJob = null

        audioTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                println("Error stopping audio: ${e.message}")
            }
        }
        audioTrack = null
    }

    override fun isPlaying(): Boolean {
        return isCurrentlyPlaying
    }
}
